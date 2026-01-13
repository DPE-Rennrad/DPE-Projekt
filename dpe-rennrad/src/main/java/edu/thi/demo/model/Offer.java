package edu.thi.demo.model;

import io.quarkus.hibernate.orm.JsonFormat;

import java.time.LocalDateTime;

// Model class representing an Offer
// Pallmann Florian
public class Offer {
    public String id;
    public String title;
    public String description;
    public Double price;
    public Double originalPrice;
    public Integer discountPercent;
    public String brand;
    public String category;
    public String url;
    public String imageUrl;
    public String source;
    public Boolean crawled;

    public Offer() {}

    public Offer(String id, String title, String description, Double price,
                 Double originalPrice, Integer discountPercent, String brand,
                 String category, String url, String imageUrl, String source, Boolean crawled) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent;
        this.brand = brand;
        this.category = category;
        this.url = url;
        this.imageUrl = imageUrl;
        this.source = source;
        this.crawled = crawled;
    }
}