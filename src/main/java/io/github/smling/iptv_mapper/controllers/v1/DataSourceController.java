package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dto.common.PageDto;
import io.github.smling.iptv_mapper.services.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/dataSource")
@Tag(name = "Data Source", description = "Endpoints for managing data sources")
public class DataSourceController extends BaseApiV1Controller {
    private final DataSourceService service;

    public DataSourceController(DataSourceService service) {
        this.service = service;
    }

    /**
     * GET /api/v1/data-sources
     * Retrieve a paginated list of all data sources.
     */
    @Operation(
            summary = "List all data sources",
            description = "Returns a paginated list of data sources. Supports standard pageable parameters: page, size, and sort."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageDto.Response.class)))
    })
    @GetMapping
    public ResponseEntity<PageDto.Response<DataSourceEntity>> list(Pageable pageable) {
        var page = service.list(pageable);
        return ResponseEntity.ok(PageDto.Response.of(page));
    }

    /**
     * GET /api/v1/data-sources/{id}
     * Retrieve a specific data source by its UUID.
     */
    @Operation(
            summary = "Get data source by ID",
            description = "Fetch a specific data source using its unique UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data source found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DataSourceEntity.class))),
            @ApiResponse(responseCode = "404", description = "Data source not found",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<DataSourceEntity> getById(@PathVariable UUID id) {
        try {
            DataSourceEntity entity = service.get(id);
            return ResponseEntity.ok(entity);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
