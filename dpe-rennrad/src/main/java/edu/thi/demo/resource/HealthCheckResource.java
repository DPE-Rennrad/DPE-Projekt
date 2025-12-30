package edu.thi.demo.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import jakarta.enterprise.inject.Instance;
import java.util.HashMap;
import java.util.Map;

@Path("/health")
public class HealthCheckResource {

    @Inject
    Instance<JavaDelegate> allDelegates;

    @GET
    @Path("/delegates")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> checkDelegates() {
        Map<String, String> status = new HashMap<>();
        
        int count = 0;
        for (JavaDelegate delegate : allDelegates) {
            status.put("delegate_" + count, delegate.getClass().getSimpleName());
            count++;
        }
        
        status.put("total_delegates", String.valueOf(count));
        return status;
    }
}
