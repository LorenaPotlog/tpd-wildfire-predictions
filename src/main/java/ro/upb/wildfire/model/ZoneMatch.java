package ro.upb.wildfire.model;

public record ZoneMatch(
        String zoneName,
        String country
) {

    public static ZoneMatch unknown() {
        return new ZoneMatch("Unknown", "Unknown");
    }
}

