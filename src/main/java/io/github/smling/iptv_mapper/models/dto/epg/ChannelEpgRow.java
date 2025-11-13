package io.github.smling.iptv_mapper.models.dto.epg;

import java.util.UUID;

public interface ChannelEpgRow {
    UUID getChannelDbId();
    String getXmltvId();
    String getDisplayName();
}

