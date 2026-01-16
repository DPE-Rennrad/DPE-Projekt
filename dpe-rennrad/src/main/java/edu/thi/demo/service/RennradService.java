package edu.thi.demo.service;

import edu.thi.demo.model.Rennrad;
import edu.thi.demo.repository.RennradRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class RennradService {

    @Inject
    RennradRepository rennradRepository;

    public List<Rennrad> getAllRennraeder() {
        return rennradRepository.listAll();
    }

    public Rennrad getRennradById(Long id) {
        return rennradRepository.findById(id);
    }

    public List<Rennrad> getRennraederByTyp(String typ) {
        return rennradRepository.findByTyp(typ);
    }

    @Transactional
    public Rennrad createRennrad(Rennrad rennrad) {
        rennradRepository.persist(rennrad);
        return rennrad;
    }

    @Transactional
    public Rennrad updateRennrad(Long id, Rennrad rennrad) {
        Rennrad existing = rennradRepository.findById(id);
        if (existing != null) {
            existing.name = rennrad.name;
            existing.typ = rennrad.typ;
            existing.preis = rennrad.preis;
            rennradRepository.persist(existing);
        }
        return existing;
    }

    @Transactional
    public boolean deleteRennrad(Long id) {
        return rennradRepository.deleteById(id);
    }

    // Weitere Methoden für BPMN-Integration können hier hinzugefügt werden
}

// Eduard Merker
// für theoretische Erweiterungen 