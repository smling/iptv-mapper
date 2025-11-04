package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Channel(
        @JacksonXmlProperty(isAttribute = true, localName = "id")
        String id,

        @JacksonXmlProperty(localName = "display-name")
        String displayName
) {}
