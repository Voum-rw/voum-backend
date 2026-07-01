package com.voum.modules.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.common.ApiResponse;
import com.voum.modules.support.controller.SupportController;
import com.voum.modules.support.entity.SupportTicket;
import com.voum.modules.support.entity.UserReport;
import com.voum.modules.support.entity.LostItem;
import com.voum.modules.support.service.SupportService;
import com.voum.modules.support.service.ReportService;
import com.voum.modules.support.service.LostItemService;
import com.voum.modules.support.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SupportControllerTest {

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
        SupportController controller = new SupportController(supportService, reportService, lostItemService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void createTicket_shouldInvokeSupportServiceAndReturnTicket() throws Exception {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .type(SupportTicket.TicketType.GENERAL_SUPPORT)
                .subject("Lateness complaint")
                .description("Driver was late")
                .build();

        SupportTicket ticket = SupportTicket.builder()
                .id(UUID.randomUUID())
                .subject("Lateness complaint")
                .build();

        when(supportService.createTicket(any(), any(), any(), any(), any(), any(), any())).thenReturn(ticket);

        mockMvc.perform(post("/api/v1/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subject").value("Lateness complaint"));
    }

    @Test
    public void createReport_shouldInvokeReportServiceAndReturnReport() throws Exception {
        CreateReportRequest req = CreateReportRequest.builder()
                .reportedUserId(UUID.randomUUID())
                .reason("Speeding")
                .description("Too fast")
                .severity(UserReport.ReportSeverity.HIGH)
                .build();

        UserReport report = UserReport.builder()
                .id(UUID.randomUUID())
                .reason("Speeding")
                .build();

        when(reportService.createReport(any(), any(), any(), any(), any(), any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void createLostItem_shouldInvokeLostItemService() throws Exception {
        CreateLostItemRequest req = CreateLostItemRequest.builder()
                .itemName("Wallet")
                .description("Left in seat")
                .build();

        LostItem item = LostItem.builder()
                .id(UUID.randomUUID())
                .itemName("Wallet")
                .build();

        when(lostItemService.createReport(any(), any(), any(), any())).thenReturn(item);

        mockMvc.perform(post("/api/v1/lost-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
