package io.github.smling.iptv_mapper.models.dto.epg;

import java.time.Instant;
import java.util.UUID;

public interface ChannelProgrammeView {
    UUID getChannelDbId();   // c.id
    String getChannelId();     // c.channel_id (your business/channel code)
    UUID   getProgrammeId();   // p.id
    Instant getStartTime();    // p.start_time
    Instant getStopTime();     // p.stop_time
    String getTitle();         // p.title
    String getDescription();   // p.description
}

