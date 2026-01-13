package main.java.com.roadbike.crawler.model.camunda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Camunda Engine REST API message request
 * Pallmann Florian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CamundaMessageRequest {
    
    private String messageName;
    private Boolean resultEnabled;
    private Map<String, ProcessVariable> processVariables;
}
