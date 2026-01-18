//Niklas Putz

package edu.thi.demo.resource;

import edu.thi.demo.model.Kunde;
import edu.thi.demo.service.KundeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stammdaten/kunde")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KundeResource {

    @Inject
    KundeService kundeService;

    @PUT
    @Path("/{id}")
    public Response updateKunde(@PathParam("id") Long id, Kunde kunde) {
        Kunde updated = kundeService.updateKunde(id, kunde);
        if (updated != null) {
            return Response.ok(updated).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Kunde nicht gefunden\"}")
                    .build();
        }
    }
}