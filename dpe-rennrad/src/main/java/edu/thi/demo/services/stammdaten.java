package edu.thi.demo.services;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/stammdaten")
public class stammdaten {

    private static final Logger LOG = Logger.getLogger(stammdaten.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBeispiel() {
        String ergebnis = "Beispiel-Stammdaten: Max Mustermann, Kundennummer 12345";

        // Loggt das Ergebnis in der Konsole
        LOG.info("Anfrage erhalten. Rückgabewert: " + ergebnis);

        return ergebnis;
    }
}