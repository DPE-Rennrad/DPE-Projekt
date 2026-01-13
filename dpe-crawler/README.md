# Bike Crawler - Mock Implementation

A Spring Boot application that generates mock road bike offers and sends them to Camunda Engine via REST API for process automation testing.

## Overview

This mock crawler simulates a web crawler that finds road bike deals and sends them to Camunda BPM processes through message events. The application generates realistic offer data at random intervals between 10-30 seconds.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Camunda Platform 7 running on localhost:8080

## Quick Start

### 1. Build the application

```bash
mvn clean package
```

### 2. Start the crawler

```bash
mvn spring-boot:run
```

The application will start generating offers every 10-30 seconds and send them to Camunda.

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Camunda Engine URL
crawler.target.url=http://localhost:8080/engine-rest/message

# Message name (must match BPMN)
crawler.message.name=Crawlermessage

# Initial delay before first crawl (milliseconds)
crawler.initial.delay=5000
```

## Offer Data Structure

Each generated offer contains:

- **id**: Unique identifier
- **title**: Product name
- **description**: Product description
- **price**: Current price in EUR
- **originalPrice**: Original price before discount
- **discountPercent**: Discount percentage
- **brand**: Bike brand (Canyon, Specialized, Trek, etc.)
- **category**: Product category (Road Bike, Aero Road Bike, etc.)
- **url**: Product URL
- **imageUrl**: Product image URL
- **source**: Source website
- **crawled**: Is the offer from a crawl (always true)

## Message Format

The crawler sends messages to Camunda in this format:

```json
{
  "messageName": "Crawlermessage",
  "resultEnabled": true,
  "processVariables": {
    "offer": {
      "value": "{...offer JSON...}",
      "type": "String"
    }
  }
}
```