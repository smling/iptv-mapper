package io.github.smling.iptv_mapper.models.dto.epg;

import java.time.Instant;

public interface ProgrammeLineView {
    String getChannelXmltvId();       // mapping id (same as above)

    Instant getStartUtc();     // programme start (UTC)

    Instant getStopUtc();      // programme stop  (UTC)

    String getTitle();                // nullable ok

    String getDesc();                 // nullable ok
}
