package edu.thi.demo.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stored_event")
public class StoredEvent extends PanacheEntity {

    @Column(name = "provider_key")
    public String providerKey;

    @Column(name = "title")
    public String title;

    @Column(name = "location")
    public String location;

    @Column(name = "event_date")
    public Instant eventDate;

    /**
     * JSON array of first names, e.g. ["Hans","Peter"].
     */
    @Lob
    @Column(name = "passende_leute", columnDefinition = "LONGTEXT")
    public String passendeLeuteJson;
}

// Eduard Merker