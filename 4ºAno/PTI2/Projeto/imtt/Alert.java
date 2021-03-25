package imtt;

public class Alert {
    private static final double R_EARTH = 6371e3;
    private long tstamp, ttl;
    private double latitude, longitude;
    private int radius;
    private String message;

    public Alert(long tstamp, long ttl, double latitude, double longitude, int radius, String message) {
        this.tstamp = tstamp;
        this.ttl = ttl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.message = message;
    }

    public long getTstamp() {
        return this.tstamp;
    }

    public long getTtl() {
        return this.ttl;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public int getRadius() {
        return this.radius;
    }

    public String getMessage() {
        return this.message;
    }

    public int getDistance(double latitude, double longitude) {
        double latA = this.latitude * Math.PI / 180;
        double lngA = this.longitude * Math.PI / 180;
        double latB = latitude * Math.PI / 180;
        double lngB = longitude * Math.PI / 180;

        return (int) (Math.acos(Math.sin(latA) * Math.sin(latB)
                + Math.cos(latA) * Math.cos(latB) * Math.cos(lngB - lngA)) * R_EARTH);
    }
}
