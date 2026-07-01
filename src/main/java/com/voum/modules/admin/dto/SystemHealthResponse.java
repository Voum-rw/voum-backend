package com.voum.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String database;
    private String redis;
    private String websocket;
    private String notifications;
    private String storage;
}
