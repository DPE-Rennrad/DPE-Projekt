package edu.thi.demo.model;

//Klasse geschrieben von Felix Sewald
public class RouteRequest {

    public String startLocation;
    public String endLocation;
    public String difficulty;

    public RouteRequest() {
    }

    public RouteRequest(String startLocation, String endLocation, String difficulty) {
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.difficulty = difficulty;
    }
}
