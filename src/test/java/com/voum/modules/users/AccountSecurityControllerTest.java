package com.voum.modules.users;

import com.voum.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSecurityControllerTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @InjectMocks AccountSecurityController controller;

    @Test void changingPasswordRequiresCurrentPasswordAndRevokesRefreshTokens() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).password("hash").build();
        when(users.findForAccountUpdate(id)).thenReturn(Optional.of(user));
        when(encoder.matches("old", "hash")).thenReturn(true);
        when(encoder.encode("new-password")).thenReturn("new-hash");
        var request = new AccountSecurityController.PasswordChange();
        request.setCurrentPassword("old"); request.setNewPassword("new-password");
        controller.changePassword(id, request);
        assertEquals("new-hash", user.getPassword());
        verify(tokens).deleteByUser(user);
    }
    @Test void wrongPasswordCannotEnrollEmail() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).password("hash").build();
        when(users.findForAccountUpdate(id)).thenReturn(Optional.of(user));
        var request = new AccountSecurityController.EmailEnrollment();
        request.setCurrentPassword("wrong"); request.setEmail("owner@example.com");
        assertThrows(ApiException.class, () -> controller.enrollEmail(id, request));
        verify(users, never()).save(any());
    }
    @Test void existingEmailCannotBeReplacedEvenWithCorrectPassword() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).password("hash").email("original@example.com").build();
        when(users.findForAccountUpdate(id)).thenReturn(Optional.of(user));
        when(encoder.matches("password", "hash")).thenReturn(true);
        var request = new AccountSecurityController.EmailEnrollment();
        request.setCurrentPassword("password"); request.setEmail("replacement@example.com");
        assertThrows(ApiException.class, () -> controller.enrollEmail(id, request));
        assertEquals("original@example.com", user.getEmail());
        verify(users, never()).save(any());
    }
}
