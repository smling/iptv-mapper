package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record PreviouslyShown(
        @JacksonXmlProperty(isAttribute = true, localName = "start") String start,
        @JacksonXmlProperty(isAttribute = true, localName = "channel") String channel
) {}

