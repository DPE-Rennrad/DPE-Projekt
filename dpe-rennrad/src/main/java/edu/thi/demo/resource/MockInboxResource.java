package edu.thi.demo.resource;

import edu.thi.demo.model.Offer;
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
    private final ConcurrentLinkedDeque<Map<String, Object>> routeEmailDeliveries = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> bewertungsbogenDeliveries = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> produktempfehlungDeliveries = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> verhinderungsabfrageDeliveries = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> kiWetterabfrageDeliveries = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> motivationsergebnisDeliveries = new ConcurrentLinkedDeque<>();

    public static class NewsletterDeliveryRequest {
        public String processInstanceId;
        public String taskId;
        public String status;
        public String newsletterText;
    }

    public static class EmailDeliveryRequest {
        public String processInstanceId;
        public String taskId;
        public String status;
        public String emailType;
        public String emailText;
    }

    public static class ProduktempfehlungDeliveryRequest {
        public String processInstanceId;
        public String kundeEmail;
        public Offer offer;
        public String sentAt;
    }

    public static class VerhinderungsabfrageDeliveryRequest {
        public String message;
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

    //Methode geschrieben von Felix Sewald
    @POST
    @Path("/route")
    public Response receiveRouteEmail(EmailDeliveryRequest body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("processInstanceId", body == null ? null : body.processInstanceId);
        item.put("taskId", body == null ? null : body.taskId);
        item.put("status", body == null ? null : body.status);
        item.put("emailType", body == null ? null : body.emailType);
        item.put("emailText", body == null ? null : body.emailText);

        routeEmailDeliveries.addFirst(item);
        while (routeEmailDeliveries.size() > MAX_ITEMS) {
            routeEmailDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", routeEmailDeliveries.size())).build();
    }

    //Methode geschrieben von Felix Sewald
    @GET
    @Path("/route")
    public List<Map<String, Object>> listRouteEmails(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : routeEmailDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    //Methode geschrieben von Felix Sewald
    @GET
    @Path("/route/latest")
    public Response latestRouteEmail() {
        Map<String, Object> first = routeEmailDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No route email received yet")).build();
        }
        return Response.ok(first).build();
    }

    //Methode geschrieben von Felix Sewald
    @POST
    @Path("/bewertungsbogen")
    public Response receiveBewertungsbogen(EmailDeliveryRequest body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("processInstanceId", body == null ? null : body.processInstanceId);
        item.put("taskId", body == null ? null : body.taskId);
        item.put("status", body == null ? null : body.status);
        item.put("emailType", body == null ? null : body.emailType);
        item.put("emailText", body == null ? null : body.emailText);

        bewertungsbogenDeliveries.addFirst(item);
        while (bewertungsbogenDeliveries.size() > MAX_ITEMS) {
            bewertungsbogenDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", bewertungsbogenDeliveries.size())).build();
    }

    //Methode geschrieben von Felix Sewald
    @GET
    @Path("/bewertungsbogen")
    public List<Map<String, Object>> listBewertungsboegen(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : bewertungsbogenDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    //Methode geschrieben von Felix Sewald
    @GET
    @Path("/bewertungsbogen/latest")
    public Response latestBewertungsbogen() {
        Map<String, Object> first = bewertungsbogenDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No bewertungsbogen received yet")).build();
        }
        return Response.ok(first).build();
    }

    // ========== PRODUKTEMPFEHLUNG ENDPOINTS (Pallmann Florian) ==========

    @POST
    @Path("/produktempfehlung")
    public Response receiveProduktempfehlung(ProduktempfehlungDeliveryRequest body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("processInstanceId", body == null ? null : body.processInstanceId);
        item.put("kundeEmail", body == null ? null : body.kundeEmail);
        item.put("offer", body == null ? null : body.offer);
        item.put("sentAt", body == null ? null : body.sentAt);

        produktempfehlungDeliveries.addFirst(item);
        while (produktempfehlungDeliveries.size() > MAX_ITEMS) {
            produktempfehlungDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", produktempfehlungDeliveries.size())).build();
    }

    @GET
    @Path("/produktempfehlung")
    public List<Map<String, Object>> listProduktempfehlungen(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : produktempfehlungDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    @GET
    @Path("/produktempfehlung/latest")
    public Response latestProduktempfehlung() {
        Map<String, Object> first = produktempfehlungDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No produktempfehlung received yet")).build();
        }
        return Response.ok(first).build();
    }

    // ========== Niklas Putz ==========

    @POST
    @Path("/verhinderungsabfrage")
    public Response receiveVerhinderungsabfrage(VerhinderungsabfrageDeliveryRequest body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("message", body == null ? null : body.message);

        verhinderungsabfrageDeliveries.addFirst(item);
        while (verhinderungsabfrageDeliveries.size() > MAX_ITEMS) {
            verhinderungsabfrageDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", verhinderungsabfrageDeliveries.size())).build();
    }

    @GET
    @Path("/verhinderungsabfrage")
    public List<Map<String, Object>> listVerhinderungsabfragen(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : verhinderungsabfrageDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    @GET
    @Path("/verhinderungsabfrage/latest")
    public Response latestVerhinderungsabfrage() {
        Map<String, Object> first = verhinderungsabfrageDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No verhinderungsabfrage received yet")).build();
        }
        return Response.ok(first).build();
    }

    // ========== Niklas Putz ==========

    @POST
    @Path("/KIWetterabfrage")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response receiveKIWetterabfrage(String body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("content", body);

        kiWetterabfrageDeliveries.addFirst(item);
        while (kiWetterabfrageDeliveries.size() > MAX_ITEMS) {
            kiWetterabfrageDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", kiWetterabfrageDeliveries.size())).build();
    }

    @GET
    @Path("/KIWetterabfrage")
    public List<Map<String, Object>> listKIWetterabfragen(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : kiWetterabfrageDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    @GET
    @Path("/KIWetterabfrage/latest")
    public Response latestKIWetterabfrage() {
        Map<String, Object> first = kiWetterabfrageDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No KI Wetterabfrage received yet")).build();
        }
        return Response.ok(first).build();
    }

    // ========== Niklas Putz ==========

    @POST
    @Path("/motivationsergebnis")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response receiveMotivationsergebnis(String body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("receivedAt", Instant.now().toString());
        item.put("content", body);

        motivationsergebnisDeliveries.addFirst(item);
        while (motivationsergebnisDeliveries.size() > MAX_ITEMS) {
            motivationsergebnisDeliveries.pollLast();
        }

        return Response.ok(Map.of("ok", true, "stored", motivationsergebnisDeliveries.size())).build();
    }

    @GET
    @Path("/motivationsergebnis")
    public List<Map<String, Object>> listMotivationsergebnisse(@QueryParam("limit") @DefaultValue("20") int limit) {
        int n = Math.max(1, Math.min(200, limit));
        List<Map<String, Object>> out = new ArrayList<>(n);
        int i = 0;
        for (Map<String, Object> item : motivationsergebnisDeliveries) {
            out.add(item);
            i++;
            if (i >= n) {
                break;
            }
        }
        return out;
    }

    @GET
    @Path("/motivationsergebnis/latest")
    public Response latestMotivationsergebnis() {
        Map<String, Object> first = motivationsergebnisDeliveries.peekFirst();
        if (first == null) {
            return Response.status(404).entity(Map.of("ok", false, "message", "No Motivationsergebnis received yet")).build();
        }
        return Response.ok(first).build();
    }
}