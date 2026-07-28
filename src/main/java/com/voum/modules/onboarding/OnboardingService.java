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

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;
    private final UploadedDocumentRepository documentRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationSessionRepository sessionRepository;
    private final MotariVerificationProfileRepository profileRepository;
    private final VerificationAuditLogRepository auditLogRepository;
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

            if (documentRepository.existsByOwnerIdAndDocumentType(userId, "PROFILE_IMAGE")) {
                completion += 25;
            } else {
                missingFields.add("profileImage");
            }

            boolean hasIdFront = documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_FRONT") || documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_CARD");
            boolean hasIdBack = documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_BACK") || documentRepository.existsByOwnerIdAndDocumentType(userId, "NATIONAL_ID_CARD");
            if (hasIdFront && hasIdBack) {
                completion += 25;
            } else {
                missingFields.add("nationalIdCard");
            }

            boolean hasInsFront = documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_POLICY_FRONT") || documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_POLICY") || documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_CERTIFICATE");
            boolean hasInsBack = documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_POLICY_BACK") || documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_POLICY") || documentRepository.existsByOwnerIdAndDocumentType(userId, "INSURANCE_CERTIFICATE");
            if (hasInsFront && hasInsBack) {
                completion += 25;
            } else {
                missingFields.add("insurancePolicy");
            }

            boolean hasPermitFront = documentRepository.existsByOwnerIdAndDocumentType(userId, "MOTORCYCLE_PERMIT_FRONT") || documentRepository.existsByOwnerIdAndDocumentType(userId, "DRIVING_PERMIT_FRONT") || documentRepository.existsByOwnerIdAndDocumentType(userId, "MOTORCYCLE_PERMIT");
            boolean hasPermitBack = documentRepository.existsByOwnerIdAndDocumentType(userId, "MOTORCYCLE_PERMIT_BACK") || documentRepository.existsByOwnerIdAndDocumentType(userId, "DRIVING_PERMIT_BACK") || documentRepository.existsByOwnerIdAndDocumentType(userId, "MOTORCYCLE_PERMIT");
            if (hasPermitFront && hasPermitBack) {
                completion += 25;
            } else {
                missingFields.add("motorcyclePermit");
            }

            if (completion >= 100) {
                if (!"APPROVED".equals(motari.getVerificationStatus())) {
                    motari.setOnboardingStatus("COMPLETED");
                    motari.setVerificationStatus("APPROVED");
                    motariRepository.save(motari);
                }
                verStatus = "APPROVED";
                completed = true;
            }
        }

        return OnboardingStatusResponse.builder()
                .completed(completed)
                .verificationStatus(verStatus)
                .profileCompletion(completion)
                .missingFields(missingFields)
                .build();
    }

    // ── VMS API: GET /api/v1/verification/me ─────────────────────────────────

    @Transactional
    public VerificationMeResponse getVerificationMeStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (user.getRole() != Role.MOTARI) {
            throw new ApiException("Verification portal is available for Motari drivers only.", HttpStatus.BAD_REQUEST);
        }

        Motari motari = motariRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        VerificationSession session = sessionRepository.findTopByMotariIdOrderByStartedAtDesc(userId)
                .orElseGet(() -> {
                    VerificationSession newSession = VerificationSession.builder()
                            .motariId(userId)
                            .status("NOT_STARTED")
                            .startedAt(Instant.now())
                            .build();
                    return sessionRepository.save(newSession);
                });

        MotariVerificationProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> {
                    MotariVerificationProfile newProfile = MotariVerificationProfile.builder()
                            .motariId(userId)
                            .verificationLevel("LEVEL_0")
                            .build();
                    return profileRepository.save(newProfile);
                });

        List<UploadedDocument> docs = documentRepository.findByOwnerId(userId);
        Map<String, UploadedDocument> docMap = new HashMap<>();
        for (UploadedDocument d : docs) {
            String key = d.getDocumentType();
            if (!docMap.containsKey(key) || (d.getVersion() != null && d.getVersion() > docMap.get(key).getVersion())) {
                docMap.put(key, d);
            }
        }

        // Build 3 category statuses: PERSONAL, MOTORCYCLE, DRIVING
        // 1. Personal
        UploadedDocument idFront = docMap.get("NATIONAL_ID_FRONT");
        if (idFront == null) idFront = docMap.get("NATIONAL_ID_CARD");
        UploadedDocument idBack = docMap.get("NATIONAL_ID_BACK");
        if (idBack == null) idBack = docMap.get("NATIONAL_ID_CARD");
        UploadedDocument profilePhoto = docMap.get("PROFILE_IMAGE");

        VerificationMeResponse.DocumentItemStatus nationalIdItem = buildItemStatus(
                "NATIONAL_ID", "National ID Card", true, idFront, idBack, null);

        VerificationMeResponse.DocumentItemStatus profilePhotoItem = buildItemStatus(
                "PROFILE_IMAGE", "Profile Photo (Mandatory Selfie)", true, null, null, profilePhoto);

        boolean personalComplete = isItemApprovedOrUploaded(nationalIdItem) && isItemApprovedOrUploaded(profilePhotoItem);
        VerificationMeResponse.CategoryStatus personalCat = VerificationMeResponse.CategoryStatus.builder()
                .category("PERSONAL")
                .title("Personal Identity")
                .completed(personalComplete)
                .items(List.of(nationalIdItem, profilePhotoItem))
                .build();

        // 2. Motorcycle
        UploadedDocument platePhoto = docMap.get("PLATE_PHOTO");
        UploadedDocument vehicleReg = docMap.get("VEHICLE_REGISTRATION");
        if (vehicleReg == null) vehicleReg = docMap.get("MOTORCYCLE_PERMIT_FRONT");
        UploadedDocument insuranceFront = docMap.get("INSURANCE_POLICY_FRONT");
        if (insuranceFront == null) insuranceFront = docMap.get("INSURANCE_POLICY");
        if (insuranceFront == null) insuranceFront = docMap.get("INSURANCE_CERTIFICATE");
        UploadedDocument insuranceBack = docMap.get("INSURANCE_POLICY_BACK");
        if (insuranceBack == null) insuranceBack = docMap.get("INSURANCE_POLICY");
        if (insuranceBack == null) insuranceBack = docMap.get("INSURANCE_CERTIFICATE");

        VerificationMeResponse.DocumentItemStatus plateItem = buildItemStatus(
                "PLATE_PHOTO", "Motorcycle Plate Photo", true, null, null, platePhoto);

        VerificationMeResponse.DocumentItemStatus vehicleRegItem = buildItemStatus(
                "VEHICLE_REGISTRATION", "Vehicle Registration (Yellow Card)", true, null, null, vehicleReg);

        VerificationMeResponse.DocumentItemStatus insuranceItem = buildItemStatus(
                "INSURANCE_POLICY", "Insurance Certificate", true, insuranceFront, insuranceBack, null);

        boolean motorcycleComplete = profile.getPlateNumber() != null && !profile.getPlateNumber().isEmpty()
                && isItemApprovedOrUploaded(plateItem) && isItemApprovedOrUploaded(vehicleRegItem) && isItemApprovedOrUploaded(insuranceItem);

        VerificationMeResponse.CategoryStatus motorcycleCat = VerificationMeResponse.CategoryStatus.builder()
                .category("MOTORCYCLE")
                .title("Motorcycle & Registration")
                .completed(motorcycleComplete)
                .items(List.of(plateItem, vehicleRegItem, insuranceItem))
                .build();

        // 3. Driving
        UploadedDocument permitFront = docMap.get("DRIVING_PERMIT_FRONT");
        if (permitFront == null) permitFront = docMap.get("MOTORCYCLE_PERMIT_FRONT");
        UploadedDocument permitBack = docMap.get("DRIVING_PERMIT_BACK");
        if (permitBack == null) permitBack = docMap.get("MOTORCYCLE_PERMIT_BACK");

        VerificationMeResponse.DocumentItemStatus drivingPermitItem = buildItemStatus(
                "DRIVING_PERMIT", "Permanent Driving License", true, permitFront, permitBack, null);

        boolean drivingComplete = isItemApprovedOrUploaded(drivingPermitItem);
        VerificationMeResponse.CategoryStatus drivingCat = VerificationMeResponse.CategoryStatus.builder()
                .category("DRIVING")
                .title("Driving License")
                .completed(drivingComplete)
                .items(List.of(drivingPermitItem))
                .build();

        // Calculate progress percentage
        int totalPoints = 0;
        if (isItemApprovedOrUploaded(nationalIdItem)) totalPoints += 25;
        if (isItemApprovedOrUploaded(profilePhotoItem)) totalPoints += 15;
        if (profile.getPlateNumber() != null && !profile.getPlateNumber().isEmpty() && isItemApprovedOrUploaded(plateItem)) totalPoints += 15;
        if (isItemApprovedOrUploaded(vehicleRegItem)) totalPoints += 15;
        if (isItemApprovedOrUploaded(insuranceItem)) totalPoints += 15;
        if (isItemApprovedOrUploaded(drivingPermitItem)) totalPoints += 15;
        int progress = Math.min(100, totalPoints);

        // Calculate session status and auto update
        String sessionStatus = session.getStatus();
        if ("NOT_STARTED".equals(sessionStatus) && progress > 0) {
            sessionStatus = "IN_PROGRESS";
            session.setStatus(sessionStatus);
            sessionRepository.save(session);
        }

        if ("IN_PROGRESS".equals(sessionStatus) && progress >= 100) {
            sessionStatus = "SUBMITTED";
            session.setStatus(sessionStatus);
            session.setSubmittedAt(Instant.now());
            sessionRepository.save(session);
        }

        boolean isApproved = "VERIFIED".equals(sessionStatus) || "APPROVED".equals(motari.getVerificationStatus()) || user.getIsVerified() == Boolean.TRUE;
        boolean canGoOnline = isApproved;

        String verLevel = profile.getVerificationLevel();
        if (isApproved) {
            verLevel = "LEVEL_3";
            profile.setVerificationLevel(verLevel);
            profileRepository.save(profile);
        }

        return VerificationMeResponse.builder()
                .sessionId(session.getId())
                .status(isApproved ? "VERIFIED" : sessionStatus)
                .verificationLevel(verLevel)
                .progress(isApproved ? 100 : progress)
                .canGoOnline(canGoOnline)
                .rejectionReason(session.getRejectionReason())
                .plateNumber(profile.getPlateNumber())
                .nationalIdNumber(profile.getNationalIdNumber())
                .permitNumber(profile.getPermitNumber())
                .permitExpiryDate(profile.getPermitExpiryDate())
                .insuranceExpiryDate(profile.getInsuranceExpiryDate())
                .categories(List.of(personalCat, motorcycleCat, drivingCat))
                .build();
    }

    private VerificationMeResponse.DocumentItemStatus buildItemStatus(
            String documentType, String title, boolean required,
            UploadedDocument frontDoc, UploadedDocument backDoc, UploadedDocument singleDoc) {

        VerificationMeResponse.SideStatus frontStatus = null;
        if (frontDoc != null) {
            frontStatus = VerificationMeResponse.SideStatus.builder()
                    .uploaded(true)
                    .documentId(frontDoc.getId().toString())
                    .status(frontDoc.getStatus() != null ? frontDoc.getStatus() : "UPLOADED")
                    .rejectionReason(frontDoc.getRejectionReason())
                    .expiryDate(frontDoc.getExpiryDate())
                    .build();
        }

        VerificationMeResponse.SideStatus backStatus = null;
        if (backDoc != null) {
            backStatus = VerificationMeResponse.SideStatus.builder()
                    .uploaded(true)
                    .documentId(backDoc.getId().toString())
                    .status(backDoc.getStatus() != null ? backDoc.getStatus() : "UPLOADED")
                    .rejectionReason(backDoc.getRejectionReason())
                    .expiryDate(backDoc.getExpiryDate())
                    .build();
        }

        VerificationMeResponse.SideStatus singleStatus = null;
        if (singleDoc != null) {
            singleStatus = VerificationMeResponse.SideStatus.builder()
                    .uploaded(true)
                    .documentId(singleDoc.getId().toString())
                    .status(singleDoc.getStatus() != null ? singleDoc.getStatus() : "UPLOADED")
                    .rejectionReason(singleDoc.getRejectionReason())
                    .expiryDate(singleDoc.getExpiryDate())
                    .build();
        }

        String itemStatus = "NOT_STARTED";
        String rejection = null;

        if (singleDoc != null) {
            itemStatus = singleDoc.getStatus() != null ? singleDoc.getStatus() : "UPLOADED";
            rejection = singleDoc.getRejectionReason();
        } else if (frontDoc != null || backDoc != null) {
            if (frontDoc != null && backDoc != null) {
                if ("NEEDS_CORRECTION".equals(frontDoc.getStatus()) || "NEEDS_CORRECTION".equals(backDoc.getStatus())) {
                    itemStatus = "NEEDS_CORRECTION";
                    rejection = frontDoc.getRejectionReason() != null ? frontDoc.getRejectionReason() : backDoc.getRejectionReason();
                } else if ("REJECTED".equals(frontDoc.getStatus()) || "REJECTED".equals(backDoc.getStatus())) {
                    itemStatus = "REJECTED";
                    rejection = frontDoc.getRejectionReason() != null ? frontDoc.getRejectionReason() : backDoc.getRejectionReason();
                } else if ("APPROVED".equals(frontDoc.getStatus()) && "APPROVED".equals(backDoc.getStatus())) {
                    itemStatus = "APPROVED";
                } else {
                    itemStatus = "UPLOADED";
                }
            } else {
                itemStatus = "PARTIALLY_UPLOADED";
            }
        }

        return VerificationMeResponse.DocumentItemStatus.builder()
                .documentType(documentType)
                .title(title)
                .required(required)
                .front(frontStatus)
                .back(backStatus)
                .single(singleStatus)
                .itemStatus(itemStatus)
                .rejectionReason(rejection)
                .build();
    }

    private boolean isItemApprovedOrUploaded(VerificationMeResponse.DocumentItemStatus item) {
        if (item == null) return false;
        if (item.getSingle() != null && item.getSingle().isUploaded()) return true;
        return (item.getFront() != null && item.getFront().isUploaded()) && (item.getBack() != null && item.getBack().isUploaded());
    }

    // ── VMS API: Document Upload ─────────────────────────────────────────────

    @Transactional
    public UploadedDocument uploadVmsDocument(UUID userId, String docType, String category, String side, byte[] content, String originalFilename, String contentType, LocalDate expiryDate) {
        log.info("Processing VMS document upload of type '{}', category '{}', side '{}' for user: {}", docType, category, side, userId);

        VerificationSession session = sessionRepository.findTopByMotariIdOrderByStartedAtDesc(userId)
                .orElseGet(() -> sessionRepository.save(VerificationSession.builder()
                        .motariId(userId)
                        .status("IN_PROGRESS")
                        .startedAt(Instant.now())
                        .build()));

        // Determine category if omitted
        String cat = category;
        if (cat == null || cat.isEmpty()) {
            if (docType.startsWith("NATIONAL") || docType.startsWith("PROFILE")) cat = "PERSONAL";
            else if (docType.startsWith("PLATE") || docType.startsWith("VEHICLE") || docType.startsWith("INSURANCE")) cat = "MOTORCYCLE";
            else if (docType.startsWith("DRIVING") || docType.startsWith("MOTO")) cat = "DRIVING";
            else cat = "PERSONAL";
        }

        // Calculate versioning
        Optional<UploadedDocument> existingOpt = documentRepository.findTopByOwnerIdAndDocumentTypeOrderByVersionDesc(userId, docType);
        int nextVersion = existingOpt.map(d -> (d.getVersion() != null ? d.getVersion() + 1 : 2)).orElse(1);

        String ext = getFileExtension(originalFilename);
        String storageKey = String.format("motaris/%s/verification/sessions/%s/%s/%s_v%d%s",
                userId.toString(), session.getId().toString(), cat.toLowerCase(), docType.toLowerCase(), nextVersion, ext);

        String checksum = computeSha256(content);
        StoredFile storedFile = storageService.uploadFile(storageKey, originalFilename, content, contentType);

        UploadedDocument doc = UploadedDocument.builder()
                .sessionId(session.getId())
                .ownerId(userId)
                .category(cat.toUpperCase())
                .documentType(docType.toUpperCase())
                .side(side != null ? side.toUpperCase() : "SINGLE")
                .fileUrl(storedFile.fileUrl())
                .storageKey(storageKey)
                .fileName(originalFilename)
                .contentType(contentType)
                .fileSize((long) content.length)
                .fileChecksum(checksum)
                .version(nextVersion)
                .status("UPLOADED")
                .expiryDate(expiryDate)
                .build();

        doc = documentRepository.save(doc);

        // Update profile photo link if profile image
        if ("PROFILE_IMAGE".equalsIgnoreCase(docType)) {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.getRole() == Role.PASSENGER) {
                    passengerRepository.findById(userId).ifPresent(p -> {
                        p.setProfileImage(storedFile.fileUrl());
                        passengerRepository.save(p);
                    });
                } else if (u.getRole() == Role.MOTARI) {
                    motariRepository.findById(userId).ifPresent(m -> {
                        m.setProfileImage(storedFile.fileUrl());
                        motariRepository.save(m);
                    });
                }
            });
        }

        // Audit Log
        auditLogRepository.save(VerificationAuditLog.builder()
                .sessionId(session.getId())
                .documentId(doc.getId())
                .action("DOCUMENT_UPLOADED")
                .performedBy(userId)
                .metadataJson("{\"docType\":\"" + docType + "\",\"version\":" + nextVersion + "}")
                .build());

        // Update Session Status
        if ("NOT_STARTED".equals(session.getStatus())) {
            session.setStatus("IN_PROGRESS");
            sessionRepository.save(session);
        }

        return doc;
    }

    @Transactional
    public UploadedDocument uploadDocument(UUID userId, String docType, byte[] content, String originalFilename, String contentType) {
        return uploadVmsDocument(userId, docType, null, null, content, originalFilename, contentType, null);
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
    public MotariVerificationProfile saveMotariProfileData(UUID userId, String plateNumber, String nationalIdNumber, String permitNumber, LocalDate permitExpiryDate, LocalDate insuranceExpiryDate) {
        MotariVerificationProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> MotariVerificationProfile.builder().motariId(userId).verificationLevel("LEVEL_0").build());

        if (plateNumber != null && !plateNumber.trim().isEmpty()) {
            profile.setPlateNumber(plateNumber.trim().toUpperCase());
            motariRepository.findById(userId).ifPresent(m -> {
                m.setMotoPlateNumber(plateNumber.trim().toUpperCase());
                motariRepository.save(m);
            });
        }

        if (nationalIdNumber != null) profile.setNationalIdNumber(nationalIdNumber.trim());
        if (permitNumber != null) profile.setPermitNumber(permitNumber.trim());
        if (permitExpiryDate != null) profile.setPermitExpiryDate(permitExpiryDate);
        if (insuranceExpiryDate != null) profile.setInsuranceExpiryDate(insuranceExpiryDate);

        profile = profileRepository.save(profile);
        log.info("Updated verification profile data for motari: {}", userId);
        return profile;
    }

    @Transactional
    public OnboardingStatusResponse submitMotariOnboarding(UUID userId) {
        Motari motari = motariRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        VerificationSession session = sessionRepository.findTopByMotariIdOrderByStartedAtDesc(userId)
                .orElseGet(() -> sessionRepository.save(VerificationSession.builder()
                        .motariId(userId)
                        .status("SUBMITTED")
                        .startedAt(Instant.now())
                        .build()));

        session.setStatus("SUBMITTED");
        session.setSubmittedAt(Instant.now());
        session.setRejectionReason(null);
        sessionRepository.save(session);

        motari.setVerificationStatus("UNDER_REVIEW");
        motariRepository.save(motari);

        User user = motari.getUser();
        user.setStatus("PENDING_VERIFICATION");
        userRepository.save(user);

        auditLogRepository.save(VerificationAuditLog.builder()
                .sessionId(session.getId())
                .action("STATUS_CHANGED")
                .performedBy(userId)
                .metadataJson("{\"status\":\"SUBMITTED\"}")
                .build());

        return getOnboardingStatus(userId);
    }

    @Transactional
    public void reviewDocument(UUID documentId, UUID adminId, AdminDocumentReviewRequest req) {
        UploadedDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException("Document not found.", HttpStatus.NOT_FOUND));

        String action = req.getAction() != null ? req.getAction().toUpperCase() : "APPROVE";
        if ("APPROVE".equals(action)) {
            doc.setStatus("APPROVED");
            doc.setRejectionReason(null);
        } else if ("REQUEST_CORRECTION".equals(action)) {
            doc.setStatus("NEEDS_CORRECTION");
            doc.setRejectionReason(req.getRejectionReason());
            doc.setAdminNotes(req.getAdminNotes());
        } else if ("REJECT".equals(action)) {
            doc.setStatus("REJECTED");
            doc.setRejectionReason(req.getRejectionReason());
            doc.setAdminNotes(req.getAdminNotes());
        }
        documentRepository.save(doc);

        auditLogRepository.save(VerificationAuditLog.builder()
                .sessionId(doc.getSessionId())
                .documentId(doc.getId())
                .action("DOCUMENT_" + action)
                .performedBy(adminId)
                .metadataJson("{\"reason\":\"" + req.getRejectionReason() + "\",\"notes\":\"" + req.getAdminNotes() + "\"}")
                .build());
    }

    @Transactional
    public void approveVerification(UUID requestId, UUID adminId, AdminVerifyRequest req) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Verification request not found.", HttpStatus.NOT_FOUND));

        Motari motari = motariRepository.findById(request.getMotariId())
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        request.setStatus("APPROVED");
        request.setVerifiedBy(adminId);
        verificationRequestRepository.save(request);

        sessionRepository.findTopByMotariIdOrderByStartedAtDesc(motari.getId()).ifPresent(s -> {
            s.setStatus("VERIFIED");
            s.setVerifiedBy(adminId);
            s.setReviewedAt(Instant.now());
            sessionRepository.save(s);
        });

        profileRepository.findById(motari.getId()).ifPresent(p -> {
            p.setVerificationLevel("LEVEL_3");
            profileRepository.save(p);
        });

        motari.setVerificationStatus("APPROVED");
        motari.setOnboardingStatus("COMPLETED");
        motariRepository.save(motari);

        User user = motari.getUser();
        user.setStatus("ACTIVE");
        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Admin [{}] approved verification for motari [{}]", adminId, motari.getId());
        eventPublisher.publishEvent(new AccountApprovedEvent(this, motari.getId()));
    }

    @Transactional
    public void rejectVerification(UUID requestId, UUID adminId, AdminVerifyRequest req) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException("Verification request not found.", HttpStatus.NOT_FOUND));

        Motari motari = motariRepository.findById(request.getMotariId())
                .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

        request.setStatus("REJECTED");
        request.setVerifiedBy(adminId);
        request.setRejectionReason(req.getRejectionReason());
        verificationRequestRepository.save(request);

        sessionRepository.findTopByMotariIdOrderByStartedAtDesc(motari.getId()).ifPresent(s -> {
            s.setStatus("REJECTED");
            s.setRejectionReason(req.getRejectionReason());
            s.setVerifiedBy(adminId);
            s.setReviewedAt(Instant.now());
            sessionRepository.save(s);
        });

        motari.setVerificationStatus("REJECTED");
        motari.setOnboardingStatus("IN_PROGRESS");
        motariRepository.save(motari);

        User user = motari.getUser();
        user.setStatus("INACTIVE");
        userRepository.save(user);

        log.info("Admin [{}] rejected verification for motari [{}]", adminId, motari.getId());
        eventPublisher.publishEvent(new AccountRejectedEvent(this, motari.getId(), req.getRejectionReason()));
    }

    @Transactional(readOnly = true)
    public List<VerificationRequest> getPendingVerifications() {
        return verificationRequestRepository.findByStatus("PENDING");
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}
