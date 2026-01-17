package edu.thi.demo.services;

import org.apache.camel.builder.RouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KiMockAntwortRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("platform-http:/api/trigger-wetter-daten?httpMethodRestrict=POST")
                .routeId("trigger-wetter-daten-endpoint")
                .log("=== Wetter Daten Nachricht empfangen ===")
                .log("Body: ${body}")
                // Entferne HTTP-spezifische Header, die den Pfad enthalten
                .removeHeader("CamelHttpPath")
                .removeHeader("CamelHttpUri")
                .removeHeader("CamelHttpUrl")
                .setHeader("Content-Type", constant("application/json"))
                .setBody(constant("{ \"messageName\": \"Wetter_Daten_erhalten\", \"resultEnabled\": true }"))
                .log("Sende Wetter Daten an Process Engine...")
                .to("http://localhost:8080/engine-rest/message?bridgeEndpoint=true")
                .log("=== Response erhalten: ${body} ===");
    }
}