package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Video(
        @JacksonXmlProperty(localName = "present") String present,
        @JacksonXmlProperty(localName = "colour") String colour,
        @JacksonXmlProperty(localName = "aspect") String aspect,
        @JacksonXmlProperty(localName = "quality") String quality
) {}

