package com.voum.modules.location.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Geocoding controller using Nominatim (OpenStreetMap) — 100% free, no API key required.
 *
 * Endpoints:
 *  GET /api/v1/location/places/autocomplete?input=Kigali&lat=-1.9441&lon=30.0619
 *  GET /api/v1/location/places/reverse-geocode?lat=-1.9441&lon=30.0619
 *  GET /api/v1/location/places/details?place_id=<osm_place_id>
 */
@RestController
@RequestMapping("/api/v1/location/places")
@RequiredArgsConstructor
public class NominatimPlacesController {

    private static final Logger log = LoggerFactory.getLogger(NominatimPlacesController.class);
    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "VoumApp/1.0 (voum.rw contact@voum.rw)";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Autocomplete/search for a place by text query.
     * Returns a list of predictions in a Google-Maps-compatible shape so the Flutter
     * PlacesService parses it without changes on the client side.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<Map<String, Object>> autocomplete(
            @RequestParam("input") String input,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            @RequestParam(value = "location", required = false) String location) {

        if (input == null || input.trim().length() < 2) {
            return ResponseEntity.ok(Map.of("predictions", List.of()));
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE + "/search")
                .queryParam("q", input.trim())
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 7)
                .queryParam("countrycodes", "rw")
                .queryParam("accept-language", "en");

        // Bias results towards user's current position if provided
        if (lat != null && lon != null) {
            builder.queryParam("viewbox",
                    (lon - 0.3) + "," + (lat + 0.2) + "," + (lon + 0.3) + "," + (lat - 0.2))
                    .queryParam("bounded", 0);
        } else if (location != null && !location.isEmpty()) {
            String[] parts = location.split(",");
            if (parts.length == 2) {
                try {
                    double plat = Double.parseDouble(parts[0].trim());
                    double plon = Double.parseDouble(parts[1].trim());
                    builder.queryParam("viewbox",
                            (plon - 0.3) + "," + (plat + 0.2) + "," + (plon + 0.3) + "," + (plat - 0.2))
                            .queryParam("bounded", 0);
                } catch (NumberFormatException ignored) {}
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> raw = resp.getBody() != null ? resp.getBody() : List.of();
            List<Map<String, Object>> predictions = new ArrayList<>();

            for (Map<String, Object> item : raw) {
                Map<String, Object> address = item.containsKey("address")
                        ? (Map<String, Object>) item.get("address") : Map.of();

                String displayName = (String) item.getOrDefault("display_name", "");
                String mainText = buildMainText(address, displayName);
                String secondaryText = buildSecondaryText(address, displayName, mainText);
                String placeId = String.valueOf(item.getOrDefault("place_id", ""));
                String lat2 = String.valueOf(item.getOrDefault("lat", "0"));
                String lon2 = String.valueOf(item.getOrDefault("lon", "0"));

                // Google-Maps-compatible prediction shape
                Map<String, Object> pred = new LinkedHashMap<>();
                pred.put("place_id", placeId);
                pred.put("description", displayName);
                pred.put("structured_formatting", Map.of(
                        "main_text", mainText,
                        "secondary_text", secondaryText
                ));
                // Embed coordinates so the client can resolve them without a details call
                pred.put("lat", lat2);
                pred.put("lon", lon2);
                predictions.add(pred);
            }

            return ResponseEntity.ok(Map.of("predictions", predictions));
        } catch (Exception e) {
            log.error("Nominatim autocomplete error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("predictions", List.of()));
        }
    }

    /**
     * Returns lat/lng for a place given its Nominatim place_id.
     * Returns in Google-Maps-compatible format: result.geometry.location.lat/lng
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> details(
            @RequestParam("place_id") String placeId) {

        // If the client embedded coordinates in place_id as "lat,lon" (used as fallback)
        if (placeId.contains(",")) {
            String[] parts = placeId.split(",");
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                return ResponseEntity.ok(buildGoogleStyleDetailResponse(lat, lon));
            } catch (NumberFormatException ignored) {}
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE + "/lookup")
                .queryParam("osm_ids", "N" + placeId + ",W" + placeId + ",R" + placeId)
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> raw = resp.getBody();
            if (raw != null && !raw.isEmpty()) {
                double lat = Double.parseDouble(String.valueOf(raw.get(0).get("lat")));
                double lon = Double.parseDouble(String.valueOf(raw.get(0).get("lon")));
                return ResponseEntity.ok(buildGoogleStyleDetailResponse(lat, lon));
            }
        } catch (Exception e) {
            log.error("Nominatim details error for place_id {}: {}", placeId, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("result", Map.of()));
    }

    /**
     * Reverse geocode: convert lat/lng coordinates to a human-readable address.
     * Returns in Google-Maps-compatible format (results[0].formatted_address).
     */
    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, Object>> reverseGeocode(
            @RequestParam(value = "latlng", required = false) String latlng,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon) {

        double resolvedLat;
        double resolvedLon;

        if (lat != null && lon != null) {
            resolvedLat = lat;
            resolvedLon = lon;
        } else if (latlng != null && latlng.contains(",")) {
            String[] parts = latlng.split(",");
            try {
                resolvedLat = Double.parseDouble(parts[0].trim());
                resolvedLon = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.ok(Map.of("results", List.of()));
            }
        } else {
            return ResponseEntity.ok(Map.of("results", List.of()));
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE + "/reverse")
                .queryParam("lat", resolvedLat)
                .queryParam("lon", resolvedLon)
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("zoom", 16)
                .queryParam("accept-language", "en");

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, Map.class);
            Map<String, Object> raw = resp.getBody();

            if (raw != null && raw.containsKey("display_name")) {
                Map<String, Object> address = raw.containsKey("address")
                        ? (Map<String, Object>) raw.get("address") : Map.of();

                String formattedAddress = buildReverseAddress(address,
                        (String) raw.getOrDefault("display_name", ""));

                // Google-Maps-compatible result shape
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("formatted_address", formattedAddress);
                result.put("types", List.of("street_address"));
                result.put("place_id", String.valueOf(raw.getOrDefault("place_id", "")));

                return ResponseEntity.ok(Map.of("results", List.of(result)));
            }
        } catch (Exception e) {
            log.error("Nominatim reverse geocode error for {},{}: {}", resolvedLat, resolvedLon, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("results", List.of()));
    }

    // ──────────────────────────── helpers ────────────────────────────

    private String buildMainText(Map<String, Object> address, String displayName) {
        // Prefer specific named place, then road, then suburb
        for (String key : new String[]{"amenity", "tourism", "shop", "road", "pedestrian", "path", "suburb", "neighbourhood"}) {
            if (address.containsKey(key) && address.get(key) != null) {
                return String.valueOf(address.get(key));
            }
        }
        // Fall back to first part of display name
        String[] parts = displayName.split(",");
        return parts.length > 0 ? parts[0].trim() : displayName;
    }

    private String buildSecondaryText(Map<String, Object> address, String displayName, String mainText) {
        List<String> parts = new ArrayList<>();
        for (String key : new String[]{"suburb", "city_district", "city", "county", "state"}) {
            if (address.containsKey(key) && address.get(key) != null) {
                String val = String.valueOf(address.get(key));
                if (!val.equals(mainText)) parts.add(val);
            }
        }
        if (!parts.isEmpty()) return String.join(", ", parts);
        // Strip main text from display name and return the rest
        if (displayName.startsWith(mainText)) {
            String rest = displayName.substring(mainText.length()).replaceFirst("^,\\s*", "");
            return rest.isEmpty() ? "Rwanda" : rest;
        }
        return "Rwanda";
    }

    /**
     * Build a clean, human-readable address from Nominatim address components.
     * Priority: road + house_number → suburb/neighbourhood → city → district
     */
    private String buildReverseAddress(Map<String, Object> address, String displayName) {
        List<String> parts = new ArrayList<>();

        String road = getAddressField(address, "road", "pedestrian", "path", "footway");
        String houseNumber = getAddressField(address, "house_number");
        String suburb = getAddressField(address, "suburb", "neighbourhood", "quarter", "city_district");
        String city = getAddressField(address, "city", "town", "village", "municipality");
        String country = getAddressField(address, "country");

        if (road != null) {
            if (houseNumber != null) {
                parts.add(houseNumber + " " + road);
            } else {
                parts.add(road);
            }
        }
        if (suburb != null) parts.add(suburb);
        if (city != null && !city.equals(suburb)) parts.add(city);

        if (parts.isEmpty()) {
            // Last resort: use the first meaningful segment of display_name
            String[] segments = displayName.split(",");
            for (String seg : segments) {
                seg = seg.trim();
                if (!seg.isEmpty() && !seg.equalsIgnoreCase("rwanda")) {
                    parts.add(seg);
                    if (parts.size() >= 3) break;
                }
            }
        }

        return parts.isEmpty() ? displayName : String.join(", ", parts);
    }

    private String getAddressField(Map<String, Object> address, String... keys) {
        for (String key : keys) {
            if (address.containsKey(key) && address.get(key) != null) {
                return String.valueOf(address.get(key));
            }
        }
        return null;
    }

    private Map<String, Object> buildGoogleStyleDetailResponse(double lat, double lon) {
        return Map.of("result", Map.of(
                "geometry", Map.of(
                        "location", Map.of("lat", lat, "lng", lon)
                )
        ));
    }
}
