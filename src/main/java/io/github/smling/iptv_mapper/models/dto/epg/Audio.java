package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Audio(
        @JacksonXmlProperty(localName = "present") String present,
        @JacksonXmlProperty(localName = "stereo") String stereo
) {}

