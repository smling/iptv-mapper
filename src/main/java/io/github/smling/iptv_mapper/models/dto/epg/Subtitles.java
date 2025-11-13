package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record Subtitles(
        @JacksonXmlProperty(isAttribute = true, localName = "type") String type,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "language") List<Text> language
) {}

