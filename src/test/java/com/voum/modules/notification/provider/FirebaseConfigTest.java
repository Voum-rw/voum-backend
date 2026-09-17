package com.voum.modules.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FirebaseConfigTest {
    @Test void uppercaseFieldsAreConvertedAndEscapedNewlinesRestored() throws Exception {
        var json = FirebaseConfig.credentialJson(Map.of("PROJECT_ID", "test-project", "PRIVATE_KEY", "line1\\nline2", "CLIENT_EMAIL", "test@example.invalid"));
        var fields = new ObjectMapper().readTree(json);
        assertEquals("test-project", fields.get("project_id").asText());
        assertEquals("line1\nline2", fields.get("private_key").asText());
        assertEquals("service_account", fields.get("type").asText());
    }
    @Test void prefixedVariablesAreAccepted() throws Exception {
        var json = FirebaseConfig.credentialJson(Map.of("FIREBASE_PROJECT_ID", "test-project", "FIREBASE_PRIVATE_KEY", "test-placeholder", "FIREBASE_CLIENT_EMAIL", "test@example.invalid"));
        assertEquals("test-project", new ObjectMapper().readTree(json).get("project_id").asText());
    }
    @Test void wholeJsonKeepsPriority() throws Exception {
        assertEquals("{}", FirebaseConfig.credentialJson(Map.of("FIREBASE_CREDENTIALS", "{}", "PRIVATE_KEY", "placeholder")));
    }
    @Test void missingAndIncompleteCredentialsAreDistinguished() throws Exception {
        assertNull(FirebaseConfig.credentialJson(Map.of("PROJECT_ID", "unrelated")));
        assertThrows(IllegalArgumentException.class, () -> FirebaseConfig.credentialJson(Map.of("CLIENT_EMAIL", "test@example.invalid")));
    }
}
