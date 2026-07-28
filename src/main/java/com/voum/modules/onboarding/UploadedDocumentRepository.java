package com.voum.modules.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {
    List<UploadedDocument> findByOwnerId(UUID ownerId);
    List<UploadedDocument> findBySessionId(UUID sessionId);
    Optional<UploadedDocument> findByOwnerIdAndDocumentType(UUID ownerId, String documentType);
    Optional<UploadedDocument> findTopByOwnerIdAndDocumentTypeOrderByVersionDesc(UUID ownerId, String documentType);
    boolean existsByOwnerIdAndDocumentType(UUID ownerId, String documentType);
    boolean existsByOwnerIdAndDocumentTypeAndStatus(UUID ownerId, String documentType, String status);
}
