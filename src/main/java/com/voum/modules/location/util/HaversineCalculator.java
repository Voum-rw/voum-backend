package com.voum.modules.location.util;

public class HaversineCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the distance between two latitude/longitude points in kilometers
     * using the Haversine formula.
     */
    public static double calculateDistance(double startLat, double startLng, double endLat, double endLng) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLng = Math.toRadians(endLng - startLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return EARTH_RADIUS_KM * c;
    }
}
