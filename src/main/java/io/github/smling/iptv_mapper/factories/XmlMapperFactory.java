package io.github.smling.iptv_mapper.factories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public final class XmlMapperFactory {
    public static XmlMapper ofEPG() {
        XmlMapper mapper = (XmlMapper) new XmlMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                ;
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}