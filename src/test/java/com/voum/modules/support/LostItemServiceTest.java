package com.voum.modules.support;

import com.voum.modules.support.entity.LostItem;
import com.voum.modules.support.entity.LostItem.LostItemStatus;
import com.voum.modules.support.repository.LostItemRepository;
import com.voum.modules.support.service.LostItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LostItemServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LostItemService lostItemService;

    private final UUID reportedBy = UUID.randomUUID();
    private final UUID tripId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        lostItemService = new LostItemService(
                lostItemRepository,
                eventPublisher
        );
    }

    @Test
    public void createReport_shouldSaveLostItemAndPublishEvent() {
        LostItem savedItem = LostItem.builder()
                .id(itemId)
                .tripId(tripId)
                .reportedBy(reportedBy)
                .itemName("Leather wallet")
                .description("Black leather wallet with cards inside")
                .status(LostItemStatus.REPORTED)
                .build();

        when(lostItemRepository.save(any(LostItem.class))).thenReturn(savedItem);

        LostItem item = lostItemService.createReport(reportedBy, tripId, "Leather wallet", "Black leather wallet with cards inside");

        assertNotNull(item);
        assertEquals(itemId, item.getId());
        assertEquals(LostItemStatus.REPORTED, item.getStatus());
        verify(lostItemRepository).save(any(LostItem.class));
        verify(eventPublisher).publishEvent(any(com.voum.modules.support.events.LostItemCreatedEvent.class));
    }

    @Test
    public void updateStatus_shouldModifyStatusField() {
        LostItem item = LostItem.builder()
                .id(itemId)
                .status(LostItemStatus.REPORTED)
                .build();

        when(lostItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        lostItemService.updateStatus(itemId, LostItemStatus.FOUND);

        assertEquals(LostItemStatus.FOUND, item.getStatus());
        verify(lostItemRepository).save(item);
    }

    @Test
    public void resolveItem_shouldSetStatusToResolved() {
        LostItem item = LostItem.builder()
                .id(itemId)
                .status(LostItemStatus.REPORTED)
                .build();

        when(lostItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        lostItemService.resolveItem(itemId);

        assertEquals(LostItemStatus.RESOLVED, item.getStatus());
        verify(lostItemRepository).save(item);
    }
}
