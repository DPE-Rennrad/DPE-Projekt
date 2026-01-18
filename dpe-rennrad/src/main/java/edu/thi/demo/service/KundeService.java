package edu.thi.demo.service;

import edu.thi.demo.model.Kunde;
import edu.thi.demo.repository.KundeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class KundeService {

    @Inject
    KundeRepository kundeRepository;

    @Transactional
    public Kunde createKunde(Kunde kunde) {
        kundeRepository.persist(kunde);
        return kunde;
    }

    @Transactional
    public Kunde getKundeById(Long id) {
        return kundeRepository.findById(id);
    }

    @Transactional
    public Kunde updateKunde(Long id, Kunde kunde) {
        Kunde existing = kundeRepository.findById(id);
        if (existing != null) {
            existing.name = kunde.name;
            existing.email = kunde.email;
            kundeRepository.persist(existing);
        }
        return existing;
    }
}

// Eduard Merker
