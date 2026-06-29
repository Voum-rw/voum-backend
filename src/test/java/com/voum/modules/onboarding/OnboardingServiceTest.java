package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import com.voum.modules.onboarding.dto.AdminVerifyRequest;
import com.voum.modules.onboarding.dto.OnboardingStatusResponse;
import com.voum.modules.onboarding.dto.PassengerCompleteRequest;
import com.voum.modules.users.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private MotariRepository motariRepository;

    @Mock
    private UploadedDocumentRepository documentRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void getOnboardingStatus_passengerWithoutImage_shouldReturnFiftyPercent() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.PASSENGER).build();
        Passenger passenger = Passenger.builder().id(userId).user(user).firstName("Jane").lastName("Doe").onboardingStatus("IN_PROGRESS").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(userId)).thenReturn(Optional.of(passenger));
        when(documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE")).thenReturn(false);

        OnboardingStatusResponse status = onboardingService.getOnboardingStatus(userId);

        assertNotNull(status);
        assertFalse(status.isCompleted());
        assertEquals(50, status.getProfileCompletion());
        assertTrue(status.getMissingFields().contains("profileImage"));
    }

    @Test
    void getOnboardingStatus_motariFullyUploaded_shouldReturnHundredPercent() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.MOTARI).build();
        Motari motari = Motari.builder()
                .id(userId)
                .user(user)
                .motoPlateNumber("RA123A")
                .verificationStatus("PENDING")
                .onboardingStatus("IN_PROGRESS")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(motariRepository.findById(userId)).thenReturn(Optional.of(motari));
        when(documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE")).thenReturn(true);
        when(documentRepository.existsByOwnerIdAndDocumentType(userId, "DRIVING_PERMIT")).thenReturn(true);
        when(documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_FRONT")).thenReturn(true);

        OnboardingStatusResponse status = onboardingService.getOnboardingStatus(userId);

        assertNotNull(status);
        assertEquals(100, status.getProfileCompletion());
        assertTrue(status.getMissingFields().isEmpty());
    }

    @Test
    void completePassengerOnboarding_withoutImage_shouldThrowException() {
        UUID userId = UUID.randomUUID();
        Passenger passenger = Passenger.builder().id(userId).onboardingStatus("IN_PROGRESS").build();
        PassengerCompleteRequest req = new PassengerCompleteRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");

        when(passengerRepository.findById(userId)).thenReturn(Optional.of(passenger));
        when(documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> 
                onboardingService.completePassengerOnboarding(userId, req));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void approveVerification_shouldTransitionStatusToActive() {
        UUID requestId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID motariId = UUID.randomUUID();

        VerificationRequest request = VerificationRequest.builder()
                .id(requestId)
                .motariId(motariId)
                .status("PENDING")
                .build();

        User user = User.builder().id(motariId).status("PENDING_VERIFICATION").build();
        Motari motari = Motari.builder()
                .id(motariId)
                .user(user)
                .verificationStatus("PENDING")
                .onboardingStatus("IN_PROGRESS")
                .build();

        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(motariRepository.findById(motariId)).thenReturn(Optional.of(motari));

        AdminVerifyRequest verifyRequest = new AdminVerifyRequest();
        verifyRequest.setAdminNotes("Documents are authentic.");

        onboardingService.approveVerification(requestId, adminId, verifyRequest);

        assertEquals("APPROVED", request.getStatus());
        assertEquals(adminId, request.getVerifiedBy());
        assertEquals("APPROVED", motari.getVerificationStatus());
        assertEquals("COMPLETED", motari.getOnboardingStatus());
        assertEquals("ACTIVE", user.getStatus());
        assertTrue(user.getIsVerified());
    }

    @Test
    void rejectVerification_withoutReason_shouldThrowException() {
        UUID requestId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        VerificationRequest request = VerificationRequest.builder()
                .id(requestId)
                .status("PENDING")
                .build();

        when(verificationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        AdminVerifyRequest verifyRequest = new AdminVerifyRequest();
        verifyRequest.setRejectionReason(""); // Empty rejection reason

        ApiException exception = assertThrows(ApiException.class, () -> 
                onboardingService.rejectVerification(requestId, adminId, verifyRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
