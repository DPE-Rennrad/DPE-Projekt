package edu.thi.demo.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.thi.demo.model.Offer;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.Variables;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * External Task Worker for Produktempfehlung process
 * Pallmann Florian
 */
@Startup
@ApplicationScoped
public class ProduktempfehlungExternalTaskWorker {
    private static final Logger LOG = Logger.getLogger(ProduktempfehlungExternalTaskWorker.class);

    static final String TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI = "kundenempfehlungen-abgleichen-ki";
    static final String TOPIC_PRODUKTEMPFEHLUNG_LINK = "produktempfehlung-link";
    static final String TOPIC_PRODUKTEMPFEHLUNG_SENDEN = "produktempfehlung-senden";
    static final String WORKER_ID = "quarkus-produktempfehlung-worker";

    @ConfigProperty(name = "camunda.engine-rest.url", defaultValue = "http://localhost:8080/engine-rest")
    String engineRestUrl;

    private ExternalTaskClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void start() {
        LOG.infof("Starting External Task workerId=%s baseUrl=%s", WORKER_ID, engineRestUrl);

        client = ExternalTaskClient.create()
                .baseUrl(engineRestUrl)
                .workerId(WORKER_ID)
                .asyncResponseTimeout(10_000)
                .build();

        client.subscribe(TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI)
                .lockDuration(30_000)
                .handler(new ProduktempfehlungAbgleichenKiHandler())
                .open();
        LOG.infof("Subscribed topic=%s", TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI);

        client.subscribe(TOPIC_PRODUKTEMPFEHLUNG_LINK)
                .lockDuration(30_000)
                .handler(new ProduktempfehlungLinkHandler())
                .open();
        LOG.infof("Subscribed topic=%s", TOPIC_PRODUKTEMPFEHLUNG_LINK);

        client.subscribe(TOPIC_PRODUKTEMPFEHLUNG_SENDEN)
                .lockDuration(30_000)
                .handler(new ProduktempfehlungSendenHandler())
                .open();
        LOG.infof("Subscribed topic=%s", TOPIC_PRODUKTEMPFEHLUNG_SENDEN);
    }

    @PreDestroy
    void stop() {
        if (client != null) {
            client.stop();
            LOG.info("External Task Client stopped");
        }
    }

    /**
     * Helper: Extract Offer from process variable (handles both String JSON and Object)
     */
    private Offer extractOffer(Object offerObj) throws Exception {
        if (offerObj == null) {
            throw new IllegalArgumentException("Offer variable is null");
        }

        if (offerObj instanceof String) {
            // JSON String - deserialize
            LOG.debug("Offer ist String (JSON), deserialisiere...");
            return objectMapper.readValue((String) offerObj, Offer.class);
        } else {
            // Object - convert
            LOG.debug("Offer ist Objekt, konvertiere...");
            return objectMapper.convertValue(offerObj, Offer.class);
        }
    }

    class ProduktempfehlungAbgleichenKiHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s",
                    TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                // extract Offer
                Object offerObj = externalTask.getVariable("offer");
                Offer offer = extractOffer(offerObj);

                LOG.infof("KI-Abgleich für Offer: title=%s, price=%.2f, discount=%d%%",
                        offer.title, offer.price, offer.discountPercent);

                // Mock KI-Score
                double kiScore = mockKiScore(offer);

                Map<String, Object> variables = new HashMap<>();
                variables.put("kiScore", kiScore);
                variables.put(
                        "offer",
                        Variables.objectValue(offer)
                                .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                                .create()
                );

                externalTaskService.complete(externalTask, variables);
                LOG.infof("Completed topic=%s. Relevant=%s, Score=%.2f",
                        TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI, kiScore);

            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s",
                        TOPIC_PRODUKTEMPFEHLUNG_ABGLEICHEN_KI, externalTask.getId());
                externalTaskService.handleFailure(
                        externalTask,
                        "Fehler beim KI-Abgleich",
                        stackTrace(e),
                        3,
                        10_000L
                );
            }
        }

        private double mockKiScore(Offer offer) {
            if(offer.crawled){
                if (offer.discountPercent == null) return 0.0;
                return Math.min(offer.discountPercent / 100.0, 1.0);
            }
            else {
                return 1.0;
            }

        }
    }

    class ProduktempfehlungLinkHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s",
                    TOPIC_PRODUKTEMPFEHLUNG_LINK, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                Object offerObj = externalTask.getVariable("offer");
                Offer offer = extractOffer(offerObj);

                LOG.infof("Link-Transformation für: %s", offer.title);

                // Affiliate-Link
                String affiliateUrl = offer.url + ".affiliate.com";
                offer.url = affiliateUrl;

                Map<String, Object> variables = new HashMap<>();
                variables.put(
                        "offer",
                        Variables.objectValue(offer)
                                .serializationDataFormat(Variables.SerializationDataFormats.JSON.getName())
                                .create()
                );
                variables.put("affiliateUrl", affiliateUrl);

                externalTaskService.complete(externalTask, variables);
                LOG.infof("Completed topic=%s. Affiliate-Link: %s",
                        TOPIC_PRODUKTEMPFEHLUNG_LINK, affiliateUrl);

            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s",
                        TOPIC_PRODUKTEMPFEHLUNG_LINK, externalTask.getId());
                externalTaskService.handleFailure(
                        externalTask,
                        "Fehler bei Link-Transformation",
                        stackTrace(e),
                        3,
                        10_000L
                );
            }
        }
    }

    class ProduktempfehlungSendenHandler implements ExternalTaskHandler {
        @Override
        public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
            LOG.infof("Handling topic=%s taskId=%s processInstanceId=%s",
                    TOPIC_PRODUKTEMPFEHLUNG_SENDEN, externalTask.getId(), externalTask.getProcessInstanceId());
            try {
                Object offerObj = externalTask.getVariable("offer");
                Offer offer = extractOffer(offerObj);

                String customerEmail = String.valueOf(
                        externalTask.getVariable("kundeEmail") != null
                                ? externalTask.getVariable("kundeEmail")
                                : "kunde@example.com"
                );

                LOG.infof("Sende Produktempfehlung an: %s", customerEmail);

                // Mock: send email
                boolean emailSent = mockSendEmail(customerEmail, offer);

                Map<String, Object> variables = new HashMap<>();
                variables.put("emailSent", emailSent);
                variables.put("sentAt", java.time.LocalDateTime.now().toString());

                externalTaskService.complete(externalTask, variables);
                LOG.infof("Completed topic=%s. Email sent=%s",
                        TOPIC_PRODUKTEMPFEHLUNG_SENDEN, emailSent);

            } catch (Exception e) {
                LOG.errorf(e, "Failed topic=%s taskId=%s",
                        TOPIC_PRODUKTEMPFEHLUNG_SENDEN, externalTask.getId());
                externalTaskService.handleFailure(
                        externalTask,
                        "Fehler beim Senden",
                        stackTrace(e),
                        3,
                        10_000L
                );
            }
        }

        private boolean mockSendEmail(String email, Offer offer) {
            LOG.infof("Email an %s:\n" +
                            "   Produkt: %s\n" +
                            "   Marke: %s\n" +
                            "   Preis: %.2f EUR (vorher: %.2f EUR)\n" +
                            "   Rabatt: %d%%\n" +
                            "   Link: %s",
                    email,
                    offer.title,
                    offer.brand,
                    offer.price,
                    offer.originalPrice,
                    offer.discountPercent,
                    offer.url
            );
            return true;
        }
    }

    private static String stackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}