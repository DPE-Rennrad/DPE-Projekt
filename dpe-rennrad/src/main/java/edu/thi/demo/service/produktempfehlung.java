package edu.thi.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.thi.demo.model.Offer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/*
Florian Pallmann
 */

@Path("/produktempfehlung")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class produktempfehlung {
    private static final Logger LOG = Logger.getLogger(produktempfehlung.class);

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/ki-abgleich")
    public Response kiAbgleich(Offer offer) {
        LOG.infof("KI-Abgleich für Offer: title=%s, price=%.2f, discount=%d%%",
                offer.title, offer.price, offer.discountPercent);

        try {
            // Mock KI-Score
            double kiScore = calculateKiScore(offer);

            Map<String, Object> response = new HashMap<>();
            response.put("kiScore", kiScore);
            response.put("offer", offer);
            response.put("success", true);

            LOG.infof("KI-Abgleich abgeschlossen. Score=%.2f", kiScore);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Fehler beim KI-Abgleich");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "success", false,
                            "error", "Fehler beim KI-Abgleich: " + e.getMessage()
                    ))
                    .build();
        }
    }

    @POST
    @Path("/link-transformation")
    public Response linkTransformation(Offer offer) {
        LOG.infof("Link-Transformation für: %s", offer.title);

        try {
            // Affiliate-Link erstellen
            String affiliateUrl = offer.url + ".affiliate.com";
            offer.url = affiliateUrl;

            Map<String, Object> response = new HashMap<>();
            response.put("offer", offer);
            response.put("affiliateUrl", affiliateUrl);
            response.put("success", true);

            LOG.infof("Link-Transformation abgeschlossen. Affiliate-Link: %s", affiliateUrl);

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.errorf(e, "Fehler bei Link-Transformation");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "success", false,
                            "error", "Fehler bei Link-Transformation: " + e.getMessage()
                    ))
                    .build();
        }
    }


    // Helper Methods

    private double calculateKiScore(Offer offer) {
        if (offer.crawled) {
            if (offer.discountPercent == null) return 0.0;
            return Math.min(offer.discountPercent / 100.0, 1.0);
        } else {
            return 1.0;
        }
    }
}