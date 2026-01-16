package edu.thi.demo.resource;

import edu.thi.demo.model.RouteRequest;
import edu.thi.demo.model.RouteResponse;
import edu.thi.demo.service.RouteGenerationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

//Klasse geschrieben von Felix Sewald
@Path("/route")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteResource {

    @Inject
    RouteGenerationService routeGenerationService;

    @GET
    @Path("/generate")
    public RouteResponse generateRoute(
            @QueryParam("startLocation") String startLocation,
            @QueryParam("endLocation") String endLocation,
            @QueryParam("difficulty") @DefaultValue("medium") String difficulty) {

        if (startLocation == null || startLocation.trim().isEmpty()) {
            throw new WebApplicationException("startLocation ist erforderlich", 400);
        }

        if (endLocation == null || endLocation.trim().isEmpty()) {
            throw new WebApplicationException("endLocation ist erforderlich", 400);
        }

        RouteRequest request = new RouteRequest(startLocation, endLocation, difficulty);
        return routeGenerationService.generateRoute(request);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "Route Generation Service is running";
    }
}
