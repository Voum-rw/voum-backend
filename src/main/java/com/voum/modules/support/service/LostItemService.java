package com.voum.modules.support.service;

import com.voum.common.ApiException;
import com.voum.modules.support.entity.LostItem;
import com.voum.modules.support.entity.LostItem.LostItemStatus;
import com.voum.modules.support.repository.LostItemRepository;
import com.voum.modules.support.events.LostItemCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LostItemService {

    private final LostItemRepository lostItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LostItem createReport(UUID reportedBy, UUID tripId, String itemName, String description) {
        LostItem item = LostItem.builder()
                .tripId(tripId)
                .reportedBy(reportedBy)
                .itemName(itemName)
                .description(description)
                .status(LostItemStatus.REPORTED)
                .build();

        item = lostItemRepository.save(item);

        eventPublisher.publishEvent(new LostItemCreatedEvent(item));

        return item;
    }

    @Transactional
    public void updateStatus(UUID itemId, LostItemStatus status) {
        LostItem item = lostItemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException("Lost item not found.", HttpStatus.NOT_FOUND));

        item.setStatus(status);
        lostItemRepository.save(item);
    }

    @Transactional
    public void resolveItem(UUID itemId) {
        updateStatus(itemId, LostItemStatus.RESOLVED);
    }

    @Transactional(readOnly = true)
    public List<LostItem> getMyReports(UUID reportedBy) {
        return lostItemRepository.findByReportedByOrderByCreatedAtDesc(reportedBy);
    }

    @Transactional(readOnly = true)
    public List<LostItem> getAllLostItems() {
        return lostItemRepository.findAll();
    }
}
