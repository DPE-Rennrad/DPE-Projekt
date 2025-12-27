package edu.thi.demo.service;

import edu.thi.demo.model.ExternalProvider;
import edu.thi.demo.repository.ExternalProviderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ExternalProviderService {

    @Inject
    ExternalProviderRepository externalProviderRepository;

    public List<ExternalProvider> getAllProviders() {
        return externalProviderRepository.listAll();
    }

    public List<ExternalProvider> getActiveProviders() {
        return externalProviderRepository.findActiveProviders();
    }

    public ExternalProvider getProviderById(Long id) {
        return externalProviderRepository.findById(id);
    }

    @Transactional
    public ExternalProvider createProvider(ExternalProvider provider) {
        externalProviderRepository.persist(provider);
        return provider;
    }

    @Transactional
    public void initializeMockProviders() {
        // Prüfen, ob bereits Provider existieren
        if (externalProviderRepository.count() == 0) {
            // Mock-Daten erstellen
            ExternalProvider provider1 = new ExternalProvider(
                "EventBrite API", 
                "https://api.eventbrite.com/v3/events", 
                "mock-api-key-123", 
                true
            );
            ExternalProvider provider2 = new ExternalProvider(
                "Meetup API", 
                "https://api.meetup.com/events", 
                "mock-api-key-456", 
                true
            );
            ExternalProvider provider3 = new ExternalProvider(
                "Local Events API", 
                "https://api.localevents.com/search", 
                "mock-api-key-789", 
                true
            );
            
            externalProviderRepository.persist(provider1);
            externalProviderRepository.persist(provider2);
            externalProviderRepository.persist(provider3);
        }
    }
}
