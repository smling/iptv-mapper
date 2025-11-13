package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record ProgrammeLength(
        @JacksonXmlProperty(isAttribute = true, localName = "units")
        String units,

        @JacksonXmlText
        String value
) {}

