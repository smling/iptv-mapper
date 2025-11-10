package io.github.smling.iptv_mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.iptv_mapper.models.dto.epg.Tv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "ENABLE_NETWORK_TESTS", matches = "(?i)true|1|yes")
class EPGClientTest {
    private static final URI XML_URL = URI.create("https://www.open-epg.com/files/hongkong1.xml");
    private static final URI GZ_URL  = URI.create("https://www.open-epg.com/files/hongkong1.xml.gz");

    private final EPGClient client = new EPGClient();
    private final ObjectMapper json = new ObjectMapper();

    static Stream<URI> urlProvider() {
        return Stream.of(XML_URL, GZ_URL);
    }

    @ParameterizedTest(name = "fetchEpg should deserialize from: {0}")
    @MethodSource("urlProvider")
    @DisplayName("EPGClient.fetchEpg: plain XML and .gz both deserialize")
    void fetchEpg_deserializes_forXmlAndGzip(URI url) throws Exception {
        Tv tv = client.fetchEpg(url);

        assertNotNull(tv, "Deserialized Tv should not be null");

        // If Tv has obvious getters (e.g., channels/programmes), add light sanity checks:
        // Example (commented — uncomment if these methods exist on your Tv type):
         assertFalse(tv.channels().isEmpty(), "Channels should not be empty");
         assertFalse(tv.programmes().isEmpty(), "Programmes should not be empty");
    }

    @Test
    @DisplayName("EPGClient.fetchEpg: XML and GZ yield equivalent content")
    void fetchEpg_xmlAndGz_yieldSameData() throws Exception {
        Tv tvXml = client.fetchEpg(XML_URL);
        Tv tvGz  = client.fetchEpg(GZ_URL);

        assertNotNull(tvXml, "XML result should not be null");
        assertNotNull(tvGz,  "GZ result should not be null");

        // Compare via canonical JSON trees to avoid relying on Tv.equals()
        JsonNode treeXml = json.valueToTree(tvXml);
        JsonNode treeGz  = json.valueToTree(tvGz);

        assertEquals(treeXml, treeGz, "Parsed results from XML and GZ should be equivalent");
    }
}
