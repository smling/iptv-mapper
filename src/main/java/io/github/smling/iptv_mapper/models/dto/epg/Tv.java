package io.github.smling.iptv_mapper.models.dto.epg;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "tv")
public record Tv(
        @JacksonXmlProperty(isAttribute = true, localName = "generator-info-name")
        String generatorInfoName,

        @JacksonXmlProperty(isAttribute = true, localName = "generator-info-url")
        String generatorInfoUrl,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "channel")
        List<Channel> channels,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "programme")
        List<Programme> programmes
) {}
