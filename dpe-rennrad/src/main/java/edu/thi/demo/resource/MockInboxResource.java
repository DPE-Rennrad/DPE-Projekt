package edu.thi.demo.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Path("/mock-inbox")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MockInboxResource {

    private static final int MAX_ITEMS = 200;

    private final ConcurrentLinkedDeque<Map<String, Object>> newsletterDeliveries = new ConcurrentLinkedDeque<>();

    public static class NewsletterDeliveryRequest {
        public String processInstanceId;
        public String taskId;
        public String status;
        public String newsletterText;
    }

    @POST
    @Path("/newsletter")
    public Response receiveNewsletter(NewsletterDeliveryRequest body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("processInstanceId", body == null ? null : body.processInstanceId);
        item.put("taskId", body == null ? null : body.taskId);
        item.put("status", body == null ? null : body.status);
        item.put("newsletterText", body == null ? null : body.newsletterText);

        newsletterDeliveries.addFirst(item);
        while (newsletterDeliveries.size() > MAX_ITEMS) {
            newsletterDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", newsletterDeliveries.size())).build();
    }

    @GET
    @Path("/newsletter")
    public List<Map<String, Object>> listNewsletters(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : newsletterDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    @GET
    @Path("/newsletter/latest")
    public Response latestNewsletter() {
        Map<String, Object> first = newsletterDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No newsletter received yet")).build();
        }
        return Response.ok(first).build();
    }
}
