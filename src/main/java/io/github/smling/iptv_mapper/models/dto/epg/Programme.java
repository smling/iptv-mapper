package io.github.smling.iptv_mapper.models.dto.epg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;
import java.util.Map;

// forward references used by static factory
import io.github.smling.iptv_mapper.models.dao.epg.ProgrammeEntity;
import io.github.smling.iptv_mapper.parsers.EPGTimeParser;

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

        // optional attrs
        @JacksonXmlProperty(isAttribute = true, localName = "pdc-start")
        String pdcStart,
        @JacksonXmlProperty(isAttribute = true, localName = "vps-start")
        String vpsStart,
        @JacksonXmlProperty(isAttribute = true, localName = "showview")
        String showview,
        @JacksonXmlProperty(isAttribute = true, localName = "videoplus")
        String videoplus,
        @JacksonXmlProperty(isAttribute = true, localName = "clumpidx")
        String clumpidx,

        // multi-lingual elements
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "title")
        List<Text> titles,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "sub-title")
        List<Text> subTitles,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "desc")
        List<Text> descs,

        @JacksonXmlProperty(localName = "credits")
        Credits credits,

        @JacksonXmlProperty(localName = "date")
        String date,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "category")
        List<Text> categories,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "keyword")
        List<Text> keywords,

        // language/orig-language
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "language")
        List<Text> languages,

        @JacksonXmlProperty(localName = "orig-language")
        Text origLanguage,

        @JacksonXmlProperty(localName = "length")
        ProgrammeLength length,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "icon")
        List<Icon> icons,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "url")
        List<UrlRef> urls,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "country")
        List<String> countries,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "episode-num")
        List<EpisodeNumber> episodeNums,

        @JacksonXmlProperty(localName = "video")
        Video video,
        @JacksonXmlProperty(localName = "audio")
        Audio audio,

        @JacksonXmlProperty(localName = "previously-shown")
        PreviouslyShown previouslyShown,

        @JacksonXmlProperty(localName = "premiere")
        Text premiere,
        @JacksonXmlProperty(localName = "last-chance")
        Text lastChance,

        @JsonProperty("new")
        @JacksonXmlProperty(localName = "new")
        Empty isNew,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "subtitles")
        List<Subtitles> subtitles,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "rating")
        List<Rating> ratings,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "star-rating")
        List<StarRating> starRatings,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "review")
        List<Review> reviews,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "image")
        List<Image> images,

        // New: flexible metadata to align with DAO JSONB
        Map<String, Object> meta
) {
    // Helpers to keep DTO and DAO in sync
    public static Programme fromEntity(ProgrammeEntity e) {
        if (e == null) return null;
        String start = EPGTimeParser.toIsoInstantString(e.getStartTime().toInstant());
        String stop  = EPGTimeParser.toIsoInstantString(e.getStopTime().toInstant());
        String channelId = (e.getChannel() != null ? e.getChannel().getChannelId() : null);
        String title = e.getTitle();
        String desc  = e.getDescription();
        return new Programme(
                start,
                stop,
                channelId,
                null, null, null, null, null,
                title == null ? List.<Text>of() : List.of(new Text(null, title)),
                List.<Text>of(),
                desc == null ? List.<Text>of() : List.of(new Text(null, desc)),
                null,
                null,
                List.<Text>of(),
                List.<Text>of(),
                List.<Text>of(),
                null,
                null,
                List.<Icon>of(),
                List.<UrlRef>of(),
                List.<String>of(),
                List.<EpisodeNumber>of(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.<Subtitles>of(),
                List.<Rating>of(),
                List.<StarRating>of(),
                List.<Review>of(),
                List.<Image>of(),
                e.getMeta()
        );
    }
}
