package edu.thi.demo.repository;

import edu.thi.demo.model.Kunde;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KundeRepository implements PanacheRepository<Kunde> {

}