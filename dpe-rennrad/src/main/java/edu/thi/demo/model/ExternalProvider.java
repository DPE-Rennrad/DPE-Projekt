// Eduard Merker

package edu.thi.demo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "external_provider")
@XmlRootElement
public class ExternalProvider extends PanacheEntity {

    public String name;
    public String apiEndpoint;
    public String apiKey;
    public boolean active;

    // Konstruktoren
    public ExternalProvider() {
    }

    public ExternalProvider(String name, String apiEndpoint, String apiKey, boolean active) {
        this.name = name;
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
        this.active = active;
    }
}

