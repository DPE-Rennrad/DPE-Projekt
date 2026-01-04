package edu.thi.demo.service;

import edu.thi.demo.model.RoutePoint;
import edu.thi.demo.model.RouteRequest;
import edu.thi.demo.model.RouteResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@ApplicationScoped
public class RouteGenerationService {

    private static final Random random = new Random();
    private static final Map<String, Coordinates> CITY_COORDINATES = new HashMap<>();

    static {
        CITY_COORDINATES.put("München", new Coordinates(48.1351, 11.5820));
        CITY_COORDINATES.put("Berlin", new Coordinates(52.5200, 13.4050));
        CITY_COORDINATES.put("Hamburg", new Coordinates(53.5511, 9.9937));
        CITY_COORDINATES.put("Köln", new Coordinates(50.9375, 6.9603));
        CITY_COORDINATES.put("Frankfurt", new Coordinates(50.1109, 8.6821));
        CITY_COORDINATES.put("Stuttgart", new Coordinates(48.7758, 9.1829));
        CITY_COORDINATES.put("Nürnberg", new Coordinates(49.4521, 11.0767));
        CITY_COORDINATES.put("Dresden", new Coordinates(51.0504, 13.7373));
        CITY_COORDINATES.put("Leipzig", new Coordinates(51.3397, 12.3731));
        CITY_COORDINATES.put("Ingolstadt", new Coordinates(48.7634, 11.4201));
    }

    public RouteResponse generateRoute(RouteRequest request) {
        Coordinates start = getCoordinatesForLocation(request.startLocation);
        Coordinates end = getCoordinatesForLocation(request.endLocation);

        int waypoints = 15 + random.nextInt(20);
        double totalDistance = calculateDistance(start, end);
        double elevationGain = calculateElevationGain(request.difficulty, totalDistance);

        List<RoutePoint> coordinates = new ArrayList<>();
        double currentElevation = 300 + random.nextDouble() * 100;

        for (int i = 0; i <= waypoints; i++) {
            double progress = (double) i / waypoints;
            double lat = start.lat + (end.lat - start.lat) * progress;
            double lon = start.lon + (end.lon - start.lon) * progress;

            lat += (random.nextDouble() - 0.5) * 0.02;
            lon += (random.nextDouble() - 0.5) * 0.02;

            currentElevation += (random.nextDouble() - 0.5) * 20;
            if ("hard".equalsIgnoreCase(request.difficulty)) {
                currentElevation += (random.nextDouble() - 0.3) * 30;
            } else if ("medium".equalsIgnoreCase(request.difficulty)) {
                currentElevation += (random.nextDouble() - 0.4) * 15;
            }

            currentElevation = Math.max(200, Math.min(1200, currentElevation));

            coordinates.add(new RoutePoint(
                Math.round(lat * 1000000.0) / 1000000.0,
                Math.round(lon * 1000000.0) / 1000000.0,
                Math.round(currentElevation * 10.0) / 10.0
            ));
        }

        return new RouteResponse(
            coordinates,
            Math.round(totalDistance * 10.0) / 10.0,
            Math.round(elevationGain),
            request.difficulty,
            request.startLocation,
            request.endLocation
        );
    }

    private Coordinates getCoordinatesForLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return CITY_COORDINATES.get("München");
        }

        Coordinates coords = CITY_COORDINATES.get(location);
        if (coords != null) {
            return coords;
        }

        for (Map.Entry<String, Coordinates> entry : CITY_COORDINATES.entrySet()) {
            if (entry.getKey().toLowerCase().contains(location.toLowerCase()) ||
                location.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return CITY_COORDINATES.get("München");
    }

    private double calculateDistance(Coordinates start, Coordinates end) {
        double latDiff = Math.abs(end.lat - start.lat);
        double lonDiff = Math.abs(end.lon - start.lon);
        double straightLine = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111.0;
        return straightLine * (1.3 + random.nextDouble() * 0.3);
    }

    private double calculateElevationGain(String difficulty, double distance) {
        double baseElevation = distance * 8;
        if ("hard".equalsIgnoreCase(difficulty)) {
            return baseElevation * (1.5 + random.nextDouble() * 0.5);
        } else if ("medium".equalsIgnoreCase(difficulty)) {
            return baseElevation * (1.0 + random.nextDouble() * 0.3);
        } else {
            return baseElevation * (0.5 + random.nextDouble() * 0.2);
        }
    }

    private static class Coordinates {
        double lat;
        double lon;

        Coordinates(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
