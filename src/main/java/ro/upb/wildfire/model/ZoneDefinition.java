package ro.upb.wildfire.model;

public record ZoneDefinition(
        String zoneName,
        String country,
        double minLatitude,
        double maxLatitude,
        double minLongitude,
        double maxLongitude
) {

    public boolean contains(double latitude, double longitude) {
        return latitude >= minLatitude
                && latitude <= maxLatitude
                && longitude >= minLongitude
                && longitude <= maxLongitude;
    }
}

