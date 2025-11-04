package io.github.smling.iptv_mapper.services;

// src/main/java/io/github/smling/iptv_mapper/service/CrudService.java

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CRUDService<T> {
    T create(T toCreate);
    T get(UUID id);
    Page<T> list(Pageable pageable);
    T update(UUID id, T toUpdate);        // full replace (PUT semantics)
    void delete(UUID id);
}

