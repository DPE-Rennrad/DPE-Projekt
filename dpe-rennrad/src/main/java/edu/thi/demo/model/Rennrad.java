package edu.thi.demo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.persistence.NamedQuery;

@Entity
@Table(name = "rennrad")
@XmlRootElement
@NamedQuery(name = "Rennrad.findByTyp", query = "SELECT r FROM Rennrad r WHERE r.typ LIKE ?1")
public class Rennrad extends PanacheEntity {

    public String name;
    public String typ;
    public double preis;

    // Getter und Setter können weggelassen werden, da Panache sie automatisch generiert
}

// Eduard Merker
// für theoretische Erweiterungen 