package main.java.com.roadbike.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application - Road Bike Crawler Mock
 * Pallmann Florian
 */
@SpringBootApplication
@EnableScheduling
public class RoadBikeCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoadBikeCrawlerApplication.class, args);
    }
}
