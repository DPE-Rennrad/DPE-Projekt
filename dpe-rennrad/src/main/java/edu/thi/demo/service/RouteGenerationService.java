package edu.thi.demo.service;

import edu.thi.demo.model.RoutePoint;
import edu.thi.demo.model.RouteRequest;
import edu.thi.demo.model.RouteResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Klasse geschrieben von Felix Sewald
@ApplicationScoped
public class RouteGenerationService {

    private static final Random random = new Random();

    // Vordefinierte Basis-Route
    private static final double[][] BASE_ROUTE = {
        {48.1351, 11.5820, 520},
        {48.2045, 11.5432, 545},
        {48.2891, 11.5101, 580},
        {48.3567, 11.4923, 520},
        {48.4234, 11.4756, 490},
        {48.5012, 11.4589, 510},
        {48.5789, 11.4412, 550},
        {48.6543, 11.4298, 530},
        {48.7634, 11.4201, 480}
    };

    public RouteResponse generateRoute(RouteRequest request) {
        List<RoutePoint> coordinates = new ArrayList<>();

        for (double[] point : BASE_ROUTE) {
            coordinates.add(new RoutePoint(
                point[0] + (random.nextDouble() - 0.5) * 0.01,
                point[1] + (random.nextDouble() - 0.5) * 0.01,
                point[2] + (random.nextDouble() - 0.5) * 50     // Höhe ±25m
            ));
        }

        double distance = 70 + random.nextDouble() * 15;

        // Höhenmeter abhängig von Schwierigkeit
        long elevationGain = "hard".equalsIgnoreCase(request.difficulty) ? 700 + random.nextInt(300) :
                             "medium".equalsIgnoreCase(request.difficulty) ? 400 + random.nextInt(200) :
                             200 + random.nextInt(150);

        return new RouteResponse(
            coordinates,
            Math.round(distance * 10.0) / 10.0,
            elevationGain,
            request.difficulty,
            request.startLocation,
            request.endLocation
        );
    }
}
