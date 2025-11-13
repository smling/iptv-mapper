package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record Credits(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "director") List<String> director,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "actor") List<PersonActor> actor,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "writer") List<String> writer,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "adapter") List<String> adapter,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "producer") List<String> producer,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "composer") List<String> composer,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "editor") List<String> editor,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "presenter") List<String> presenter,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "commentator") List<String> commentator,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "guest") List<String> guest
) {}

