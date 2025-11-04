package io.github.smling.iptv_mapper.models.dto.epg;

public interface ChannelLineView {
    String getXmltvId();      // use mapping id (m3u_item_channel_map.id) as the XMLTV channel id
    String getDisplayName();  // channel.display_name
}

