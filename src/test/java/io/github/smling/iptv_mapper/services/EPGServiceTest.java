package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dao.epg.ProgrammeEntity;
import io.github.smling.iptv_mapper.models.dto.epg.ChannelEpgRow;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelDisplayNameRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelUrlRepository;
import io.github.smling.iptv_mapper.repositories.epg.ProgrammeRepository;
import io.github.smling.iptv_mapper.services.epg.EPGService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EPGServiceTest {

    @Test
    void generateTo_usesChannelIdAsXmltvIdAndProgrammeChannel() {
        // Arrange
        DataSourceRepository dataSourceRepo = mock(DataSourceRepository.class);
        Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), ZoneOffset.UTC);
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        ProgrammeRepository programmeRepository = mock(ProgrammeRepository.class);
        ChannelDisplayNameRepository channelDisplayNameRepository = mock(ChannelDisplayNameRepository.class);
        ChannelUrlRepository channelUrlRepository = mock(ChannelUrlRepository.class);
        org.springframework.core.env.Environment environment = mock(org.springframework.core.env.Environment.class);
        org.springframework.context.ApplicationContext applicationContext = mock(org.springframework.context.ApplicationContext.class);

        EPGService service = new EPGService(
                dataSourceRepo,
                clock,
                mock(io.github.smling.iptv_mapper.repositories.epg.TvRepository.class),
                channelRepository,
                programmeRepository,
                channelDisplayNameRepository,
                channelUrlRepository,
                "iptv-mapper-test",
                environment,
                applicationContext
        );

        String channelId = "my-channel-id";
        UUID channelDbId = UUID.randomUUID();

        ChannelEpgRow row = new ChannelEpgRow() {
            @Override
            public UUID getChannelDbId() {
                return channelDbId;
            }

            @Override
            public String getXmltvId() {
                return channelId;
            }

            @Override
            public String getDisplayName() {
                return "Test Channel";
            }
        };

        when(channelRepository.findAllChannelsForEpg()).thenReturn(List.of(row));
        when(channelDisplayNameRepository.findByChannel_IdIn(List.of(channelDbId))).thenReturn(Collections.emptyList());
        when(channelUrlRepository.findByChannel_IdIn(List.of(channelDbId))).thenReturn(Collections.emptyList());

        ChannelEntity ch = new ChannelEntity()
                .setId(channelDbId)
                .setChannelId(channelId)
                .setDisplayName("Test Channel");
        ProgrammeEntity prog = new ProgrammeEntity()
                .setChannel(ch)
                .setStartTime(OffsetDateTime.now())
                .setStopTime(OffsetDateTime.now().plusHours(1))
                .setTitle("Some title")
                .setDescription("Some desc");
        when(programmeRepository.findEntitiesBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prog));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Act
        service.generateTo(out, null, null);
        String xml = out.toString();

        // Assert: channel element id and programme channel attribute both use channelId
        assertTrue(xml.contains("channel id=\"" + channelId + "\""), "EPG channel id should use channel.channel_id");
        assertTrue(xml.contains("channel=\"" + channelId + "\""), "EPG programme channel should use channel.channel_id");
    }
}
