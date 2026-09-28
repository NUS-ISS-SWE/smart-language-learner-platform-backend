package com.nus.iss.dto;

import java.util.List;

public record ModerationQueueResponse(
        List<ModerationQueueItem> items,
        long total,
        int page,
        int pageSize,
        boolean hasMore) {
}
