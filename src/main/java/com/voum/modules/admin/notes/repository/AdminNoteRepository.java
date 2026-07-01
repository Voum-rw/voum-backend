package com.voum.modules.admin.notes.repository;

import com.voum.modules.admin.notes.entity.AdminNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminNoteRepository extends JpaRepository<AdminNote, UUID> {
    List<AdminNote> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
