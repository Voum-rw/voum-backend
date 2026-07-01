package com.voum.modules.onboarding;

import com.voum.common.ApiException;
import com.voum.modules.onboarding.dto.*;
import com.voum.modules.onboarding.events.AccountApprovedEvent;
import com.voum.modules.onboarding.events.AccountRejectedEvent;
import com.voum.modules.users.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;
    private final UploadedDocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        List<String> missingFields = new ArrayList<>();
        int completion = 0;
        boolean completed = false;
        String verStatus = "N/A";

        if (user.getRole() == Role.PASSENGER) {
            Passenger passenger = passengerRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("Passenger profile not found.", HttpStatus.NOT_FOUND));

            completed = "COMPLETED".equals(passenger.getOnboardingStatus());

            // Check details
            if (passenger.getFirstName() != null && !passenger.getFirstName().trim().isEmpty() &&
                passenger.getLastName() != null && !passenger.getLastName().trim().isEmpty()) {
                completion += 50;
            } else {
                missingFields.add("name");
            }

            boolean hasImage = documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE");
            if (hasImage) {
                completion += 50;
            } else {
                missingFields.add("profileImage");
            }

        } else if (user.getRole() == Role.MOTARI) {
            Motari motari = motariRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

            completed = "COMPLETED".equals(motari.getOnboardingStatus());
            verStatus = motari.getVerificationStatus();

            // 1. Plate Number check
            if (motari.getMotoPlateNumber() != null && !motari.getMotoPlateNumber().trim().isEmpty()) {
                completion += 25;
            } else {
                missingFields.add("motoPlateNumber");
            }

            // 2. Profile Image check
            if (documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE")) {
                completion += 25;
            } else {
                missingFields.add("profileImage");
            }

            // 3. Driving Permit check
            if (documentRepository.existsByOwnerIdAndDocumentType(userId, "DRIVING_PERMIT")) {
                completion += 25;
            } else {
                missingFields.add("drivingPermit");
            }

            // 4. National ID check
            if (documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_FRONT")) {
                completion += 25;
            } else {
                missingFields.add("nationalIdFront");
            }
        }

        return OnboardingStatusResponse.builder()
                .completed(completed)
                .verificationStatus(verStatus)
                .profileCompletion(completion)
                .missingFields(missingFields)
                .build();
    }

    @Transactional
    public UploadedDocument uploadDocument(UUID userId, String docType, byte[] content, String originalFilename, String contentType) {
        log.info("Processing document upload of type '{}' for user: {}", docType, userId);

        // Standardize document folder naming
        String folder = "user-" + userId;
        StoredFile storedFile = storageService.uploadFile(folder, originalFilename, content, contentType);

        // Check if document of this type already exists for the owner
        Optional<UploadedDocument> existingDocOpt = documentRepository.findByOwnerIdAndDocumentType(userId, docType);
        
        UploadedDocument doc;
        if (existingDocOpt.isPresent()) {
            doc = existingDocOpt.get();
            doc.setFileUrl(storedFile.fileUrl());
            doc.setFileName(storedFile.fileName());
            doc.setContentType(storedFile.contentType());
            doc.setFileSize(storedFile.size());
        } else {
            doc = UploadedDocument.builder()
                    .ownerId(userId)
                    .documentType(docType)
                    .fileUrl(storedFile.fileUrl())
                    .fileName(storedFile.fileName())
                    .contentType(storedFile.contentType())
                    .fileSize(storedFile.size())
                    .build();
        }

        doc = documentRepository.save(doc);

        // Update profile links if PROFILE_IMAGE
        if ("PROFILE_IMAGE".equals(docType)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));
            if (user.getRole() == Role.PASSENGER) {
                Passenger passenger = passengerRepository.findById(userId).orElseThrow();
                passenger.setProfileImage(storedFile.fileUrl());
                passengerRepository.save(passenger);
            } else if (user.getRole() == Role.MOTARI) {
                Motari motari = motariRepository.findById(userId).orElseThrow();
                motari.setProfileImage(storedFile.fileUrl());
                motariRepository.save(motari);
            }
        }

        return doc;
    }

    @Transactional
    public OnboardingStatusResponse completePassengerOnboarding(UUID userId, PassengerCompleteRequest req) {
        Passenger passenger = passengerRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Passenger profile not found.", HttpStatus.NOT_FOUND));

        boolean hasImage = documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE");
        if (!hasImage) {
            throw new ApiException("Profile image upload is required to complete onboarding.", HttpStatus.BAD_REQUEST);
        }

        passenger.setFirstName(req.getFirstName());
        passenger.setLastName(req.getLastName());
        passenger.setOnboardingStatus("COMPLETED");
        passengerRepository.save(passenger);

        User user = passenger.getUser();
        user.setName(req.getFirstName() + " " + req.getLastName());
        user.setStatus("ACTIVE");
        userRepository.save(user);

        log.info("Passenger onboarding completed for user: {}", userId);
        return getOnboardingStatus(userId);
    }

    @Transactional
    public OnboardingStatusResponse submitMotariOnboarding(UUID userId) {
        Motari motari = motariRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        OnboardingStatusResponse status = getOnboardingStatus(userId);
        if (status.getProfileCompletion() < 100) {
            throw new ApiException("Cannot submit onboarding. Missing requirements: " + status.getMissingFields(), HttpStatus.BAD_REQUEST);
        }

        // Generate verification request ticket
        VerificationRequest request = verificationRequestRepository.findByMotariIdAndStatus(userId, "PENDING")
                .orElseGet(() -> VerificationRequest.builder()
                        .motariId(userId)
                        .createdBy(userId)
                        .build());
        
        request.setStatus("PENDING");
        request.setRejectionReason(null); // Clear previous rejection
        request = verificationRequestRepository.save(request);

        motari.setVerificationStatus("PENDING");
        motari.setVerificationRequestId(request.getId());
        motariRepository.save(motari);

        User user = motari.getUser();
        user.setStatus("PENDING_VERIFICATION");
        userRepository.save(user);

        log.info("Motari onboarding submitted for verification. Request ID: {}", request.getId());
        return getOnboardingStatus(userId);
    }

    @Transactional
    public void approveVerification(UUID requestId, UUID adminId, AdminVerifyRequest req) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Verification request not found.", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(request.getStatus()) && !"UNDER_REVIEW".equals(request.getStatus())) {
            throw new ApiException("Request cannot be approved in its current state.", HttpStatus.BAD_REQUEST);
        }

        Motari motari = motariRepository.findById(request.getMotariId())
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        // Update Request
        request.setStatus("APPROVED");
        request.setVerifiedBy(adminId);
        request.setUpdatedBy(adminId);
        if (req.getAdminNotes() != null) {
            request.setAdminNotes(req.getAdminNotes());
        }
        verificationRequestRepository.save(request);

        // Update Motari Profile
        motari.setVerificationStatus("APPROVED");
        motari.setOnboardingStatus("COMPLETED");
        motariRepository.save(motari);

        // Update User Account
        User user = motari.getUser();
        user.setStatus("ACTIVE");
        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Admin [{}] approved verification request [{}] for motari [{}]", adminId, requestId, motari.getId());
        eventPublisher.publishEvent(new AccountApprovedEvent(this, motari.getId()));
    }

    @Transactional
    public void rejectVerification(UUID requestId, UUID adminId, AdminVerifyRequest req) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Verification request not found.", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(request.getStatus()) && !"UNDER_REVIEW".equals(request.getStatus())) {
            throw new ApiException("Request cannot be rejected in its current state.", HttpStatus.BAD_REQUEST);
        }

        if (req.getRejectionReason() == null || req.getRejectionReason().trim().isEmpty()) {
            throw new ApiException("Rejection reason is required.", HttpStatus.BAD_REQUEST);
        }

        Motari motari = motariRepository.findById(request.getMotariId())
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        // Update Request
        request.setStatus("REJECTED");
        request.setVerifiedBy(adminId);
        request.setUpdatedBy(adminId);
        request.setRejectionReason(req.getRejectionReason());
        if (req.getAdminNotes() != null) {
            request.setAdminNotes(req.getAdminNotes());
        }
        verificationRequestRepository.save(request);

        // Update Motari Profile (send back to onboarding IN_PROGRESS)
        motari.setVerificationStatus("REJECTED");
        motari.setOnboardingStatus("IN_PROGRESS");
        motariRepository.save(motari);

        // Update User Account
        User user = motari.getUser();
        user.setStatus("INACTIVE");
        userRepository.save(user);

        log.info("Admin [{}] rejected verification request [{}] for motari [{}] - Reason: {}", adminId, requestId, motari.getId(), req.getRejectionReason());
        eventPublisher.publishEvent(new AccountRejectedEvent(this, motari.getId(), req.getRejectionReason()));
    }

    @Transactional(readOnly = true)
    public List<VerificationRequest> getPendingVerifications() {
        return verificationRequestRepository.findByStatus("PENDING");
    }
}
