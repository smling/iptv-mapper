package io.github.smling.iptv_mapper.models.dto.epg;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record EpisodeNumber(
        @JacksonXmlProperty(isAttribute = true, localName = "system")
        String system,

        // The inner text node of <episode-num>...</episode-num>
        @JacksonXmlProperty(localName = "")
        @JacksonXmlText
        String value
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EpisodeNumber {}
}
