package edu.thi.demo.services;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/kimockantwort")
public class KiMockAntwort {

    private static final Logger LOG = Logger.getLogger(KiMockAntwort.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getKiAntwort() {
        String ergebnis = "Frische Luft pur! Bei knackigen -2°C und trockenen Straßen kannst du jetzt eine herrlich belebende Radtour erleben. " +
                "Die klare Winterluft gibt dir den ultimativen Energiekick – ideal, um hellwach und voller Elan durchzustarten. Gute Fahrt!";

        // Loggt das Ergebnis in der Konsole
        LOG.info("KI Mock Anfrage erhalten. Rückgabewert: " + ergebnis);

        return ergebnis;
    }
}