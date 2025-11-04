package io.github.smling.iptv_mapper.models.dto.m3u;

public interface M3UPlaylistLineView {
    String getTvgChno();
    String getChannelName();   // channel.display_name (fallback to channelId)
    String getTvgName();       // channel.tvg_name (fallbacks handled in SQL)
    String getTvgId();         // a stable id (use channelId or DB UUID-as-text)
    String getTvgLogo();       // best icon URL if available
    String getStreamUrl();     // m3u_item.url
}

