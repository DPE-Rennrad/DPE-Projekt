package main.java.com.roadbike.crawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.com.roadbike.crawler.model.Offer;
import main.java.com.roadbike.crawler.model.camunda.CamundaMessageRequest;
import main.java.com.roadbike.crawler.model.camunda.ProcessVariable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock crawler service that generates random road bike offers
 * Pallmann Florian
 */
@Slf4j
@Service
public class RoadBikeCrawlerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    @Value("${crawler.target.url}")
    private String targetUrl;
    
    @Value("${crawler.message.name:Crawlermessage}")
    private String messageName;

    private static final String[] BRANDS = {
        "Canyon", "Specialized", "Trek", "Giant", "Bianchi", 
        "Cervélo", "Pinarello", "Scott", "BMC", "Cube"
    };

    private static final String[] CATEGORIES = {
        "Road Bike", "Aero Road Bike", "Endurance Road Bike", 
        "Gravel Bike", "Time Trial Bike", "Triathlon Bike"
    };

    private static final String[] ADJECTIVES = {
        "Premium", "Elite", "Pro", "Racing", "Carbon", 
        "Ultimate", "Advanced", "Aero", "Lightweight", "Performance"
    };

    private static final String[] SOURCES = {
        "bike-discount.de", "radonline.de", "bike24.de", 
        "rose-bikes.de", "fahrrad.de"
    };

    public RoadBikeCrawlerService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the mock crawl at random intervals between 10-30 seconds
     */
    @Scheduled(initialDelayString = "${crawler.initial.delay:5000}", 
               fixedDelayString = "#{T(java.util.concurrent.ThreadLocalRandom).current().nextInt(10000, 30000)}")
    public void crawlOffers() {
        try {
            Offer offer = generateRandomOffer();
            log.info("New offer found: {} - {} EUR ({}% discount)", 
                     offer.getTitle(), offer.getPrice(), offer.getDiscountPercent());
            
            sendOffer(offer);
            
            // Next crawl in 10-30 seconds
            int nextDelay = ThreadLocalRandom.current().nextInt(10, 31);
            log.info("Next crawl in {} seconds", nextDelay);
            
        } catch (Exception e) {
            log.error("Error during crawling: ", e);
        }
    }

    /**
     * Generates a random mock offer
     */
    private Offer generateRandomOffer() {
        String brand = BRANDS[random.nextInt(BRANDS.length)];
        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
        String source = SOURCES[random.nextInt(SOURCES.length)];
        
        double originalPrice = 1500 + (random.nextDouble() * 7500); // 1500-9000 EUR
        int discount = 10 + random.nextInt(51); // 10-60% discount
        double currentPrice = originalPrice * (1 - discount / 100.0);
        
        String model = adjective + " " + (1000 + random.nextInt(9000));
        
        return Offer.builder()
                .id(UUID.randomUUID().toString())
                .title(brand + " " + category + " " + model)
                .description(String.format("Top %s with carbon frame and Shimano/SRAM groupset", category))
                .price(Math.round(currentPrice * 100.0) / 100.0)
                .originalPrice(Math.round(originalPrice * 100.0) / 100.0)
                .discountPercent(discount)
                .brand(brand)
                .category(category)
                .url("https://" + source + "/product/" + UUID.randomUUID().toString())
                .imageUrl("https://" + source + "/images/" + UUID.randomUUID().toString() + ".jpg")
                .source(source)
                .crawled(true)
                .build();
    }

    /**
     * Sends the offer via REST POST to Camunda Engine
     */
    private void sendOffer(Offer offer) {
        try {
            // Convert offer to JSON string
            String offerJson = objectMapper.writeValueAsString(offer);
            
            // Create Camunda process variable
            ProcessVariable offerVariable = ProcessVariable.builder()
                    .value(offerJson)
                    .type("String")
                    .build();
            
            // Create process variables map
            Map<String, ProcessVariable> processVariables = new HashMap<>();
            processVariables.put("offer", offerVariable);
            
            // Create Camunda message request
            CamundaMessageRequest camundaRequest = CamundaMessageRequest.builder()
                    .messageName(messageName)
                    .resultEnabled(true)
                    .processVariables(processVariables)
                    .build();
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP request
            HttpEntity<CamundaMessageRequest> request = new HttpEntity<>(camundaRequest, headers);
            
            // Send to Camunda
            ResponseEntity<String> response = restTemplate.postForEntity(
                targetUrl, 
                request, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Offer sent successfully to Camunda! Status: {}", response.getStatusCode());
                log.debug("Response body: {}", response.getBody());
            } else {
                log.warn("Offer sent with status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error sending offer to Camunda: ", e);
        }
    }
}
