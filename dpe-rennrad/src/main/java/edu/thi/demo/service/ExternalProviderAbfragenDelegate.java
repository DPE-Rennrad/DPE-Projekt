package edu.thi.demo.service;

import edu.thi.demo.model.ExternalProvider;
import edu.thi.demo.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.RuntimeService;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Named("externalProviderAbfragenDelegate")
public class ExternalProviderAbfragenDelegate implements JavaDelegate {

    private static final Random random = new Random();
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Provider aus der Prozessvariable holen
        ExternalProvider provider = (ExternalProvider) execution.getVariable("eventanbieterEintrag");
        
        System.out.println("Sende Anfrage an Provider: " + provider.name + " (" + provider.apiEndpoint + ")");
        
        // Mock-Anfrage simulieren (kleine Verzögerung)
        Thread.sleep(100 + random.nextInt(200));
        
        // Mock-Events für diesen Provider generieren
        List<Event> mockEvents = generateMockEvents(provider);
        
        // Events und enoughAnswers in der Message senden
        RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("receivedEvents", mockEvents);
        messagePayload.put("enoughAnswers", true);  // Da nur ein Provider, genug Antworten
        
        runtimeService.correlateMessage("event", execution.getProcessInstanceId(), messagePayload);
        
        System.out.println("Message 'event' gesendet mit " + mockEvents.size() + " Events.");
    }
    
    private List<Event> generateMockEvents(ExternalProvider provider) {
        List<Event> events = new ArrayList<>();
        int eventCount = 2 + random.nextInt(4); // 2-5 Events pro Provider
        
        for (int i = 0; i < eventCount; i++) {
            Event event = new Event();
            event.name = provider.name + " Event " + (i + 1);
            event.description = "Mock Event Description von " + provider.name;
            event.location = getRandomLocation();
            event.eventDate = LocalDateTime.now().plusDays(random.nextInt(30) + 1);
            event.category = getRandomCategory();
            event.providerName = provider.name;
            event.externalId = provider.id + "-" + System.currentTimeMillis() + "-" + i;
            
            events.add(event);
        }
        
        return events;
    }
    
    private String getRandomLocation() {
        String[] locations = {"München", "Berlin", "Hamburg", "Köln", "Frankfurt", "Stuttgart"};
        return locations[random.nextInt(locations.length)];
    }
    
    private String getRandomCategory() {
        String[] categories = {"Radsport", "Marathon", "Triathlon", "Radtour", "Rennen", "Training"};
        return categories[random.nextInt(categories.length)];
    }
}
