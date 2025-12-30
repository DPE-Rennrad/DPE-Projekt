package edu.thi.demo.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Path("/kunde")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KundenAktionResource {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @ConfigProperty(name = "camunda.engine-rest.url", defaultValue = "http://localhost:8080/engine-rest")
    String engineRestUrl;

    @POST
    @Path("/deinstallieren/{processInstanceId}")
    public Response appDeinstallieren(@PathParam("processInstanceId") String processInstanceId) {
        String engineUrl = engineRestUrl + "/process-instance/" + processInstanceId + "/variables/deinstallieren";
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(engineUrl))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                    "{\"value\": true, \"type\": \"Boolean\"}"
                ))
                .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 204) {
                return Response.ok()
                    .entity("{\"message\": \"Deinstallation erfolgreich signalisiert\"}")
                    .build();
            } else {
                return Response.status(response.statusCode())
                    .entity("{\"error\": \"Fehler beim Setzen der Variable\", \"details\": \"" 
                        + response.body() + "\"}")
                    .build();
            }
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
