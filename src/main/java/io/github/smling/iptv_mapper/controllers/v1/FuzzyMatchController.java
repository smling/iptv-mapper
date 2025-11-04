package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.services.FuzzyMatchService;
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
@RequestMapping("/api/v1/match")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ingestion", description = "Endpoints to ingest M3U/EPG sources")
public class FuzzyMatchController {
    private final FuzzyMatchService fuzzyMatchService;

    public FuzzyMatchController(FuzzyMatchService fuzzyMatchService) {
        this.fuzzyMatchService = fuzzyMatchService;
    }

    @Operation(
            summary = "Ingest all EPG sources",
            description = "Fetch all enabled EPG data sources, parse playlists in parallel, and store results."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Ingestion started"),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @PostMapping("/match")
    public ResponseEntity<?> match() {
        fuzzyMatchService.match();
        return ResponseEntity.accepted().body(Map.of("status","accepted"));
    }
}
