package edu.thi.demo.model;

//Klasse geschrieben von Felix Sewald
public class RoutePoint {

    public double lat;
    public double lon;
    public double elevation;

    public RoutePoint() {
    }

    public RoutePoint(double lat, double lon, double elevation) {
        this.lat = lat;
        this.lon = lon;
        this.elevation = elevation;
    }
}
