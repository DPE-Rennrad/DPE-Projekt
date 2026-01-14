package edu.thi.demo.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class JmsMessageProducer {

    private static final Logger LOG = Logger.getLogger(JmsMessageProducer.class);

    @Inject
    @Named("jmsConnectionFactory")
    ConnectionFactory connectionFactory;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "jms.queue.provider-requests")
    String providerRequestsQueue;

    @ConfigProperty(name = "jms.queue.provider-responses")
    String providerResponsesQueue;

    /**
     * Sends a provider request message to JMS queue
     * @param providerData Map containing provider information
     * @param processInstanceId The Camunda process instance ID for correlation
     */
    public void sendProviderRequest(Map<String, Object> providerData, String processInstanceId) {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Queue queue = session.createQueue(providerRequestsQueue);
            MessageProducer producer = session.createProducer(queue);

            // Create JSON message
            String jsonPayload = objectMapper.writeValueAsString(providerData);
            TextMessage message = session.createTextMessage(jsonPayload);
            
            // Add correlation properties
            message.setStringProperty("processInstanceId", processInstanceId);
            message.setStringProperty("messageType", "PROVIDER_REQUEST");
            message.setStringProperty("providerId", String.valueOf(providerData.get("providerId")));

            producer.send(message);
            LOG.infof("Sent provider request to queue %s: processInstanceId=%s", providerRequestsQueue, processInstanceId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send provider request");
            throw new RuntimeException("JMS send failed", e);
        }
    }

    /**
     * Sends a provider response message to JMS queue (simulates external system response)
     * @param responseData Map containing response data
     * @param processInstanceId The Camunda process instance ID for correlation
     */
    public void sendProviderResponse(Map<String, Object> responseData, String processInstanceId) {
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Queue queue = session.createQueue(providerResponsesQueue);
            MessageProducer producer = session.createProducer(queue);

            String jsonPayload = objectMapper.writeValueAsString(responseData);
            TextMessage message = session.createTextMessage(jsonPayload);
            
            message.setStringProperty("processInstanceId", processInstanceId);
            message.setStringProperty("messageType", "PROVIDER_RESPONSE");

            producer.send(message);
            LOG.infof("Sent provider response to queue %s: processInstanceId=%s", providerResponsesQueue, processInstanceId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send provider response");
            throw new RuntimeException("JMS send failed", e);
        }
    }
}
