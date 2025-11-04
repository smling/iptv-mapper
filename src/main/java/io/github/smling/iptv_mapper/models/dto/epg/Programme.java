package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Programme(
        @JsonProperty("start")
        @JacksonXmlProperty(isAttribute = true, localName = "start")
        String start,

        @JsonProperty("stop")
        @JacksonXmlProperty(isAttribute = true, localName = "stop")
        String stop,

        @JsonProperty("channel")
        @JacksonXmlProperty(isAttribute = true, localName = "channel")
        String channel,

        @JsonProperty("title")
        @JacksonXmlProperty(localName = "title")
        String title,

        @JsonProperty("desc")
        @JacksonXmlProperty(localName = "desc")
        String desc

//        @JsonProperty("episode-num")
//        @JacksonXmlProperty(localName = "episode-num")
//        EpisodeNumber episodeNum
) {}