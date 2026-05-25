package ro.unibuc.bpd.wildfire.geo;

import java.util.Locale;

public final class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static String toCellId(double latitude, double longitude) {
        double lat = round(latitude, 1);
        double lon = round(longitude, 1);
        return String.format(Locale.US, "%.2f:%.2f", lat, lon);
    }

    public static double bearing(double startLat, double startLon, double endLat, double endLon) {
        double lat1 = Math.toRadians(startLat);
        double lat2 = Math.toRadians(endLat);
        double deltaLon = Math.toRadians(endLon - startLon);

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);

        return normalizeAngle(Math.toDegrees(Math.atan2(y, x)));
    }

    public static double distanceKm(double startLat, double startLon, double endLat, double endLon) {
        double deltaLat = Math.toRadians(endLat - startLat);
        double deltaLon = Math.toRadians(endLon - startLon);
        double lat1 = Math.toRadians(startLat);
        double lat2 = Math.toRadians(endLat);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static double weightedAngle(double firstDegrees, double secondDegrees, double firstWeight, double secondWeight) {
        double x = firstWeight * Math.cos(Math.toRadians(firstDegrees))
                + secondWeight * Math.cos(Math.toRadians(secondDegrees));
        double y = firstWeight * Math.sin(Math.toRadians(firstDegrees))
                + secondWeight * Math.sin(Math.toRadians(secondDegrees));
        return normalizeAngle(Math.toDegrees(Math.atan2(y, x)));
    }

    public static double normalizeAngle(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}
