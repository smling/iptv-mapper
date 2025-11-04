package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Icon(
        @JacksonXmlProperty(isAttribute = true, localName = "src")
        String src
) {}