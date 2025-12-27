package edu.thi.demo.service;

import edu.thi.demo.model.ExternalProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("externalProviderLadenDelegate")
public class ExternalProviderLadenDelegate implements JavaDelegate {

    @Inject
    ExternalProviderService externalProviderService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Mock-Provider initialisieren (falls noch nicht vorhanden)
        externalProviderService.initializeMockProviders();
        
        // Aktive Provider aus der Datenbank laden
        List<ExternalProvider> activeProviders = externalProviderService.getActiveProviders();
        
        // Provider-Liste als Prozessvariable setzen (für Multi-Instance Loop)
        execution.setVariable("eventanbieter", activeProviders);
        
        // Optional: Anzahl der Provider setzen
        execution.setVariable("providerCount", activeProviders.size());
        
        // Optional: Provider-Namen für Logging
        List<String> providerNames = activeProviders.stream()
            .map(p -> p.name)
            .collect(Collectors.toList());
        execution.setVariable("providerNames", providerNames);
        
        System.out.println("Externe Anbieter geladen: " + providerNames);
    }
}
