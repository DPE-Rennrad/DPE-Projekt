package edu.thi.demo.camunda;

import edu.thi.demo.model.ExternalProvider;
import edu.thi.demo.model.Kunde;
import edu.thi.demo.service.ExternalProviderService;
import edu.thi.demo.service.KundeService;
import edu.thi.demo.service.StoredEventService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.variable.Variables;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;



@Startup
@ApplicationScoped
public class EventermittlungExternalTaskWorker {

    private static final Logger LOG = Logger.getLogger(EventermittlungExternalTaskWorker.class);

    static final String TOPIC_PROVIDER_LADEN = "external-provider-laden";
    static final String TOPIC_PROVIDER_ABFRAGEN = "external-provider-abfragen";
    static final String TOPIC_ANTWORTEN_EMPFANGEN = "antworten-empfangen";

    static final String TOPIC_EVENTS_SPEICHERN = "events-speichern";
    static final String TOPIC_EVENTS_LADEN = "events-laden";
    static final String TOPIC_EVENTS_ABGLEICHEN_KI = "events-abgleichen-ki";
    static final String TOPIC_BELIEBTESTE_EVENTS = "beliebteste-events-ermitteln";
    static final String TOPIC_AFFILIATE_LINKS_GENERIEREN = "affiliate-links-generieren";
    static final String TOPIC_NEWSLETTER_ERSTELLEN = "newsletter-erstellen";
    static final String TOPIC_NEWSLETTER_SENDEN = "newsletter-senden";

    static final String WORKER_ID = "quarkus-eventermittlung-worker";

    private final String engineRestUrl;
    private final String newsletterSendWebhookUrl;
    private final ExternalProviderService externalProviderService;
    private final StoredEventService storedEventService;
    private final KundeService kundeService;
    private final ObjectMapper objectMapper;

    private ExternalTaskClient client;

    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    private static final List<String> MUST_HAVE_NAMES = List.of("Hans", "Peter", "Kermit");
    private static final AtomicInteger NAME_ROTATOR = new AtomicInteger(0);

    public EventermittlungExternalTaskWorker(
        @ConfigProperty(name = "camunda.engine-rest.url", defaultValue = "http://localhost:8080/engine-rest") String engineRestUrl,
        @ConfigProperty(name = "newsletter.send.webhook.url", defaultValue = "http://localhost:8088/mock-inbox/newsletter") String newsletterSendWebhookUrl,
        ExternalProviderService externalProviderService,
        StoredEventService storedEventService,
        KundeService kundeService,
        ObjectMapper objectMapper
    ) {
        this.engineRestUrl = engineRestUrl;
        this.newsletterSendWebhookUrl = newsletterSendWebhookUrl;
        this.externalProviderService = externalProviderService;
        this.storedEventService = storedEventService;
        this.kundeService = kundeService;
        this.objectMapper = objectMapper;
    }

    private static String firstNameFromFullName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] parts = trimmed.split("\\s+");
        return parts.length > 0 ? parts[0] : trimmed;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String buildNewsletterDeliveryJson(String processInstanceId, String taskId, String status, String newsletterText) {
        // keep it minimal and JSON-safe
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("processInstanceId", processInstanceId);
            payload.put("taskId", taskId);
            payload.put("status", status);
            payload.put("newsletterText", newsletterText);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            // fallback: still send something
            String safe = newsletterText == null ? "" : newsletterText.replace("\\\"", "\\\\\"");
            return "{\"processInstanceId\":\"" + (processInstanceId == null ? "" : processInstanceId)
                + "\",\"taskId\":\"" + (taskId == null ? "" : taskId)
                + "\",\"status\":\"" + (status == null ? "" : status)
                + "\",\"newsletterText\":\"" + safe + "\"}";
        }
    }

    private int postNewsletterDelivery(String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(newsletterSendWebhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode();
    }

    private String resolveCustomerFirstName(ExternalTask externalTask) {
        // Try a few likely variable names first
        Object n = externalTask.getVariable("kundeVorname");
        if (n == null) n = externalTask.getVariable("vorname");
        if (n == null) n = externalTask.getVariable("firstName");
        // Also try full name "kundeName" and extract first name
        if (n == null) {
            Object fullName = externalTask.getVariable("kundeName");
            if (fullName instanceof String s && !s.isBlank()) {
                return firstNameFromFullName(s);
            }
        }
        if (n instanceof String s && !s.isBlank()) {
            return s.trim();
        }

        // Fallback: kundeId -> load Kunde from DB -> take first token of name
        Object kundeIdObj = externalTask.getVariable("kundeId");
        if (kundeIdObj == null) {
            return "";
        }
        try {
            Long kundeId = null;
            if (kundeIdObj instanceof Number num) {
                kundeId = num.longValue();
            } else {
                kundeId = Long.parseLong(String.valueOf(kundeIdObj));
            }
            Kunde k = kundeService.getKundeById(kundeId);
            return k == null ? "" : firstNameFromFullName(k.name);
        } catch (Exception ignored) {
            return "";
        }
    }

    private List<String> buildPassendeLeuteForMockEvent(String providerKey, int indexWithinProvider) {
        // Guarantee each of Hans/Peter/Kermit occurs at least once across the run.
        // With single provider mode, ensure Peter is always included for demo
        String mustHave;
        if ("1".equals(providerKey) && indexWithinProvider == 1) {
            mustHave = "Peter";  // Changed from Hans to Peter for single-provider demo
        } else if ("2".equals(providerKey) && indexWithinProvider == 1) {
            mustHave = "Peter";
        } else if ("3".equals(providerKey) && indexWithinProvider == 1) {
            mustHave = "Kermit";
        } else {
            mustHave = MUST_HAVE_NAMES.get(Math.floorMod(NAME_ROTATOR.getAndIncrement(), MUST_HAVE_NAMES.size()));
        }

        // Add one extra random-ish name so the array looks realistic
        List<String> extras = List.of("Anna", "Maria", "Lukas");
        String extra = extras.get(ThreadLocalRandom.current().nextInt(extras.size()));
        if (extra.equalsIgnoreCase(mustHave)) {
            extra = "Max";
        }
        return List.of(mustHave, extra);
    }

    @PostConstruct
    void start() {
        LOG.infof("Starting External Task workerId=%s baseUrl=%s", WORKER_ID, engineRestUrl);
        client = ExternalTaskClient.create()
            .baseUrl(engineRestUrl)
            .workerId(WORKER_ID)
            .asyncResponseTimeout(10_000)
            .build();

        client.subscribe(TOPIC_PROVIDER_LADEN)
            .lockDuration(30_000)
            .handler(new ProviderLadenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_PROVIDER_LADEN);

        client.subscribe(TOPIC_PROVIDER_ABFRAGEN)
            .lockDuration(30_000)
            .handler(new ProviderAbfragenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_PROVIDER_ABFRAGEN);

        client.subscribe(TOPIC_ANTWORTEN_EMPFANGEN)
            .lockDuration(30_000)
            .handler(new AntwortenEmpfangenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_ANTWORTEN_EMPFANGEN);

        client.subscribe(TOPIC_EVENTS_SPEICHERN)
            .lockDuration(30_000)
            .handler(new EventsSpeichernHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_EVENTS_SPEICHERN);

        client.subscribe(TOPIC_EVENTS_LADEN)
            .lockDuration(30_000)
            .handler(new EventsLadenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_EVENTS_LADEN);

        client.subscribe(TOPIC_EVENTS_ABGLEICHEN_KI)
            .lockDuration(30_000)
            .handler(new EventsAbgleichenKiHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_EVENTS_ABGLEICHEN_KI);

        client.subscribe(TOPIC_BELIEBTESTE_EVENTS)
            .lockDuration(30_000)
            .handler(new BeliebtesteEventsErmittelnHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_BELIEBTESTE_EVENTS); 

        client.subscribe(TOPIC_AFFILIATE_LINKS_GENERIEREN)
            .lockDuration(30_000)
            .handler(new AffiliateLinksGenerierenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_AFFILIATE_LINKS_GENERIEREN);

        client.subscribe(TOPIC_NEWSLETTER_ERSTELLEN)
            .lockDuration(30_000)
            .handler(new NewsletterErstellenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_NEWSLETTER_ERSTELLEN);

        client.subscribe(TOPIC_NEWSLETTER_SENDEN)
            .lockDuration(30_000)
            .handler(new NewsletterSendenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_NEWSLETTER_SENDEN);
    }

    @PreDestroy
    void stop() {
        if (client != null) {
            client.stop();
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out;
        }
        if (value instanceof String s) {
            try {
                return objectMapper.readValue(s, new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
                // fall through
            }
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(String.valueOf(value));
        }
    }

    private List<Map<String, Object>> toListOfMaps(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof String s) {
            try {
                return objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception ignored) {
                // fall through
            }
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String asLowerString(Object v) {
        return v == null ? "" : String.valueOf(v).toLowerCase();
    }

    private static Map<String, Object> normalizeEvent(Map<String, Object> e) {
        Map<String, Object> out = new HashMap<>();
        // normalize some common keys used in the worker
        out.put("title", e.getOrDefault("title", e.getOrDefault("name", "")));
        out.put("location", e.getOrDefault("location", ""));
        out.put("provider", e.getOrDefault("provider", e.getOrDefault("providerName", "")));
        out.put("date", e.getOrDefault("date", e.getOrDefault("eventDate", "")));
        return out;
    }

    public class ProviderLadenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_PROVIDER_LADEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                externalProviderService.initializeMockProviders();
                List<ExternalProvider> providers = externalProviderService.getActiveProviders();

                // Nur den ersten Provider laden und ID als String setzen
                if (!providers.isEmpty()) {
                    ExternalProvider singleProvider = providers.get(0);
                    externalTaskService.complete(
                        externalTask,
                        Map.of(
                            "eventanbieterEintrag",
                            Variables.stringValue(String.valueOf(singleProvider.id)),
                            "providerCount",
                            Variables.integerValue(1)
                        )
                    );
                    LOG.infof("Completed topic=%s taskId=%s provider=%s", TOPIC_PROVIDER_LADEN, externalTask.getId(), singleProvider.name);
                } else {
                    externalTaskService.handleFailure(
                        externalTask,
                        "Keine Provider verfügbar",
                        "Keine aktiven Provider gefunden",
                        0,
                        0L
                    );
                }
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_PROVIDER_LADEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Provider laden fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class ProviderAbfragenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_PROVIDER_ABFRAGEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String providerIdStr = (String) externalTask.getVariable("eventanbieterEintrag");
                if (providerIdStr == null || providerIdStr.isBlank()) {
                    externalTaskService.handleFailure(
                        externalTask,
                        "Missing provider variable",
                        "Expected variable 'eventanbieterEintrag'",
                        0,
                        0L
                    );
                    return;
                }

                Long providerId;
                try {
                    providerId = Long.parseLong(providerIdStr);
                } catch (NumberFormatException e) {
                    externalTaskService.handleFailure(
                        externalTask,
                        "Invalid provider ID",
                        "Provider ID is not a valid number: " + providerIdStr,
                        0,
                        0L
                    );
                    return;
                }

                ExternalProvider provider = externalProviderService.getProviderById(providerId);
                if (provider == null) {
                    externalTaskService.handleFailure(
                        externalTask,
                        "Provider not found",
                        "Provider with id " + providerId + " not found",
                        0,
                        0L
                    );
                    return;
                }

                String providerKey = provider.id != null ? String.valueOf(provider.id) : provider.name;

                // Mock: 2-5 events
                int count = ThreadLocalRandom.current().nextInt(2, 6);
                List<Map<String, Object>> events = new ArrayList<>(count);
                for (int i = 1; i <= count; i++) {
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("provider", providerKey);
                    ev.put("title", "Event " + i + " von Anbieter " + provider.name);
                    ev.put("location", "Mock-City");
                    ev.put("date", Instant.now().plusSeconds(86400L * i).toString());
                    ev.put("passendeLeute", buildPassendeLeuteForMockEvent(providerKey, i));
                    events.add(ev);
                }

                // Persist the mock events to MySQL
                storedEventService.persistMockEvents(providerKey, events);

                // Complete the external task FIRST without additional variables
                externalTaskService.complete(externalTask);
                LOG.infof("Completed topic=%s taskId=%s providerKey=%s events=%d", TOPIC_PROVIDER_ABFRAGEN, externalTask.getId(), providerKey, events.size());

                // Send message AFTER completing the task, with a small delay to ensure Receive Task is reached
                String processInstanceId = externalTask.getProcessInstanceId();
                CompletableFuture.runAsync(() -> {
                    try {
                        // Wait 500ms to ensure the process has reached the Receive Task
                        Thread.sleep(500);
                        
                        Map<String, Object> messagePayload = new HashMap<>();
                        messagePayload.put("messageName", "event");
                        messagePayload.put("processInstanceId", processInstanceId);
                        
                        Map<String, Object> processVariables = new HashMap<>();
                        // Serialize the events list to JSON string for proper REST API format
                        String eventsJson = objectMapper.writeValueAsString(events);
                        processVariables.put("receivedEvents", Map.of("value", eventsJson, "type", "Object", "valueInfo", Map.of("objectTypeName", "java.util.List", "serializationDataFormat", "application/json")));
                        processVariables.put("enoughAnswers", Map.of("value", true, "type", "Boolean"));
                        messagePayload.put("processVariables", processVariables);

                        String payloadJson = objectMapper.writeValueAsString(messagePayload);
                        LOG.infof("Sending message correlation: %s", payloadJson);
                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(engineRestUrl + "/message"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                            .build();
                        
                        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            LOG.infof("Message 'event' successfully correlated via REST API. Status: %d", response.statusCode());
                        } else {
                            LOG.warnf("Message correlation failed. Status: %d, Response: %s", response.statusCode(), response.body());
                        }
                    } catch (Exception ex) {
                        LOG.errorf(ex, "Failed to send message via REST API (async)");
                    }
                });
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_PROVIDER_ABFRAGEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Provider abfragen fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class AntwortenEmpfangenHandler implements ExternalTaskHandler {
        @Override
        @SuppressWarnings("unchecked")
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_ANTWORTEN_EMPFANGEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                List<String> providers = toStringList(externalTask.getVariable("eventanbieter"));

                int expected = providers.size();
                int responses = 0;
                List<Map<String, Object>> allEvents = new ArrayList<>();

                Map<String, Object> vars = externalTask.getAllVariables();
                for (String p : providers) {
                    Object evObj = vars.get("events_" + p);
                    List<Map<String, Object>> providerEvents = toListOfMaps(evObj);
                    if (!providerEvents.isEmpty()) {
                        responses++;
                        allEvents.addAll(providerEvents);
                    }
                }

                double responseRate = expected == 0 ? 0.0 : (double) responses / (double) expected;
                boolean enoughAnswers = responseRate >= 0.2;

                Map<String, Object> outVars = new HashMap<>();
                outVars.put(
                    "allReceivedEvents",
                    Variables.objectValue(allEvents)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                outVars.put("totalResponses", responses);
                outVars.put("responseRate", responseRate);
                outVars.put("enoughAnswers", enoughAnswers);

                externalTaskService.complete(externalTask, outVars);
                LOG.infof(
                    "Completed topic=%s taskId=%s expected=%d responses=%d totalEvents=%d enoughAnswers=%s",
                    TOPIC_ANTWORTEN_EMPFANGEN,
                    externalTask.getId(),
                    expected,
                    responses,
                    allEvents.size(),
                    String.valueOf(enoughAnswers)
                );
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_ANTWORTEN_EMPFANGEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Antworten empfangen fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class EventsSpeichernHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_EVENTS_SPEICHERN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                // Source is produced by AntwortenEmpfangen
                List<Map<String, Object>> received = toListOfMaps(externalTask.getVariable("allReceivedEvents"));
                List<Map<String, Object>> normalized = new ArrayList<>(received.size());
                for (Map<String, Object> e : received) {
                    if (e != null) {
                        normalized.add(normalizeEvent(e));
                    }
                }

                Map<String, Object> out = new HashMap<>();
                // Keep a canonical variable name used by later steps
                out.put(
                    "events",
                    Variables.objectValue(normalized)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                // Also keep a pseudo "DB" variable inside the process instance
                out.put(
                    "eventsDb",
                    Variables.objectValue(normalized)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                // Ensure this flag exists before any gateway expressions might read it
                // Use plain Boolean for maximum REST compatibility
                out.put("passendesEventGefunden", false);
                out.put("manyRetriesEventsSpeichern", false);

                LOG.infof(
                    "Completing topic=%s taskId=%s vars=%s passendesEventGefunden=%s",
                    TOPIC_EVENTS_SPEICHERN,
                    externalTask.getId(),
                    out.keySet(),
                    String.valueOf(out.get("passendesEventGefunden"))
                );
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s savedEvents=%d", TOPIC_EVENTS_SPEICHERN, externalTask.getId(), normalized.size());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_EVENTS_SPEICHERN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Events speichern fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class EventsLadenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_EVENTS_LADEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                // Load from the real DB first
                List<Map<String, Object>> events = storedEventService.listAllAsMaps();
                if (events.isEmpty()) {
                    // Fallback: if DB empty, reuse received events if any
                    events = toListOfMaps(externalTask.getVariable("allReceivedEvents"));
                }

                Map<String, Object> out = new HashMap<>();
                out.put(
                    "events",
                    Variables.objectValue(events)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                // Defensive: some process variants may use this flag after loading
                out.put("passendesEventGefunden", false);
                out.put("manyRetriesEventsLaden", false);
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s loadedEvents=%d", TOPIC_EVENTS_LADEN, externalTask.getId(), events.size());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_EVENTS_LADEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Events laden fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class EventsAbgleichenKiHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_EVENTS_ABGLEICHEN_KI, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                List<Map<String, Object>> events = toListOfMaps(externalTask.getVariable("events"));
                if (events.isEmpty()) {
                    events = toListOfMaps(externalTask.getVariable("receivedEvents"));
                }
                if (events.isEmpty()) {
                    events = toListOfMaps(externalTask.getVariable("allReceivedEvents"));
                }

                String customerFirstName = resolveCustomerFirstName(externalTask).trim();
                String customerFirstNameLower = customerFirstName.toLowerCase();

                // Optional preference variables (if present)
                String preferredLocation = String.valueOf(externalTask.getVariable("preferredLocation") == null ? "" : externalTask.getVariable("preferredLocation"));
                String preferredKeyword = String.valueOf(externalTask.getVariable("preferredKeyword") == null ? "" : externalTask.getVariable("preferredKeyword"));

                LOG.infof("KI-Abgleich: customer='%s' events=%d preferredLocation='%s' preferredKeyword='%s'", 
                    customerFirstName, events.size(), preferredLocation, preferredKeyword);

                String loc = preferredLocation.trim().toLowerCase();
                String kw = preferredKeyword.trim().toLowerCase();

                List<Map<String, Object>> matched = new ArrayList<>();
                for (Map<String, Object> e : events) {
                    if (e == null) {
                        continue;
                    }
                    Map<String, Object> ne = normalizeEvent(e);

                    // Merge passendeLeute into normalized map if present
                    Object ppl = e.getOrDefault("passendeLeute", e.getOrDefault("matchingPeople", List.of()));
                    List<String> passendeLeute = toStringList(ppl);
                    ne.put("passendeLeute", passendeLeute);

                    LOG.infof("Event: title='%s' passendeLeute=%s location='%s'", 
                        ne.get("title"), passendeLeute, ne.get("location"));

                    boolean ok = true;

                    // Primary demo rule: if we know the customer's first name, match against passendeLeute
                    if (!customerFirstNameLower.isEmpty()) {
                        boolean nameMatch = passendeLeute.stream().anyMatch(n -> n != null && n.trim().equalsIgnoreCase(customerFirstNameLower));
                        LOG.infof("  Checking customer '%s' in passendeLeute: %s", customerFirstNameLower, nameMatch);
                        ok = ok && nameMatch;
                    }

                    if (!loc.isEmpty()) {
                        ok = ok && asLowerString(ne.get("location")).contains(loc);
                    }
                    if (!kw.isEmpty()) {
                        ok = ok && asLowerString(ne.get("title")).contains(kw);
                    }
                    if (ok) {
                        matched.add(ne);
                    }
                }

                Map<String, Object> out = new HashMap<>();
                out.put(
                    "events",
                    Variables.objectValue(matched)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                // Use plain Boolean for maximum REST compatibility
                out.put("passendesEventGefunden", !matched.isEmpty());
                out.put("manyRetriesEventsAbgleichen", false);

                LOG.infof(
                    "Completing topic=%s taskId=%s vars=%s passendesEventGefunden=%s matched=%d",
                    TOPIC_EVENTS_ABGLEICHEN_KI,
                    externalTask.getId(),
                    out.keySet(),
                    String.valueOf(out.get("passendesEventGefunden")),
                    matched.size()
                );
                externalTaskService.complete(externalTask, out);
                LOG.infof(
                    "Completed topic=%s taskId=%s matched=%d passendesEventGefunden=%s",
                    TOPIC_EVENTS_ABGLEICHEN_KI,
                    externalTask.getId(),
                    matched.size(),
                    String.valueOf(out.get("passendesEventGefunden"))
                );
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_EVENTS_ABGLEICHEN_KI, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Events abgleichen (KI) fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class BeliebtesteEventsErmittelnHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_BELIEBTESTE_EVENTS, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                List<Map<String, Object>> events = toListOfMaps(externalTask.getVariable("events"));
                if (events.isEmpty()) {
                    events = toListOfMaps(externalTask.getVariable("allReceivedEvents"));
                }

                // Simple ranking: count by title, keep top 3
                Map<String, Integer> counts = new HashMap<>();
                Map<String, Map<String, Object>> firstSeen = new HashMap<>();
                for (Map<String, Object> e : events) {
                    if (e == null) {
                        continue;
                    }
                    Map<String, Object> ne = normalizeEvent(e);
                    String title = String.valueOf(ne.getOrDefault("title", ""));
                    counts.put(title, counts.getOrDefault(title, 0) + 1);
                    firstSeen.putIfAbsent(title, ne);
                }

                List<Map<String, Object>> top = counts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .map(e -> firstSeen.getOrDefault(e.getKey(), Map.of("title", e.getKey())))
                    .toList();

                Map<String, Object> out = new HashMap<>();
                out.put(
                    "topEvents",
                    Variables.objectValue(top)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s top=%d", TOPIC_BELIEBTESTE_EVENTS, externalTask.getId(), top.size());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_BELIEBTESTE_EVENTS, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Beliebteste Events ermitteln fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class AffiliateLinksGenerierenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_AFFILIATE_LINKS_GENERIEREN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                List<Map<String, Object>> events = toListOfMaps(externalTask.getVariable("events"));
                if (events.isEmpty()) {
                    events = toListOfMaps(externalTask.getVariable("allReceivedEvents"));
                }

                List<String> links = new ArrayList<>();
                int i = 0;
                for (Map<String, Object> e : events) {
                    if (e == null) {
                        continue;
                    }
                    Map<String, Object> ne = normalizeEvent(e);
                    String eventId = String.valueOf(ne.getOrDefault("id", "event-" + (++i)));
                    String title = String.valueOf(ne.getOrDefault("title", ""));
                    String link = "https://example.test/affiliate?eventId=" + urlEncode(eventId) + "&title=" + urlEncode(title);
                    links.add(link);
                }

                Map<String, Object> out = new HashMap<>();
                out.put(
                    "affiliatelinkListe",
                    Variables.objectValue(links)
                        .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                        .create()
                );
                out.put("manyRetriesAffiliateGenerieren", false);
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s links=%d", TOPIC_AFFILIATE_LINKS_GENERIEREN, externalTask.getId(), links.size());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_AFFILIATE_LINKS_GENERIEREN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Affiliate-Links generieren fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class NewsletterErstellenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_NEWSLETTER_ERSTELLEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                List<String> links = toStringList(externalTask.getVariable("affiliatelinkListe"));
                StringBuilder sb = new StringBuilder();
                sb.append("Hallo!\n\nHier sind deine Event-Empfehlungen:\n");
                for (String l : links) {
                    if (l != null && !l.isBlank()) {
                        sb.append("- ").append(l).append("\n");
                    }
                }
                if (links.isEmpty()) {
                    sb.append("(Keine Links vorhanden)\n");
                }
                sb.append("\nViele Grüße\nDein DPE Demo-System\n");

                Map<String, Object> out = new HashMap<>();
                out.put("newsletterText", sb.toString());
                out.put("manyRetriesNewsletterErstellen", false);
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_NEWSLETTER_ERSTELLEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_NEWSLETTER_ERSTELLEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Newsletter erstellen fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    public class NewsletterSendenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_NEWSLETTER_SENDEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String text = String.valueOf(externalTask.getVariable("newsletterText") == null ? "" : externalTask.getVariable("newsletterText"));
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                LOG.infof("Mock send newsletter preview=%s", preview.replace("\n", " "));

                String status = "SENT";
                Integer webhookStatus = null;
                String webhookUrl = newsletterSendWebhookUrl;
                if (webhookUrl != null && !webhookUrl.isBlank()) {
                    try {
                        String payloadJson = buildNewsletterDeliveryJson(
                            externalTask.getProcessInstanceId(),
                            externalTask.getId(),
                            status,
                            text
                        );
                        webhookStatus = postNewsletterDelivery(payloadJson);
                        if (webhookStatus < 200 || webhookStatus >= 300) {
                            status = "SENT_WEBHOOK_FAILED";
                        } else {
                            status = "SENT_WEBHOOK";
                        }
                        LOG.infof("Newsletter webhook delivered url=%s status=%d", webhookUrl, webhookStatus);
                    } catch (Exception e) {
                        status = "SENT_WEBHOOK_FAILED";
                        LOG.warnf(e, "Newsletter webhook delivery failed url=%s", webhookUrl);
                    }
                }

                Map<String, Object> out = new HashMap<>();
                out.put("newsletterSendStatus", status);
                out.put("newsletterSendWebhookUrl", webhookUrl);
                if (webhookStatus != null) {
                    out.put("newsletterSendWebhookStatus", webhookStatus);
                }
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_NEWSLETTER_SENDEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_NEWSLETTER_SENDEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Newsletter senden fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }
}