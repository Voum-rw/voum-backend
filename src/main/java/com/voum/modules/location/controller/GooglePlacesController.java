package com.voum.modules.location.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/location/places")
@RequiredArgsConstructor
public class GooglePlacesController {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_KEY = "AIzaSyCETTh-BjISLM5qmhscx-VkoPs204aCAL0";

    @GetMapping("/autocomplete")
    public ResponseEntity<Object> autocomplete(
            @RequestParam("input") String input,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "radius", required = false) String radius) {
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json")
                .queryParam("input", input)
                .queryParam("key", API_KEY)
                .queryParam("language", "en")
                .queryParam("components", "country:rw");

        if (location != null && !location.isEmpty()) {
            builder.queryParam("location", location);
        }
        if (radius != null && !radius.isEmpty()) {
            builder.queryParam("radius", radius);
        }

        String url = builder.toUriString();
        Object response = restTemplate.getForObject(url, Object.class);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details")
    public ResponseEntity<Object> details(
            @RequestParam("place_id") String placeId) {
        
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "geometry")
                .queryParam("key", API_KEY)
                .toUriString();

        Object response = restTemplate.getForObject(url, Object.class);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/directions")
    public ResponseEntity<Object> directions(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination) {
        
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/directions/json")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("key", API_KEY)
                .toUriString();

        Object response = restTemplate.getForObject(url, Object.class);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<Object> reverseGeocode(
            @RequestParam("latlng") String latlng) {
        
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", latlng)
                .queryParam("key", API_KEY)
                .toUriString();

        Object response = restTemplate.getForObject(url, Object.class);
        return ResponseEntity.ok(response);
    }
}
