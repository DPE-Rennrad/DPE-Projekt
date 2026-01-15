package edu.thi.demo.camunda;

import edu.thi.demo.model.Kunde;
import edu.thi.demo.model.RouteRequest;
import edu.thi.demo.model.RouteResponse;
import edu.thi.demo.service.KundeService;
import edu.thi.demo.service.RouteGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Startup
@ApplicationScoped
public class RouteExternalTaskWorker {

    private static final Logger LOG = Logger.getLogger(RouteExternalTaskWorker.class);

    static final String TOPIC_ROUTE_GENERIEREN = "route-generieren";
    static final String TOPIC_ROUTE_EMAIL_ERSTELLEN = "route-email-erstellen";
    static final String TOPIC_ROUTE_EMAIL_SENDEN = "route-email-senden";
    static final String TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN = "bewertungserinnerung-erstellen";
    static final String TOPIC_BEWERTUNGSERINNERUNG_SENDEN = "bewertungserinnerung-senden";

    @ConfigProperty(name = "camunda.engine-rest.url", defaultValue = "http://localhost:8080/engine-rest")
    String engineRestUrl;

    @ConfigProperty(name = "route.email.webhook.url", defaultValue = "http://localhost:8088/mock-inbox/route")
    String routeEmailWebhookUrl;

    @ConfigProperty(name = "bewertungsbogen.webhook.url", defaultValue = "http://localhost:8088/mock-inbox/bewertungsbogen")
    String bewertungsbogenWebhookUrl;

    static final String WORKER_ID = "quarkus-route-worker";

    private final KundeService kundeService;
    private final RouteGenerationService routeGenerationService;
    private final ObjectMapper objectMapper;

    private ExternalTaskClient client;

    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    public RouteExternalTaskWorker(
        KundeService kundeService,
        RouteGenerationService routeGenerationService,
        ObjectMapper objectMapper
    ) {
        this.kundeService = kundeService;
        this.routeGenerationService = routeGenerationService;
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

    private String buildEmailDeliveryJson(String processInstanceId, String taskId, String status, String emailType, String emailText) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("processInstanceId", processInstanceId);
            payload.put("taskId", taskId);
            payload.put("status", status);
            payload.put("emailType", emailType);
            payload.put("emailText", emailText);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            String safe = emailText == null ? "" : emailText.replace("\\\"", "\\\\\"");
            return "{\"processInstanceId\":\"" + (processInstanceId == null ? "" : processInstanceId)
                + "\",\"taskId\":\"" + (taskId == null ? "" : taskId)
                + "\",\"status\":\"" + (status == null ? "" : status)
                + "\",\"emailType\":\"" + (emailType == null ? "" : emailType)
                + "\",\"emailText\":\"" + safe + "\"}";
        }
    }

    private int postEmailDelivery(String webhookUrl, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode();
    }

    private String resolveCustomerFirstName(ExternalTask externalTask) {
        Object n = externalTask.getVariable("kundeVorname");
        if (n == null) n = externalTask.getVariable("vorname");
        if (n == null) n = externalTask.getVariable("firstName");
        if (n instanceof String s && !s.isBlank()) {
            return s.trim();
        }

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

    @PostConstruct
    void start() {
        LOG.infof("Starting External Task workerId=%s baseUrl=%s", WORKER_ID, engineRestUrl);
        client = ExternalTaskClient.create()
            .baseUrl(engineRestUrl)
            .workerId(WORKER_ID)
            .asyncResponseTimeout(10_000)
            .build();

        client.subscribe(TOPIC_ROUTE_GENERIEREN)
            .lockDuration(30_000)
            .handler(new RouteGenerierenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_ROUTE_GENERIEREN);

        client.subscribe(TOPIC_ROUTE_EMAIL_ERSTELLEN)
            .lockDuration(30_000)
            .handler(new RouteEmailErstellenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_ROUTE_EMAIL_ERSTELLEN);

        client.subscribe(TOPIC_ROUTE_EMAIL_SENDEN)
            .lockDuration(30_000)
            .handler(new RouteEmailSendenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_ROUTE_EMAIL_SENDEN);

        client.subscribe(TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN)
            .lockDuration(30_000)
            .handler(new BewertungserinnerungErstellenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN);

        client.subscribe(TOPIC_BEWERTUNGSERINNERUNG_SENDEN)
            .lockDuration(30_000)
            .handler(new BewertungserinnerungSendenHandler())
            .open();
        LOG.infof("Subscribed topic=%s", TOPIC_BEWERTUNGSERINNERUNG_SENDEN);
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

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof String s) {
            try {
                return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ignored) {
            }
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    class RouteGenerierenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_ROUTE_GENERIEREN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String startLocation = String.valueOf(externalTask.getVariable("startLocation"));
                String endLocation = String.valueOf(externalTask.getVariable("endLocation"));
                String difficulty = String.valueOf(externalTask.getVariable("difficulty"));

                if (startLocation == null || "null".equals(startLocation) || startLocation.trim().isEmpty()) {
                    externalTaskService.handleFailure(
                        externalTask,
                        "startLocation ist erforderlich",
                        "Missing required variable 'startLocation'",
                        0,
                        0L
                    );
                    return;
                }

                if (endLocation == null || "null".equals(endLocation) || endLocation.trim().isEmpty()) {
                    externalTaskService.handleFailure(
                        externalTask,
                        "endLocation ist erforderlich",
                        "Missing required variable 'endLocation'",
                        0,
                        0L
                    );
                    return;
                }

                if (difficulty == null || "null".equals(difficulty)) {
                    difficulty = "medium";
                }

                RouteRequest request = new RouteRequest(startLocation, endLocation, difficulty);
                RouteResponse response = routeGenerationService.generateRoute(request);

                String responseJson = objectMapper.writeValueAsString(response);
                LOG.infof("Generated route: %s", responseJson.substring(0, Math.min(200, responseJson.length())));

                Map<String, Object> out = new HashMap<>();
                out.put("routen_empfehlung", responseJson);
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_ROUTE_GENERIEREN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_ROUTE_GENERIEREN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Route generieren fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    class RouteEmailErstellenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_ROUTE_EMAIL_ERSTELLEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                Object routeObj = externalTask.getVariable("routen_empfehlung");
                LOG.infof("Route variable type: %s, value: %s",
                    routeObj == null ? "null" : routeObj.getClass().getName(),
                    routeObj == null ? "null" : routeObj.toString().substring(0, Math.min(200, routeObj.toString().length())));

                Map<String, Object> route = toMap(routeObj);
                LOG.infof("Parsed route map keys: %s", route.keySet());

                String customerFirstName = resolveCustomerFirstName(externalTask);
                String startLocation = String.valueOf(externalTask.getVariable("startLocation") == null ? "" : externalTask.getVariable("startLocation"));
                String endLocation = String.valueOf(externalTask.getVariable("endLocation") == null ? "" : externalTask.getVariable("endLocation"));
                String difficulty = String.valueOf(externalTask.getVariable("difficulty") == null ? "" : externalTask.getVariable("difficulty"));

                StringBuilder sb = new StringBuilder();
                sb.append("Hallo");
                if (!customerFirstName.isEmpty()) {
                    sb.append(" ").append(customerFirstName);
                }
                sb.append("!\n\n");
                sb.append("Deine individuelle Routenempfehlung ist fertig:\n\n");
                sb.append("Start: ").append(startLocation).append("\n");
                sb.append("Ziel: ").append(endLocation).append("\n");
                sb.append("Schwierigkeit: ").append(difficulty).append("\n\n");

                if (route.containsKey("totalDistance")) {
                    sb.append("Distanz: ").append(route.get("totalDistance")).append(" km\n");
                }
                if (route.containsKey("elevationGain")) {
                    sb.append("Höhenmeter: ").append(route.get("elevationGain")).append(" m\n");
                }

                sb.append("\nRoutendetails:\n");
                sb.append("==============\n\n");

                if (route.containsKey("coordinates")) {
                    Object coords = route.get("coordinates");
                    LOG.infof("Coordinates object type: %s", coords == null ? "null" : coords.getClass().getName());
                    if (coords instanceof List<?> list) {
                        sb.append("Wegpunkte: ").append(list.size()).append("\n\n");
                        LOG.infof("Found %d waypoints in coordinates list", list.size());

                        int index = 1;
                        for (Object coordObj : list) {
                            if (coordObj instanceof Map<?, ?> coordMap) {
                                sb.append("Punkt ").append(index).append(": ");
                                sb.append("Lat ").append(coordMap.get("lat"));
                                sb.append(", Lon ").append(coordMap.get("lon"));
                                sb.append(", Höhe ").append(coordMap.get("elevation")).append(" m");
                                sb.append("\n");
                                index++;
                            }
                        }
                    }
                }

                sb.append("\nViel Spaß auf deiner Tour!\n\n");
                sb.append("Viele Grüße\n");
                sb.append("Dein VeloStart Team\n");

                Map<String, Object> out = new HashMap<>();
                out.put("routeEmailText", sb.toString());
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_ROUTE_EMAIL_ERSTELLEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_ROUTE_EMAIL_ERSTELLEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Route-Email erstellen fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    class RouteEmailSendenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_ROUTE_EMAIL_SENDEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String text = String.valueOf(externalTask.getVariable("routeEmailText") == null ? "" : externalTask.getVariable("routeEmailText"));
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                LOG.infof("Mock send route email preview=%s", preview.replace("\n", " "));

                String status = "SENT";
                Integer webhookStatus = null;
                String webhookUrl = routeEmailWebhookUrl;
                if (webhookUrl != null && !webhookUrl.isBlank()) {
                    try {
                        String payloadJson = buildEmailDeliveryJson(
                            externalTask.getProcessInstanceId(),
                            externalTask.getId(),
                            status,
                            "ROUTE_EMAIL",
                            text
                        );
                        webhookStatus = postEmailDelivery(webhookUrl, payloadJson);
                        if (webhookStatus < 200 || webhookStatus >= 300) {
                            status = "SENT_WEBHOOK_FAILED";
                        } else {
                            status = "SENT_WEBHOOK";
                        }
                        LOG.infof("Route email webhook delivered url=%s status=%d", webhookUrl, webhookStatus);
                    } catch (Exception e) {
                        status = "SENT_WEBHOOK_FAILED";
                        LOG.warnf(e, "Route email webhook delivery failed url=%s", webhookUrl);
                    }
                }

                Map<String, Object> out = new HashMap<>();
                out.put("routeEmailSendStatus", status);
                out.put("routeEmailSendWebhookUrl", webhookUrl);
                if (webhookStatus != null) {
                    out.put("routeEmailSendWebhookStatus", webhookStatus);
                }
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_ROUTE_EMAIL_SENDEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_ROUTE_EMAIL_SENDEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Route-Email senden fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    class BewertungserinnerungErstellenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String customerFirstName = resolveCustomerFirstName(externalTask);
                String startLocation = String.valueOf(externalTask.getVariable("startLocation") == null ? "" : externalTask.getVariable("startLocation"));
                String endLocation = String.valueOf(externalTask.getVariable("endLocation") == null ? "" : externalTask.getVariable("endLocation"));

                StringBuilder sb = new StringBuilder();
                sb.append("Hallo");
                if (!customerFirstName.isEmpty()) {
                    sb.append(" ").append(customerFirstName);
                }
                sb.append("!\n\n");
                sb.append("Wir hoffen, deine Route von ").append(startLocation).append(" nach ").append(endLocation).append(" hat dir gefallen!\n\n");
                sb.append("Wir würden uns freuen, wenn du deine Erfahrungen mit uns teilst.\n");
                sb.append("Bewerte deine Route unter:\n\n");
                sb.append("https://velostart.de/chat\n\n");
                sb.append("Mit deinem Feedback können wir dir beim nächsten Mal noch personalisiertere Routen empfehlen!\n\n");
                sb.append("Viele Grüße\n");
                sb.append("Dein VeloStart Team\n");

                Map<String, Object> out = new HashMap<>();
                out.put("bewertungserinnerungText", sb.toString());
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_BEWERTUNGSERINNERUNG_ERSTELLEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Bewertungserinnerung erstellen fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }

    class BewertungserinnerungSendenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s", TOPIC_BEWERTUNGSERINNERUNG_SENDEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                String text = String.valueOf(externalTask.getVariable("bewertungserinnerungText") == null ? "" : externalTask.getVariable("bewertungserinnerungText"));
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                LOG.infof("Mock send bewertungserinnerung preview=%s", preview.replace("\n", " "));

                String status = "SENT";
                Integer webhookStatus = null;
                String webhookUrl = bewertungsbogenWebhookUrl;
                if (webhookUrl != null && !webhookUrl.isBlank()) {
                    try {
                        String payloadJson = buildEmailDeliveryJson(
                            externalTask.getProcessInstanceId(),
                            externalTask.getId(),
                            status,
                            "BEWERTUNGSERINNERUNG",
                            text
                        );
                        webhookStatus = postEmailDelivery(webhookUrl, payloadJson);
                        if (webhookStatus < 200 || webhookStatus >= 300) {
                            status = "SENT_WEBHOOK_FAILED";
                        } else {
                            status = "SENT_WEBHOOK";
                        }
                        LOG.infof("Bewertungserinnerung webhook delivered url=%s status=%d", webhookUrl, webhookStatus);
                    } catch (Exception e) {
                        status = "SENT_WEBHOOK_FAILED";
                        LOG.warnf(e, "Bewertungserinnerung webhook delivery failed url=%s", webhookUrl);
                    }
                }

                Map<String, Object> out = new HashMap<>();
                out.put("bewertungserinnerungSendStatus", status);
                out.put("bewertungserinnerungSendWebhookUrl", webhookUrl);
                if (webhookStatus != null) {
                    out.put("bewertungserinnerungSendWebhookStatus", webhookStatus);
                }
                externalTaskService.complete(externalTask, out);
                LOG.infof("Completed topic=%s taskId=%s", TOPIC_BEWERTUNGSERINNERUNG_SENDEN, externalTask.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s", TOPIC_BEWERTUNGSERINNERUNG_SENDEN, externalTask.getId());
                externalTaskService.handleFailure(
                    externalTask,
                    "Bewertungserinnerung senden fehlgeschlagen",
                    stackTrace(e),
                    3,
                    5_000L
                );
            }
        }
    }
}
