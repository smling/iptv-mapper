package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.List;

public record PersonActor(
        @JacksonXmlProperty(isAttribute = true, localName = "role") String role,
        @JacksonXmlProperty(isAttribute = true, localName = "guest") String guest,
        @JacksonXmlText String name,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "image") List<Image> images,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "url") List<UrlRef> urls
) {}

