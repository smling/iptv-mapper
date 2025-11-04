package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.models.dto.epg.ChannelProgrammeView;
import io.github.smling.iptv_mapper.services.epg.ChannelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/epg/channel")
public class ChannelController {

    private final ChannelService service;

    public ChannelController(ChannelService service) {
        this.service = service;
    }

    @GetMapping("/{channelDbId}/programmes")
    public Page<ChannelProgrammeView> getProgrammesByChannelDbId(
            @PathVariable UUID channelDbId,
            @PageableDefault(size = 100, sort = "startTime") Pageable pageable
    ) {
        return service.listByChannelDbId(channelDbId, pageable);
    }
}
