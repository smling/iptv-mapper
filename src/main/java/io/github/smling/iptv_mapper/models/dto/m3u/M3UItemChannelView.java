package io.github.smling.iptv_mapper.models.dto.m3u;

import java.util.UUID;

public interface M3UItemChannelView {
    UUID getMapId();          // muicm.id
    UUID getChannelId();    // c.channel_id  <-- NEW
    String getTitle();        // mui.title
    String getDisplayName();  // c.display_name
    String getUrl();          // mui.url
    String getTvIconUrl();
    String getTvId();
}
