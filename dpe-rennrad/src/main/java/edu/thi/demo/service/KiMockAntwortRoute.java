//Niklas Putz

package edu.thi.demo.services;

import org.apache.camel.builder.RouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KiMockAntwortRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Endpoint fuer Backend-System (simuliert mit Postman)
        // Nimmt Nachricht entgegen und leitet sie an Camunda Intermediate Catch Event weiter
        from("platform-http:/api/wetter-antwort?httpMethodRestrict=POST")
                .routeId("wetter-antwort-von-backend")
                .log("=== Nachricht vom Backend-System empfangen ===")
                .log("Eingehende Wetter-Antwort: ${body}")
                // Bereinige die Nachricht (entferne Zeilenumbrueche und escape Anfuehrungszeichen)
                .setProperty("wetterNachricht", body())
                .process(exchange -> {
                    String nachricht = exchange.getProperty("wetterNachricht", String.class);
                    if (nachricht != null) {
                        // Entferne Zeilenumbrueche und escape Anfuehrungszeichen
                        nachricht = nachricht.replace("\r\n", " ")
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .replace("\"", "\\\"")
                                .trim();
                        exchange.setProperty("wetterNachricht", nachricht);
                    }
                })
                // Entferne HTTP-spezifische Header
                .removeHeader("CamelHttpPath")
                .removeHeader("CamelHttpUri")
                .removeHeader("CamelHttpUrl")
                .removeHeader("Content-Type")
                // Baue die Camunda Message-Anfrage mit der empfangenen Nachricht
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{ " +
                        "\"messageName\": \"Wetter_Daten_erhalten\", " +
                        "\"all\": true, " +
                        "\"processVariables\": { " +
                        "\"wetterAntwort\": { \"value\": \"${exchangeProperty.wetterNachricht}\", \"type\": \"String\" } " +
                        "} }"))
                .log("Sende Message 'Wetter_Daten_erhalten' an Camunda Process Engine...")
                .log("Camunda Request Body: ${body}")
                .to("http://localhost:8080/engine-rest/message?bridgeEndpoint=true")
                .log("=== Camunda Response: ${body} ===")
                // Bestaetigung zurueck an Postman
                .setBody(constant("{\"status\": \"success\", \"message\": \"Nachricht an Prozess weitergeleitet\"}"));
    }
}