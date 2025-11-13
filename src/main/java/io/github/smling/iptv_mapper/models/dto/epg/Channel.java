package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record Channel(
        @JacksonXmlProperty(isAttribute = true, localName = "id")
        String id,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "display-name")
        List<Text> displayNames,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "icon")
        List<Icon> icons,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "url")
        List<UrlRef> urls
) {
    public static Channel ofSingleName(String id, String displayName) {
        return new Channel(id,
                displayName == null ? java.util.List.of() : java.util.List.of(new Text(null, displayName)),
                java.util.List.of(),
                java.util.List.of());
    }
}
