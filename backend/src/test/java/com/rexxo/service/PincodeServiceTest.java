package com.rexxo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rexxo.dto.shipping.PincodeLookupResponse;
import com.rexxo.exception.RexxoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PincodeServiceTest {

    private PincodeService pincodeService;

    @BeforeEach
    void setUp() {
        pincodeService = new PincodeService(new ObjectMapper());
    }

    @Test
    @DisplayName("Should reject pincode with invalid length or non-digits")
    void testInvalidPincodeFormat() {
        PincodeLookupResponse res1 = pincodeService.lookupPincode("12345");
        assertFalse(res1.success());
        assertEquals("Pincode must be exactly 6 digits", res1.message());

        PincodeLookupResponse res2 = pincodeService.lookupPincode("1234567");
        assertFalse(res2.success());

        PincodeLookupResponse res3 = pincodeService.lookupPincode("ABCDEF");
        assertFalse(res3.success());
    }

    @Test
    @DisplayName("Should successfully lookup valid Indian pincode 560001 (Bangalore, Karnataka)")
    void testValidPincodeLookup() {
        PincodeLookupResponse res = pincodeService.lookupPincode("560001");
        assertTrue(res.success());
        assertEquals("560001", res.pincode());
        assertEquals("Karnataka", res.state());
        assertNotNull(res.district());
        assertNotNull(res.city());
        assertFalse(res.locations().isEmpty());
    }

    @Test
    @DisplayName("Should reject invalid or non-existent pincode 999999")
    void testNonExistentPincode() {
        PincodeLookupResponse res = pincodeService.lookupPincode("999999");
        assertFalse(res.success());
        assertEquals("Pincode not found", res.message());
    }

    @Test
    @DisplayName("Should pass validation when state matches pincode")
    void testValidatePincodeAndState_Success() {
        assertDoesNotThrow(() -> {
            pincodeService.validatePincodeAndState("560001", "Karnataka");
        });
    }

    @Test
    @DisplayName("Should throw badRequest when state mismatches pincode")
    void testValidatePincodeAndState_Mismatch() {
        RexxoException exception = assertThrows(RexxoException.class, () -> {
            pincodeService.validatePincodeAndState("560001", "Maharashtra");
        });
        assertTrue(exception.getMessage().contains("belongs to Karnataka"));
        assertTrue(exception.getMessage().contains("Maharashtra"));
    }

    @Test
    @DisplayName("Should throw badRequest when pincode is invalid during order validation")
    void testValidatePincodeAndState_InvalidPincode() {
        RexxoException exception = assertThrows(RexxoException.class, () -> {
            pincodeService.validatePincodeAndState("999999", "Karnataka");
        });
        assertTrue(exception.getMessage().contains("Invalid pincode 999999"));
        assertTrue(exception.getMessage().contains("Pincode not found"));
    }

    @Test
    @DisplayName("Should correctly match normalized state aliases like Delhi and NCT of Delhi")
    void testStateNormalization() {
        assertTrue(PincodeService.statesMatch("Delhi", "NCT of Delhi"));
        assertTrue(PincodeService.statesMatch("Orissa", "Odisha"));
        assertTrue(PincodeService.statesMatch("Pondicherry", "Puducherry"));
        assertTrue(PincodeService.statesMatch("Jammu & Kashmir", "Jammu and Kashmir"));
        assertFalse(PincodeService.statesMatch("Karnataka", "Kerala"));
    }
}
