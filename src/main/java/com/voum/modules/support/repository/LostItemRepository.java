package com.voum.modules.support.repository;

import com.voum.modules.support.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LostItemRepository extends JpaRepository<LostItem, UUID> {
    List<LostItem> findByReportedByOrderByCreatedAtDesc(UUID reportedBy);
    List<LostItem> findByTripId(UUID tripId);
    long countByStatus(LostItem.LostItemStatus status);
}
