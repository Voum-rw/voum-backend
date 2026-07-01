package com.voum.modules.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voum.modules.audit.entity.AuditLog;
import com.voum.modules.audit.repository.AuditLogRepository;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logAction(UUID actorId, String action, String target, String targetType, Map<String, Object> metadata) {
        String actorPhone = "SYSTEM";
        if (actorId != null) {
            User user = userRepository.findById(actorId).orElse(null);
            if (user != null) {
                actorPhone = user.getPhone();
            }
        }

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                log.warn("Failed to serialize audit log metadata to JSON: {}", e.getMessage());
            }
        }

        AuditLog logEntry = AuditLog.builder()
                .actorId(actorId)
                .actorPhone(actorPhone)
                .action(action)
                .target(target)
                .targetType(targetType)
                .metadata(metadataJson)
                .build();

        auditLogRepository.save(logEntry);
        log.info("Audit log recorded: actorPhone={}, action={}, target={}, targetType={}",
                actorPhone, action, target, targetType);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAll(pageable);
    }
}
