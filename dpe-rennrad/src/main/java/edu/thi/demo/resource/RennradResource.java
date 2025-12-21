package edu.thi.demo.resource;

import edu.thi.demo.model.Rennrad;
import edu.thi.demo.service.RennradService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/rennrad")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RennradResource {

    @Inject
    RennradService rennradService;

    @GET
    public List<Rennrad> getAll() {
        return rennradService.getAllRennraeder();
    }

    @GET
    @Path("/{id}")
    public Response getRennradById(@PathParam("id") Long id) {
        Rennrad rennrad = rennradService.getRennradById(id);
        if (rennrad != null) {
            return Response.ok(rennrad).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    public Rennrad create(Rennrad rennrad) {
        return rennradService.createRennrad(rennrad);
    }

    @PUT
    @Path("/{id}")
    public Response updateRennrad(@PathParam("id") Long id, Rennrad rennrad) {
        Rennrad updated = rennradService.updateRennrad(id, rennrad);
        if (updated != null) {
            return Response.ok(updated).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRennrad(@PathParam("id") Long id) {
        boolean deleted = rennradService.deleteRennrad(id);
        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}