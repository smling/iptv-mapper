package io.github.smling.iptv_mapper.models.dto.m3u;

import java.math.BigDecimal;
import java.util.UUID;

public interface M3UItemChannelView {
    UUID getMapId();              // muicm.id
    UUID getChannelId();          // c.id
    UUID getItemId();             // mui.id
    String getTitle();            // mui.title
    String getDisplayName();      // c.display_name
    String getUrl();              // mui.url
    String getTvIconUrl();
    String getTvId();
    String getUrlCheckerResult(); // mui.url_checker_result
    BigDecimal getConfidence();   // muicm.confidence (0..1)
    Boolean getManual();          // muicm.is_manual
}
