package io.github.smling.iptv_mapper.models.dto.common;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pageable request/response DTOs for stable API schemas.
 */
public final class PageDto {

    @Schema(name = "PageRequest", description = "Pagination request parameters")
    public record Request(
            @Schema(description = "Zero-based page index", example = "0", minimum = "0")
            Integer page,

            @Schema(description = "Page size", example = "20", minimum = "1", maximum = "200")
            Integer size,

            @ArraySchema(schema = @Schema(description = "Sort instructions as 'field,asc' or 'field,desc'", example = "createdAt,desc"))
            List<String> sort
    ) {}

    @Schema(name = "PageResponse", description = "Paged response wrapper")
    public record Response<T>(
            @Schema(description = "Page content")
            List<T> content,

            @Schema(description = "Zero-based page index", example = "0")
            int page,

            @Schema(description = "Page size", example = "20")
            int size,

            @Schema(description = "Total number of elements", example = "123")
            long totalElements,

            @Schema(description = "Total number of pages", example = "7")
            int totalPages,

            @Schema(description = "Is this the first page?", example = "true")
            boolean first,

            @Schema(description = "Is this the last page?", example = "false")
            boolean last
    ) {
        public static <T> Response<T> of(Page<T> page) {
            return new Response<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast()
            );
        }
    }
}

