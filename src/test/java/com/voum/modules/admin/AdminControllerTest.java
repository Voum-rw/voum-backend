package com.voum.modules.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.common.ApiResponse;
import com.voum.modules.users.User;
import com.voum.modules.users.Role;
import com.voum.modules.admin.controller.AdminController;
import com.voum.modules.admin.service.AdminService;
import com.voum.modules.audit.service.AuditLogService;
import com.voum.modules.admin.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.voum.modules.onboarding.OnboardingService onboardingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        AdminController adminController = new AdminController(adminService, auditLogService, onboardingService);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();
    }

    // ── Endpoint API Routing Tests ────────────────────────────────────────────

    @Test
    public void getDashboard_shouldReturnDashboardData() throws Exception {
        AdminDashboardResponse response = AdminDashboardResponse.builder()
                .totalPassengers(10L)
                .totalMotaris(5L)
                .verifiedMotaris(3L)
                .build();

        when(adminService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPassengers").value(10))
                .andExpect(jsonPath("$.data.totalMotaris").value(5))
                .andExpect(jsonPath("$.data.verifiedMotaris").value(3));

        verify(adminService, times(1)).getDashboard();
    }

    @Test
    public void suspendUser_shouldInvokeSuspendService() throws Exception {
        SuspendRequest req = SuspendRequest.builder()
                .reason("Fake documentation")
                .build();

        UUID targetId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User suspended successfully."));

        verify(adminService, times(1)).suspendUser(eq(targetId), eq("Fake documentation"), any());
    }

    @Test
    public void archiveUser_shouldInvokeArchiveService() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User account archived (soft-deleted) successfully."));

        verify(adminService, times(1)).archiveUser(eq(targetId), any());
    }

    @Test
    public void addAdminNote_shouldInvokeNoteService() throws Exception {
        UUID targetId = UUID.randomUUID();
        Map<String, String> body = Map.of("note", "Investigation pending");

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminService, times(1)).addAdminNote(eq(targetId), eq("Investigation pending"), any());
    }

    // ── Security Annotation Verification Tests (Reflection) ───────────────────

    @Test
    public void verifyControllerLevelSecurity() {
        PreAuthorize classPreAuth = AdminController.class.getAnnotation(PreAuthorize.class);
        // PreAuthorize is defined at method level for role granularity in Sprint 10
        assertNull(classPreAuth); 
    }

    @Test
    public void verifySuspendUserSecurityAnnotation_superAdminOnly() throws Exception {
        Method method = AdminController.class.getMethod("suspendUser", UUID.class, UUID.class, SuspendRequest.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SUPER_ADMIN')", preAuth.value());
    }

    @Test
    public void verifyReactivateUserSecurityAnnotation_superAdminOnly() throws Exception {
        Method method = AdminController.class.getMethod("reactivateUser", UUID.class, UUID.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SUPER_ADMIN')", preAuth.value());
    }

    @Test
    public void verifyArchiveUserSecurityAnnotation_superAdminOnly() throws Exception {
        Method method = AdminController.class.getMethod("archiveUser", UUID.class, UUID.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasRole('SUPER_ADMIN')", preAuth.value());
    }

    @Test
    public void verifyDashboardSecurityAnnotation_adminOrSuperAdmin() throws Exception {
        Method method = AdminController.class.getMethod("getDashboard");
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasAnyRole('ADMIN', 'SUPER_ADMIN')", preAuth.value());
    }

    @Test
    public void verifyGetTripDetailsSecurityAnnotation_adminOrSuperAdmin() throws Exception {
        Method method = AdminController.class.getMethod("getTripDetails", UUID.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuth);
        assertEquals("hasAnyRole('ADMIN', 'SUPER_ADMIN')", preAuth.value());
    }
}
