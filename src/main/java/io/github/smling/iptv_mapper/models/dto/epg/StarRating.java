package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record StarRating(
        @JacksonXmlProperty(isAttribute = true, localName = "system") String system,
        @JacksonXmlProperty(localName = "value") String value,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "icon") List<Icon> icons
) {}

