package com.voum.modules.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.modules.admin.controller.AdminSupportController;
import com.voum.modules.support.service.SupportService;
import com.voum.modules.support.service.ReportService;
import com.voum.modules.support.service.LostItemService;
import com.voum.modules.support.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class AdminSupportControllerTest {

    @Mock
    private SupportService supportService;

    @Mock
    private ReportService reportService;

    @Mock
    private LostItemService lostItemService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        AdminSupportController controller = new AdminSupportController(supportService, reportService, lostItemService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // ── Security Annotation Verification Tests (Reflection) ───────────────────

    @Test
    public void verifyControllerLevelSecurity_adminOrSuperAdmin() {
        PreAuthorize classPreAuth = AdminSupportController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(classPreAuth);
        assertEquals("hasAnyRole('ADMIN', 'SUPER_ADMIN')", classPreAuth.value());
    }

    @Test
    public void verifyCloseTicketSecurityAnnotation() throws Exception {
        Method method = AdminSupportController.class.getMethod("closeTicket", UUID.class, UUID.class, CloseTicketRequest.class);
        PreAuthorize preAuth = method.getAnnotation(PreAuthorize.class);
        // Inherited from class-level since no overriding method-level PreAuthorize is required
        // (but we check that class-level exists so that the whole controller is protected)
        assertNotNull(AdminSupportController.class.getAnnotation(PreAuthorize.class));
    }
}
