package edu.thi.demo.repository;

import edu.thi.demo.model.ExternalProvider;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ExternalProviderRepository implements PanacheRepository<ExternalProvider> {

    public List<ExternalProvider> findActiveProviders() {
        return list("active", true);
    }
}
