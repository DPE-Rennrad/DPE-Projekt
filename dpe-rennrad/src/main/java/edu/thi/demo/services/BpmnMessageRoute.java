package edu.thi.demo.services;

import org.apache.camel.builder.RouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BpmnMessageRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("platform-http:/api/trigger-bpmn?httpMethodRestrict=POST")
                .routeId("trigger-bpmn-endpoint")
                .log("=== Nachricht empfangen ===")
                .log("Body: ${body}")
                // Entferne HTTP-spezifische Header, die den Pfad enthalten
                .removeHeader("CamelHttpPath")
                .removeHeader("CamelHttpUri")
                .removeHeader("CamelHttpUrl")
                .setHeader("Content-Type", constant("application/json"))
                .setBody(constant("{ \"messageName\": \"Messege_DataReceived\", \"resultEnabled\": true }"))
                .log("Sende an Process Engine...")
                .to("http://localhost:8080/engine-rest/message?bridgeEndpoint=true")
                .log("=== Response erhalten: ${body} ===");
    }
}