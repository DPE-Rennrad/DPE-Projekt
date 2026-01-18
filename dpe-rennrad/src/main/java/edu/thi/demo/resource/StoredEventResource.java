//Niklas Putz

package edu.thi.demo.resource;

import edu.thi.demo.model.StoredEvent;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Path("/api/events")
public class StoredEventResource {

    private static final Logger LOG = Logger.getLogger(StoredEventResource.class);

    @DELETE
    @Path("/expired")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Map<String, Object> deleteExpiredEvents() {
        // Nur für Testzwecke da Events basierend auf dem aktuellen Tag erstellt werden:
        // aktuelles Datum + 2 Tage
        Instant cutoffDate = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

        List<StoredEvent> expiredEvents = StoredEvent.list("eventDate < ?1", cutoffDate);

        for (StoredEvent event : expiredEvents) {
            LOG.infof("Lösche Event: ID=%d, Titel='%s'", event.id, event.title);
            event.delete();
        }

        return Map.of("deletedCount", expiredEvents.size());
    }
}