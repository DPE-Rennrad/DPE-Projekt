package main.java.com.roadbike.crawler.model.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Camunda process variable
 * Pallmann Florian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessVariable {
    
    private String value;
    private String type;
}
