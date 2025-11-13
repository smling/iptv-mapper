package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record Review(
        @JacksonXmlProperty(isAttribute = true, localName = "type") String type,
        @JacksonXmlProperty(isAttribute = true, localName = "source") String source,
        @JacksonXmlProperty(isAttribute = true, localName = "reviewer") String reviewer,
        @JacksonXmlProperty(isAttribute = true, localName = "lang") String lang,
        @JacksonXmlText String value
) {}

