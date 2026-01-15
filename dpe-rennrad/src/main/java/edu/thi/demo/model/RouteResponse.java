package edu.thi.demo.model;

import java.util.List;

//Klasse geschrieben von Felix Sewald
public class RouteResponse {

    public List<RoutePoint> coordinates;
    public double totalDistance;
    public double elevationGain;
    public String difficulty;
    public String startLocation;
    public String endLocation;

    public RouteResponse() {
    }

    public RouteResponse(List<RoutePoint> coordinates, double totalDistance, double elevationGain,
                         String difficulty, String startLocation, String endLocation) {
        this.coordinates = coordinates;
        this.totalDistance = totalDistance;
        this.elevationGain = elevationGain;
        this.difficulty = difficulty;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }
}
