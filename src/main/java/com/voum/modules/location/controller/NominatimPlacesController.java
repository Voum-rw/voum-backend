package com.voum.modules.location.controller;

import com.voum.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;

/** Google-backed location names; never substitute unrelated provider results. */
@RestController
@RequestMapping("/api/v1/location/places")
public class NominatimPlacesController {
    @Value("${google.maps.api-key:}") private String googleApiKey;
    private final RestTemplate client;
    public NominatimPlacesController() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(7000);
        client = new RestTemplate(factory);
    }
    @GetMapping("/autocomplete")
    public Map<String, Object> autocomplete(@RequestParam("input") String input,
            @RequestParam(value="lat", required=false) Double lat,
            @RequestParam(value="lon", required=false) Double lon) {
        if (input.trim().length() < 2) return Map.of("predictions", List.of(), "provider", "GOOGLE");
        var query = base("place/autocomplete/json").queryParam("input", input.trim()).queryParam("components", "country:rw");
        if (lat != null && lon != null) {
            validate(lat, lon);
            query.queryParam("location", lat + "," + lon).queryParam("radius", 5000).queryParam("strictbounds", true);
        }
        return call(query);
    }
    @GetMapping("/details")
    public Map<String, Object> details(@RequestParam("place_id") String id) {
        if (id.isBlank() || id.length() > 512) throw new ApiException("Invalid place ID.", HttpStatus.BAD_REQUEST);
        return call(base("place/details/json").queryParam("place_id", id).queryParam("fields", "geometry,name,formatted_address"));
    }
    @GetMapping("/reverse-geocode")
    public Map<String, Object> reverseGeocode(@RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        validate(lat, lon);
        return call(base("geocode/json").queryParam("latlng", lat + "," + lon));
    }
    private void validate(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw new ApiException("Invalid coordinates.", HttpStatus.BAD_REQUEST);
    }
    private UriComponentsBuilder base(String path) {
        if (googleApiKey == null || googleApiKey.isBlank()) throw new ApiException("Place search is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        return UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/" + path).queryParam("key", googleApiKey.trim()).queryParam("language", "en");
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> call(UriComponentsBuilder uri) {
        Map<String, Object> result;
        try { result = client.getForObject(uri.build().encode().toUri(), Map.class); }
        catch (Exception error) { throw new ApiException("Place search is temporarily unavailable.", HttpStatus.SERVICE_UNAVAILABLE); }
        if (result == null || !("OK".equals(result.get("status")) || "ZERO_RESULTS".equals(result.get("status"))))
            throw new ApiException("Place search is temporarily unavailable.", HttpStatus.SERVICE_UNAVAILABLE);
        result.put("provider", "GOOGLE");
        return result;
    }
}
