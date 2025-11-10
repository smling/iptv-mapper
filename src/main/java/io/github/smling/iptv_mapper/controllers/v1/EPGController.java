package io.github.smling.iptv_mapper.controllers.v1;

import io.github.smling.iptv_mapper.services.epg.EPGService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Tag(name = "EPG", description = "Endpoints to maintain EPG data")
@RestController
@RequestMapping("/api/v1/epg")
public class EPGController {
    private final EPGService service;

    public EPGController(EPGService service) {
        this.service = service;
    }

    @GetMapping(produces = "application/xml; charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> epg(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to
    ) {
        StreamingResponseBody stream = out -> service.generateTo(out, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"epg.xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(stream);
    }
}
