package edu.thi.demo.service;

import edu.thi.demo.model.ExternalProvider;
import edu.thi.demo.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ApplicationScoped
@Named("externalProviderAbfragenDelegate")
public class ExternalProviderAbfragenDelegate implements JavaDelegate {

    private static final Random random = new Random();
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Aktuellen Provider aus dem Multi-Instance Loop holen
        ExternalProvider provider = (ExternalProvider) execution.getVariable("eventanbieter");
        
        System.out.println("Sende Anfrage an Provider: " + provider.name + " (" + provider.apiEndpoint + ")");
        
        // Mock-Anfrage simulieren (kleine Verzögerung)
        Thread.sleep(100 + random.nextInt(200));
        
        // Mock-Events für diesen Provider generieren
        List<Event> mockEvents = generateMockEvents(provider);
        
        // Events als Variable speichern (mit Provider-Name als Suffix)
        String variableName = "events_" + provider.id;
        execution.setVariable(variableName, mockEvents);
        
        System.out.println("Anfrage an " + provider.name + " gesendet. Erwarte " + mockEvents.size() + " Events.");
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
