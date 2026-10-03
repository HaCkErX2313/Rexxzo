package com.rexxo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.dto.shipping.PincodeLookupResponse;
import com.rexxo.dto.shipping.PincodeLookupResponse.LocationOption;
import com.rexxo.exception.RexxoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PincodeService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, PincodeLookupResponse> cache = new ConcurrentHashMap<>();

    public PincodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.postalpincode.in")
            .build();
    }

    public PincodeLookupResponse lookupPincode(String pincode) {
        if (pincode == null || !pincode.matches("^\\d{6}$")) {
            return PincodeLookupResponse.error(pincode, "Pincode must be exactly 6 digits");
        }

        // Return cached result if present
        if (cache.containsKey(pincode)) {
            return cache.get(pincode);
        }

        try {
            String rawJson = webClient.get()
                .uri("/pincode/{pincode}", pincode)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(6))
                .block();

            if (rawJson == null || rawJson.isBlank()) {
                return PincodeLookupResponse.notFound(pincode);
            }

            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray() || root.isEmpty()) {
                return PincodeLookupResponse.notFound(pincode);
            }

            JsonNode firstObj = root.get(0);
            String status = firstObj.path("Status").asText("");

            if (!"Success".equalsIgnoreCase(status) || !firstObj.has("PostOffice") || firstObj.get("PostOffice").isNull()) {
                PincodeLookupResponse notFound = PincodeLookupResponse.notFound(pincode);
                cache.put(pincode, notFound);
                return notFound;
            }

            JsonNode postOffices = firstObj.get("PostOffice");
            if (!postOffices.isArray() || postOffices.isEmpty()) {
                PincodeLookupResponse notFound = PincodeLookupResponse.notFound(pincode);
                cache.put(pincode, notFound);
                return notFound;
            }

            List<LocationOption> locations = new ArrayList<>();
            String primaryState = null;
            String primaryDistrict = null;
            String primaryCity = null;

            for (JsonNode po : postOffices) {
                String name = po.path("Name").asText("").trim();
                String district = po.path("District").asText("").trim();
                String state = po.path("State").asText("").trim();
                String block = po.path("Block").asText("").trim();

                if (primaryState == null && !state.isBlank()) primaryState = state;
                if (primaryDistrict == null && !district.isBlank()) primaryDistrict = district;
                if (primaryCity == null && !district.isBlank()) primaryCity = district;

                locations.add(new LocationOption(name, district, state));
            }

            if (primaryCity == null && !locations.isEmpty()) {
                primaryCity = locations.get(0).name();
            }

            PincodeLookupResponse result = PincodeLookupResponse.success(
                pincode,
                primaryState,
                primaryDistrict,
                primaryCity,
                locations
            );

            cache.put(pincode, result);
            return result;

        } catch (Exception e) {
            log.warn("Error calling Postal Pincode API for pincode {}: {}", pincode, e.getMessage());
            return PincodeLookupResponse.error(pincode, "Unable to verify pincode at this time. Please check your network.");
        }
    }

    public PincodeLookupResponse validatePincodeAndState(String pincode, String state) {
        if (pincode == null || !pincode.matches("^\\d{6}$")) {
            throw RexxoException.badRequest("Invalid delivery pincode: Pincode must be exactly 6 digits.");
        }
        if (state == null || state.isBlank()) {
            throw RexxoException.badRequest("State is required.");
        }

        PincodeLookupResponse lookup = lookupPincode(pincode);
        if (!lookup.success()) {
            throw RexxoException.badRequest("Invalid pincode " + pincode + ": " + lookup.message());
        }

        if (!statesMatch(lookup.state(), state)) {
            throw RexxoException.badRequest(
                "Pincode " + pincode + " belongs to " + lookup.state() +
                ", but '" + state + "' was selected. Please select the correct state."
            );
        }

        return lookup;
    }

    public static boolean statesMatch(String state1, String state2) {
        if (state1 == null || state2 == null) return false;
        String s1 = normalizeState(state1);
        String s2 = normalizeState(state2);
        return s1.equals(s2) || s1.contains(s2) || s2.contains(s1);
    }

    private static String normalizeState(String state) {
        return state.toLowerCase()
            .replaceAll("&", "and")
            .replaceAll("[^a-z0-9]", "")
            .replace("nctofdelhi", "delhi")
            .replace("nationalcapitalterritoryofdelhi", "delhi")
            .replace("pondicherry", "puducherry")
            .replace("uttaranchal", "uttarakhand")
            .replace("orissa", "odisha");
    }
}
