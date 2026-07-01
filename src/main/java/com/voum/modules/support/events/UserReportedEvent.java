package com.voum.modules.support.events;

import com.voum.modules.support.entity.UserReport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserReportedEvent {
    private final UserReport report;
}
