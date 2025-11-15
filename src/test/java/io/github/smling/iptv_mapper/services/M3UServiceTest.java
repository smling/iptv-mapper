package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.models.dto.m3u.M3UItemChannelView;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylistLineView;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import io.github.smling.iptv_mapper.repositories.M3UItemChannelMapRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UItemRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UPlaylistRepository;
import io.github.smling.iptv_mapper.services.m3u.M3UService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class M3UServiceTest {

    @Test
    void generateAll_usesChannelIdAsTvgId() {
        // Arrange
        DataSourceRepository dataSourceRepo = mock(DataSourceRepository.class);
        M3UPlaylistRepository playlistRepo = mock(M3UPlaylistRepository.class);
        M3UItemRepository itemRepo = mock(M3UItemRepository.class);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        M3UItemChannelMapRepository mapRepository = mock(M3UItemChannelMapRepository.class);
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        org.springframework.context.ApplicationContext applicationContext = mock(org.springframework.context.ApplicationContext.class);

        M3UService service = new M3UService(
                dataSourceRepo,
                playlistRepo,
                itemRepo,
                channelRepository,
                clock,
                mapRepository,
                applicationContext
        );

        String channelId = "my-channel-id";

        M3UPlaylistLineView row = new M3UPlaylistLineView() {
            @Override
            public String getTvgChno() {
                return "1000";
            }

            @Override
            public String getChannelName() {
                return "Test Channel";
            }

            @Override
            public String getTvgName() {
                return "Test Channel";
            }

            @Override
            public String getTvgId() {
                return channelId;
            }

            @Override
            public String getTvgLogo() {
                return null;
            }

            @Override
            public String getStreamUrl() {
                return "http://example.com/stream";
            }
        };

        when(itemRepo.findPlaylistLinesAll()).thenReturn(List.of(row));
        when(mapRepository.findAllProjected(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        String m3u = service.generateAll();

        // Assert: EXTINF line should contain tvg-id equal to channelId
        assertTrue(m3u.contains("tvg-id=\"" + channelId + "\""), "M3U tvg-id should use channel.channel_id");
    }
}

