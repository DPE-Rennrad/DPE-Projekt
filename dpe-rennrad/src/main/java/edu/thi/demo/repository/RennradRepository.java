package edu.thi.demo.repository;

import edu.thi.demo.model.Rennrad;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class RennradRepository implements PanacheRepository<Rennrad> {

    public List<Rennrad> findByTyp(String typ) {
        return list("#Rennrad.findByTyp", typ);
    }
}