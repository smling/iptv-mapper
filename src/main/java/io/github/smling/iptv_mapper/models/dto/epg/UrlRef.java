package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record UrlRef(
        @JacksonXmlProperty(isAttribute = true, localName = "system")
        String system,

        @JacksonXmlText
        String value
) {}

