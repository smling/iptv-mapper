package io.github.smling.iptv_mapper.models.dto.m3u;

import java.util.UUID;

public record ManualMappingRequest(
        UUID itemId,
        UUID channelId,
        String note
) {
}

