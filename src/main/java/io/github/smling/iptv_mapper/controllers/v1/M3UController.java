package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.models.dto.common.PageDto;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UItemChannelView;
import io.github.smling.iptv_mapper.models.dto.m3u.ManualMappingRequest;
import io.github.smling.iptv_mapper.services.FuzzyMatchService;
import io.github.smling.iptv_mapper.services.m3u.M3UService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "M3U", description = "Endpoints to maintain M3U data.")
@RestController
@RequestMapping("/api/v1/m3u")
public class M3UController {
    private static final MediaType M3U_MIME =
            MediaType.parseMediaType("application/vnd.apple.mpegurl");

    private final M3UService m3UService;
    private final FuzzyMatchService fuzzyMatchService;

    public M3UController(M3UService m3UService, FuzzyMatchService fuzzyMatchService) {
        this.m3UService = m3UService;
        this.fuzzyMatchService = fuzzyMatchService;
    }

    /** GET /api/v1/m3u  – returns full playlist (no filters, no url-tvg) */
    @GetMapping
    public ResponseEntity<String> full() {
        String body = m3UService.generateAll();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"playlist.m3u\"")
                .contentType(M3U_MIME)
                .body(body);
    }

    @GetMapping("/mappings/channel")
    public PageDto.Response<M3UItemChannelView> getMappings(
            @PageableDefault(size = 50, sort = "mapId") Pageable pageable) {
        return PageDto.Response.of(m3UService.list(pageable));
    }

    @PostMapping("/mappings/manual")
    public ResponseEntity<Void> setManualMapping(@RequestBody ManualMappingRequest request) {
        try {
            fuzzyMatchService.setManualMapping(request.itemId(), request.channelId(), request.note());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}

