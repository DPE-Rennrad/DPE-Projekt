package edu.thi.demo.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class JmsMessageConsumer {

    private static final Logger LOG = Logger.getLogger(JmsMessageConsumer.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @Inject
    @Named("jmsConnectionFactory")
    ConnectionFactory connectionFactory;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "jms.queue.provider-responses")
    String providerResponsesQueue;

    @ConfigProperty(name = "camunda.engine-rest.url")
    String engineRestUrl;

    /**
     * Polls JMS queue for provider responses and correlates them with Camunda process
     */
    public void startConsumer() {
        new Thread(() -> {
            try (Connection connection = connectionFactory.createConnection()) {
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue(providerResponsesQueue);
                MessageConsumer consumer = session.createConsumer(queue);

                LOG.infof("Started JMS consumer for queue: %s", providerResponsesQueue);

                consumer.setMessageListener(message -> {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String body = textMessage.getText();
                            String processInstanceId = textMessage.getStringProperty("processInstanceId");

                            LOG.infof("Received response: processInstanceId=%s", processInstanceId);

                            // Correlate message with Camunda process
                            correlateMessageToCamunda(body, processInstanceId);
                        }
                    } catch (Exception e) {
                        LOG.errorf(e, "Error processing JMS message");
                    }
                });

            } catch (Exception e) {
                LOG.errorf(e, "JMS Consumer failed");
            }
        }, "JMS-Consumer-Thread").start();
    }

    private void correlateMessageToCamunda(String responseBody, String processInstanceId) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = objectMapper.readValue(responseBody, Map.class);

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("messageName", "event");
        messagePayload.put("processInstanceId", processInstanceId);

        Map<String, Object> processVariables = new HashMap<>();
        String eventsJson = objectMapper.writeValueAsString(responseData.get("receivedEvents"));
        processVariables.put("receivedEvents", Map.of("value", eventsJson, "type", "Object", 
            "valueInfo", Map.of("objectTypeName", "java.util.List", "serializationDataFormat", "application/json")));
        processVariables.put("enoughAnswers", Map.of("value", responseData.get("enoughAnswers"), "type", "Boolean"));
        messagePayload.put("processVariables", processVariables);

        String payloadJson = objectMapper.writeValueAsString(messagePayload);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(engineRestUrl + "/message"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            LOG.infof("Message correlated successfully via JMS. Status: %d", response.statusCode());
        } else {
            LOG.warnf("Message correlation failed. Status: %d, Response: %s", response.statusCode(), response.body());
        }
    }
}
