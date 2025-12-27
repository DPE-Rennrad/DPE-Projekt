package edu.thi.demo.service;

import edu.thi.demo.model.ExternalProvider;
import edu.thi.demo.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ApplicationScoped
@Named("antwortenEmpfangenDelegate")
public class AntwortenEmpfangenDelegate implements JavaDelegate {

    private static final Random random = new Random();
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Aktuellen Provider aus dem Multi-Instance Loop holen
        ExternalProvider provider = (ExternalProvider) execution.getVariable("eventanbieter");
        
        System.out.println("Warte auf Antwort von Provider: " + provider.name);
        
        // Mock-Antwortzeit simulieren (realistische Verzögerung)
        Thread.sleep(200 + random.nextInt(500));
        
        // Gespeicherte Events für diesen Provider abrufen
        String variableName = "events_" + provider.id;
        @SuppressWarnings("unchecked")
        List<Event> receivedEvents = (List<Event>) execution.getVariable(variableName);
        
        // Simuliere, dass manchmal keine Antwort kommt (20% Chance)
        if (receivedEvents == null || random.nextInt(100) < 20) {
            System.out.println("Keine Antwort von " + provider.name + " erhalten (Timeout oder Fehler)");
            receivedEvents = new ArrayList<>();
        } else {
            System.out.println("Antwort von " + provider.name + " empfangen: " + receivedEvents.size() + " Events");
        }
        
        // Gesammelte Events in eine gemeinsame Liste zusammenführen
        @SuppressWarnings("unchecked")
        List<Event> allEvents = (List<Event>) execution.getVariable("allReceivedEvents");
        if (allEvents == null) {
            allEvents = new ArrayList<>();
        }
        allEvents.addAll(receivedEvents);
        execution.setVariable("allReceivedEvents", allEvents);
        
        // Statistiken aktualisieren
        Integer totalResponses = (Integer) execution.getVariable("totalResponses");
        if (totalResponses == null) {
            totalResponses = 0;
        }
        execution.setVariable("totalResponses", totalResponses + 1);
        
        // Erfolgsrate berechnen
        Integer providerCount = (Integer) execution.getVariable("providerCount");
        if (providerCount != null && providerCount > 0) {
            double responseRate = (totalResponses.doubleValue() / providerCount) * 100;
            execution.setVariable("responseRate", responseRate);
            execution.setVariable("enoughAnswers", responseRate >= 20.0); // min 20% Antworten
        }
    }
}
