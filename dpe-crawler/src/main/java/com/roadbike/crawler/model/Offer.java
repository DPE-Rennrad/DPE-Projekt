package main.java.com.roadbike.crawler.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a road bike offer
 * Pallmann Florian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    
    private String id;
    private String title;
    private String description;
    private Double price;
    private Double originalPrice;
    private Integer discountPercent;
    private String brand;
    private String category;
    private String url;
    private String imageUrl;
    
    private String source;
    private Boolean crawled;
}
