package edu.thi.demo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "kunde")
@XmlRootElement
public class Kunde extends PanacheEntity {

    public String name;
    public String email;

    // Getter und Setter können weggelassen werden, da Panache sie automatisch generiert
}