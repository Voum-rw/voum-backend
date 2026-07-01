package com.voum.modules.support.events;

import com.voum.modules.support.entity.LostItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LostItemCreatedEvent {
    private final LostItem lostItem;
}
