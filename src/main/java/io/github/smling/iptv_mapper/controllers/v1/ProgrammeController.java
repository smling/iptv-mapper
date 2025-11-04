package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.services.epg.ProgrammeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/programmes")
@Tag(name = "Programmes", description = "Programme maintenance endpoints")
public class ProgrammeController {

    private final ProgrammeService cleanupService;

    public ProgrammeController(ProgrammeService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Operation(
            summary = "Delete old programmes",
            description = "Deletes programme records older than N days. Default N comes from APP_CLEANUP_DAYS (default 7). " +
                    "Override via query param `days`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cleanup completed"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    })
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> deleteOldProgrammes(
            @Parameter(description = "Number of days to keep (delete anything older). Overrides default.")
            @RequestParam(name = "days", required = false) Integer days) {

        int deleted = cleanupService.cleanup(days);

        Map<String, Object> body = Map.of(
                "status", "ok",
                "deleted", deleted,
                "days", (days != null ? days : "default")
        );
        return ResponseEntity.ok(body);
    }
}
