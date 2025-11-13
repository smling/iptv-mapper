package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record Text(
        @JacksonXmlProperty(isAttribute = true, localName = "lang")
        String lang,

        @JacksonXmlText
        String value
) {}

