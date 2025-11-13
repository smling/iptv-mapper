package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record Image(
        @JacksonXmlProperty(isAttribute = true, localName = "type") String type,
        @JacksonXmlProperty(isAttribute = true, localName = "size") String size,
        @JacksonXmlProperty(isAttribute = true, localName = "orient") String orient,
        @JacksonXmlProperty(isAttribute = true, localName = "system") String system,
        @JacksonXmlText String value
) {}

