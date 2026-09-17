package com.voum.modules.location;

import com.voum.modules.location.controller.NominatimPlacesController;
import com.voum.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GooglePlacesControllerTest {
    @Test void reverseUsesConfiguredServerKeyAndKeepsGoogleAddress() {
        var controller = new NominatimPlacesController();
        ReflectionTestUtils.setField(controller, "googleApiKey", "test-server-key");
        var server = MockRestServiceServer.createServer((RestTemplate) ReflectionTestUtils.getField(controller, "client"));
        server.expect(queryParam("key", "test-server-key")).andExpect(queryParam("latlng", "-1.95,30.1"))
            .andRespond(withSuccess("{\"status\":\"OK\",\"results\":[{\"formatted_address\":\"Kigali, Rwanda\"}]}", MediaType.APPLICATION_JSON));
        var result = controller.reverseGeocode(-1.95, 30.1);
        assertEquals("GOOGLE", result.get("provider"));
        assertTrue(result.get("results").toString().contains("Kigali, Rwanda"));
        server.verify();
    }
    @Test void providerDenialIsActionableWithoutExposingKey() {
        var controller = new NominatimPlacesController();
        ReflectionTestUtils.setField(controller, "googleApiKey", "test-server-key");
        var server = MockRestServiceServer.createServer((RestTemplate) ReflectionTestUtils.getField(controller, "client"));
        server.expect(queryParam("key", "test-server-key"))
            .andRespond(withSuccess("{\"status\":\"REQUEST_DENIED\"}", MediaType.APPLICATION_JSON));
        var error = assertThrows(ApiException.class, () -> controller.reverseGeocode(-1.95, 30.1));
        assertTrue(error.getMessage().contains("enabled APIs"));
        assertFalse(error.getMessage().contains("test-server-key"));
    }
}
