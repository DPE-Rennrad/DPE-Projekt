// Eduard Merker

package edu.thi.demo.model;

import java.time.LocalDateTime;

public class Event {
    
    public String name;
    public String description;
    public String location;
    public LocalDateTime eventDate;
    public String category;
    public String providerName;
    public String externalId;
    
    public Event() {
    }
    
    public Event(String name, String description, String location, LocalDateTime eventDate, 
                 String category, String providerName, String externalId) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.category = category;
        this.providerName = providerName;
        this.externalId = externalId;
    }
}

