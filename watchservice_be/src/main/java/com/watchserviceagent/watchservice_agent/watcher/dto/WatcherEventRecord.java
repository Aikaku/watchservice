package com.watchserviceagent.watchservice_agent.watcher.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class WatcherEventRecord {

    private final String ownerKey;
    private final String eventType;   // CREATE / MODIFY / DELETE
    private final String path;        // 절대 경로 문자열
    private final long eventTimeMs;   // epoch millis
}
