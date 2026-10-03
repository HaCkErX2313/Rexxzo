package com.rexxo.dto.shipping;

import java.util.List;

public record PincodeLookupResponse(
    boolean success,
    String pincode,
    String state,
    String district,
    String city,
    List<LocationOption> locations,
    String message
) {
    public record LocationOption(String name, String district, String state) {}

    public static PincodeLookupResponse success(
        String pincode,
        String state,
        String district,
        String city,
        List<LocationOption> locations
    ) {
        return new PincodeLookupResponse(true, pincode, state, district, city, locations, "Location found");
    }

    public static PincodeLookupResponse notFound(String pincode) {
        return new PincodeLookupResponse(false, pincode, null, null, null, List.of(), "Pincode not found");
    }

    public static PincodeLookupResponse error(String pincode, String message) {
        return new PincodeLookupResponse(false, pincode, null, null, null, List.of(), message);
    }
}
