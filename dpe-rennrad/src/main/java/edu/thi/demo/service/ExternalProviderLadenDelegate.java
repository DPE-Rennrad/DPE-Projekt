// Eduard Merker
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
        
        // Nur den ersten aktiven Provider laden (für vereinfachten Prozess)
        if (!activeProviders.isEmpty()) {
            ExternalProvider singleProvider = activeProviders.get(0);
            execution.setVariable("eventanbieterEintrag", singleProvider);
            execution.setVariable("providerCount", 1);
            System.out.println("Ein externer Anbieter geladen: " + singleProvider.name);
        } else {
            throw new RuntimeException("Keine aktiven Provider verfügbar");
        }
    }
}

