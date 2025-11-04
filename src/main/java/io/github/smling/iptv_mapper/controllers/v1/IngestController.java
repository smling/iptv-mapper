package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.services.epg.EPGService;
import io.github.smling.iptv_mapper.services.m3u.M3UService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingest")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ingestion", description = "Endpoints to ingest M3U/EPG sources")
public class IngestController {
    private final M3UService m3uService;
    private final EPGService epgService;
    public IngestController(M3UService m3uService, EPGService epgService) {
        this.m3uService = m3uService;
        this.epgService = epgService;
    }

    @Operation(
            summary = "Ingest all M3U sources",
            description = "Fetch all enabled M3U data sources, parse playlists in parallel, and store results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Ingestion started"),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/m3u")
    public ResponseEntity<?> ingestM3U() {
        m3uService.ingestDataSources(); // uses an Executor for parallel fetch
        return ResponseEntity.accepted().body(Map.of("status","accepted"));
    }

    @Operation(
            summary = "Ingest all EPG sources",
            description = "Fetch all enabled EPG data sources, parse playlists in parallel, and store results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Ingestion started"),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/epg")
    public ResponseEntity<?> ingestEPG() {
        epgService.ingestDataSources(); // uses an Executor for parallel fetch
        return ResponseEntity.accepted().body(Map.of("status","accepted"));
    }
}
