package edu.thi.demo.camunda;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;

@Startup
@ApplicationScoped
public class MotivationExternalTaskWorker {

    private static final Logger LOG = Logger.getLogger(MotivationExternalTaskWorker.class);

    static final String TOPIC_SPRUCH_ERSTELLEN = "spruch-erstellen";
    static final String TOPIC_KUNDE_ABFRAGEN = "kunde-abfragen";
    // NEU: Topic für das Versenden
    static final String TOPIC_NACHRICHT_VERSENDEN = "nachricht-versenden";

    @ConfigProperty(name = "camunda.engine-rest.url", defaultValue = "http://localhost:8080/engine-rest")
    String engineRestUrl;

    static final String WORKER_ID = "quarkus-motivation-worker";

    private final ObjectMapper objectMapper;
    private ExternalTaskClient client;
    private final Random random;

    // Wortlisten ... (unverändert)
    private static final String[] SUBJEKTE = {"Du", "Dein Traum", "Deine Kraft", "Dein Wille", "Deine Leidenschaft"};
    private static final String[] VERBEN = {"erreicht", "erobert", "verwirklicht", "schafft", "erschafft", "meistert"};
    private static final String[] ADJEKTIVE = {"großartige", "unglaubliche", "wunderbare", "fantastische", "beeindruckende", "außergewöhnliche"};
    private static final String[] OBJEKTE = {"Ziele", "Träume", "Erfolge", "Möglichkeiten", "Chancen", "Herausforderungen"};
    private static final String[] ANFÄNGE = {"Glaube an dich", "Vertraue dir selbst", "Sei mutig", "Bleib stark", "Niemals aufgeben"};
    private static final String[] ENDEN = {"und alles wird möglich", "denn du bist stärker als du denkst", "und der Erfolg wird folgen", "denn du verdienst es", "und du wirst siegen"};

    public MotivationExternalTaskWorker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.random = new Random();
    }

    @PostConstruct
    void start() {
        LOG.infof("Starting External Task workerId=%s baseUrl=%s", WORKER_ID, engineRestUrl);
        client = ExternalTaskClient.create()
                .baseUrl(engineRestUrl)
                .workerId(WORKER_ID)
                .asyncResponseTimeout(10_000)
                .build();

        // Korrektur: Spruch-Handler dem richtigen Topic zuordnen
        client.subscribe(TOPIC_SPRUCH_ERSTELLEN)
                .lockDuration(30_000)
                .handler(new SpruchErstellenHandler())
                .open();
        LOG.infof("Subscribed topic=%s", TOPIC_SPRUCH_ERSTELLEN);

        // 3. NEU: Nachricht versenden
        client.subscribe(TOPIC_NACHRICHT_VERSENDEN)
                .lockDuration(30_000)
                .handler(new NachrichtVersendenHandler())
                .open();

        LOG.infof("Subscribed to topics: %s, %s, %s",
                TOPIC_SPRUCH_ERSTELLEN, TOPIC_KUNDE_ABFRAGEN, TOPIC_NACHRICHT_VERSENDEN);
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

    // Hilfsmethoden generiereSpruch() etc. (unverändert)
    public String generiereSpruch() {
        int spruchTyp = random.nextInt(3);
        switch (spruchTyp) {
            case 0: return generiereTyp1();
            case 1: return generiereTyp2();
            case 2: return generiereTyp3();
            default: return generiereTyp1();
        }
    }
    private String generiereTyp1() { String subjekt = SUBJEKTE[random.nextInt(SUBJEKTE.length)]; String verb = VERBEN[random.nextInt(VERBEN.length)]; String adjektiv = ADJEKTIVE[random.nextInt(ADJEKTIVE.length)]; String objekt = OBJEKTE[random.nextInt(OBJEKTE.length)]; return subjekt + " " + verb + " " + adjektiv + " " + objekt + "!"; }
    private String generiereTyp2() { String anfang = ANFÄNGE[random.nextInt(ANFÄNGE.length)]; String ende = ENDEN[random.nextInt(ENDEN.length)]; return anfang + ", " + ende + "!"; }
    private String generiereTyp3() { String adjektiv = ADJEKTIVE[random.nextInt(ADJEKTIVE.length)]; String objekt = OBJEKTE[random.nextInt(OBJEKTE.length)]; String verb = VERBEN[random.nextInt(VERBEN.length)]; String subjekt = SUBJEKTE[random.nextInt(SUBJEKTE.length)].toLowerCase(); return adjektiv.substring(0, 1).toUpperCase() + adjektiv.substring(1) + " " + objekt + " " + verb + " " + subjekt + "!"; }

    // Der ursprüngliche Handler wurde umbenannt, da er Spruch-Logik enthält
    class SpruchErstellenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s", TOPIC_SPRUCH_ERSTELLEN, externalTask.getId());
            try {
                String motivationsSpruch = generiereSpruch();
                Map<String, Object> out = new HashMap<>();
                out.put("motivationsSpruch", motivationsSpruch);
                externalTaskService.complete(externalTask, out);

                LOG.infof("Erfolgreich abgeschlossen: %s", motivationsSpruch);

            } catch (Exception e) {
                externalTaskService.handleFailure(externalTask, "Fehler beim Spruch", stackTrace(e), 3, 5000L);
            }
        }
    }

    class KundeAbfragenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            // Wir holen uns die ID, BEVOR wir den Task abschließen
            final String processInstanceId = externalTask.getProcessInstanceId();

            LOG.infof("Sende Task: Anfrage an Kunde wird simuliert... (ID: %s)", processInstanceId);

            try {
                // 1. Hier würdest du normalerweise die externe API aufrufen.
                // Wir entscheiden hier schonmal im Voraus, ob der Kunde "antworten" wird.
                boolean kundeWirdAntworten = ThreadLocalRandom.current().nextBoolean();

                // 2. Den External Task SOFORT abschließen.
                // Damit wandert der Token im BPMN zum nächsten Element (dem Catch-Event).
                externalTaskService.complete(externalTask);
                LOG.info("Sende Task abgeschlossen. Token wartet nun beim Catch Event.");

                // 3. Die Simulation der externen Antwort (asynchron)
                if (kundeWirdAntworten) {
                    new Thread(() -> {
                        try {
                            // Wir warten 2 Sekunden, um die Verzögerung der Antwort zu simulieren
                            // In dieser Zeit hat Camunda das Token sicher auf das Catch-Event geschoben.
                            Thread.sleep(2000);

                            LOG.info("Simuliere: Kunde antwortet jetzt!");
                            triggerMessageEvent(processInstanceId, "MotivationTriggerMessage");
                        } catch (Exception e) {
                            LOG.error("Fehler bei der Antwort-Simulation", e);
                        }
                    }).start();
                } else {
                    LOG.info("Simuliere: Kunde antwortet nicht. Timer wird irgendwann auslösen.");
                }

            } catch (Exception e) {
                LOG.error("Fehler im Sende-Task", e);
                externalTaskService.handleFailure(externalTask, "Senden fehlgeschlagen", stackTrace(e), 0, 0L);
            }
        }
        private void triggerMessageEvent(String processInstanceId, String messageName) {
            try {
                // JSON Body für Camunda REST API
                String jsonBody = String.format(
                        "{\"messageName\": \"%s\", \"processInstanceId\": \"%s\"}",
                        messageName, processInstanceId
                );

                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(engineRestUrl + "/message"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    LOG.infof("Message '%s' erfolgreich an Camunda gesendet.", messageName);
                } else {
                    LOG.errorf("Fehler beim Senden der Nachricht: Status %d - %s", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                LOG.error("Kritischer Fehler beim HTTP-Call zu Camunda", e);
            }
        }
    }

    class NachrichtVersendenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s", TOPIC_NACHRICHT_VERSENDEN, externalTask.getId());

            try {
                // Hier holen wir den Spruch aus den Prozessvariablen
                String spruch = externalTask.getVariable("motivationsSpruch");

                if (spruch != null) {
                    LOG.info("--------------------------------------------------");
                    LOG.infof("VERSENDE NACHRICHT: %s", spruch);
                    LOG.info("--------------------------------------------------");

                    // Hier käme dein Code für E-Mail, SMS oder WhatsApp hin

                    externalTaskService.complete(externalTask);
                    LOG.infof("Task %s erfolgreich abgeschlossen.", TOPIC_NACHRICHT_VERSENDEN);
                } else {
                    LOG.warn("Kein Motivationsspruch gefunden!");
                    externalTaskService.handleBpmnError(externalTask, "NO_QUOTE_FOUND", "Spruch war leer");
                }

            } catch (Exception e) {
                LOG.error("Fehler beim Versenden der Nachricht", e);
                externalTaskService.handleFailure(externalTask, "Versandfehler", stackTrace(e), 2, 5000L);
            }
        }
    }

}