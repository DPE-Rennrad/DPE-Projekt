package edu.thi.demo.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Apache Camel Routes for Content-Based Routing with bidirectional XML/JSON transformation
 * 
 * Anforderung 2: Mittels Apache Camel die Nachricht des Prozesses aus JMS-Queue lesen 
 * und an mehrere Systeme (Dateisystem, DB) routen mit XML/JSON Transformation
 */
@ApplicationScoped
public class ProviderRoutingRoutes extends RouteBuilder {

    private static final Logger LOG = Logger.getLogger(ProviderRoutingRoutes.class);

    @ConfigProperty(name = "jms.queue.provider-requests")
    String providerRequestsQueue;

    @ConfigProperty(name = "jms.queue.provider-responses")
    String providerResponsesQueue;

    @ConfigProperty(name = "camel.file.output.path", defaultValue = "target/camel-output")
    String fileOutputPath;

    @ConfigProperty(name = "camel.file.input.path", defaultValue = "target/camel-input")
    String fileInputPath;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void configure() throws Exception {
        
        // Global Error Handler
        onException(Exception.class)
            .handled(true)
            .log("ERROR processing message: ${exception.message}")
            .to("log:errors");

        /**
         * Route 1: Read from provider-requests queue and route based on providerId
         * Content-Based Routing: Different providers go to different destinations
         */
        from("jms:queue:" + providerRequestsQueue)
            .routeId("provider-request-router")
            .log("Received provider request: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .choice()
                // Provider 1 -> File System (JSON format)
                .when(simple("${body[providerId]} == '1'"))
                    .log("Routing Provider 1 to file system (JSON)")
                    .marshal().json(JsonLibrary.Jackson)
                    .setHeader(Exchange.FILE_NAME, simple("provider-1-${date:now:yyyyMMdd-HHmmss}.json"))
                    .to("file:" + fileOutputPath + "/provider1?charset=utf-8")
                    .process(this::createMockResponse)
                    .to("jms:queue:" + providerResponsesQueue)
                // Provider 2 -> File System (XML format)
                .when(simple("${body[providerId]} == '2'"))
                    .log("Routing Provider 2 to file system (XML)")
                    .process(this::convertJsonToXml)
                    .setHeader(Exchange.FILE_NAME, simple("provider-2-${date:now:yyyyMMdd-HHmmss}.xml"))
                    .to("file:" + fileOutputPath + "/provider2?charset=utf-8")
                    .process(this::createMockResponse)
                    .to("jms:queue:" + providerResponsesQueue)
                // Provider 3 -> Database (would be SQL insert)
                .when(simple("${body[providerId]} == '3'"))
                    .log("Routing Provider 3 to database (simulated)")
                    .marshal().json(JsonLibrary.Jackson)
                    .setHeader(Exchange.FILE_NAME, simple("provider-3-db-${date:now:yyyyMMdd-HHmmss}.json"))
                    .to("file:" + fileOutputPath + "/database-simulation?charset=utf-8")
                    .process(this::createMockResponse)
                    .to("jms:queue:" + providerResponsesQueue)
                // Default -> Log
                .otherwise()
                    .log("Unknown provider ID: ${body[providerId]}")
            .end();

        /**
         * Route 2: Read files from input directory and put into response queue
         * Anforderung 3: Daten aus Dateisystem lesen und in JMS-Queue ablegen
         */
        from("file:" + fileInputPath + "?noop=true&delay=5000")
            .routeId("file-to-jms-router")
            .log("Reading file: ${header.CamelFileName}")
            .choice()
                // JSON files
                .when(simple("${header.CamelFileName} ends with '.json'"))
                    .log("Processing JSON file")
                    .convertBodyTo(String.class)
                    .setHeader("messageType", constant("PROVIDER_RESPONSE"))
                    .to("jms:queue:" + providerResponsesQueue)
                // XML files - convert to JSON first
                .when(simple("${header.CamelFileName} ends with '.xml'"))
                    .log("Processing XML file - converting to JSON")
                    .process(this::convertXmlToJson)
                    .setHeader("messageType", constant("PROVIDER_RESPONSE"))
                    .to("jms:queue:" + providerResponsesQueue)
                .otherwise()
                    .log("Skipping non-JSON/XML file: ${header.CamelFileName}")
            .end();
    }

    /**
     * Convert JSON Map to XML String
     */
    private void convertJsonToXml(Exchange exchange) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = exchange.getIn().getBody(Map.class);
            
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<providerRequest>\n");
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                xml.append("  <").append(entry.getKey()).append(">")
                   .append(entry.getValue())
                   .append("</").append(entry.getKey()).append(">\n");
            }
            
            xml.append("</providerRequest>");
            
            exchange.getIn().setBody(xml.toString());
            LOG.info("Converted JSON to XML");
        } catch (Exception e) {
            LOG.error("Failed to convert JSON to XML", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert XML String to JSON (simplified)
     */
    private void convertXmlToJson(Exchange exchange) {
        try {
            String xml = exchange.getIn().getBody(String.class);
            
            // Simple XML parsing (for demo - in production use proper XML parser)
            Map<String, Object> jsonMap = new java.util.HashMap<>();
            jsonMap.put("receivedEvents", java.util.List.of());
            jsonMap.put("enoughAnswers", true);
            jsonMap.put("source", "xml-file");
            jsonMap.put("originalXml", xml);
            
            String json = objectMapper.writeValueAsString(jsonMap);
            exchange.getIn().setBody(json);
            LOG.info("Converted XML to JSON");
        } catch (Exception e) {
            LOG.error("Failed to convert XML to JSON", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create mock response for provider request
     */
    private void createMockResponse(Exchange exchange) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> request = exchange.getIn().getBody(Map.class);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("receivedEvents", java.util.List.of(
                createMockEvent("Event 1 from Provider " + request.get("providerId")),
                createMockEvent("Event 2 from Provider " + request.get("providerId"))
            ));
            response.put("enoughAnswers", true);
            response.put("processInstanceId", exchange.getIn().getHeader("processInstanceId", String.class));
            
            String json = objectMapper.writeValueAsString(response);
            exchange.getIn().setBody(json);
            exchange.getIn().setHeader("processInstanceId", request.get("processInstanceId"));
            
            LOG.info("Created mock response for provider " + request.get("providerId"));
        } catch (Exception e) {
            LOG.error("Failed to create mock response", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> createMockEvent(String title) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("title", title);
        event.put("date", java.time.Instant.now().toString());
        event.put("location", "Mock-City");
        event.put("passendeLeute", java.util.List.of("Peter", "Maria"));
        return event;
    }
}

// Eduard Merker