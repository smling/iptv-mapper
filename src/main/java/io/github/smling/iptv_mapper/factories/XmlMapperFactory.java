package io.github.smling.iptv_mapper.factories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public final class XmlMapperFactory {
    public static XmlMapper ofEPG() {
        XmlMapper mapper = (XmlMapper) new XmlMapper()
                // Keep nulls out, but allow empty strings so required XMLTV
                // elements like <title> and <display-name> still serialize
                // even when values are empty.
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                ;
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
