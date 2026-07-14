package com.carlosalbertoxw.notes.web;

import com.carlosalbertoxw.notes.auth.CurrentKeyId;
import com.carlosalbertoxw.notes.data.NoteRepository;
import com.carlosalbertoxw.notes.model.Note;
import com.carlosalbertoxw.notes.model.NoteRequest;
import com.carlosalbertoxw.notes.model.NoteSummary;
import com.carlosalbertoxw.notes.model.PagedResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints REST de notas. Todas las operaciones actuan sobre las notas del
 * cliente autenticado; las notas de otros clientes responden 404.
 */
@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NoteRepository repository;

    public NotesController(NoteRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista las notas del cliente (id, titulo y fechas; el contenido se obtiene por id).
     * Paginacion por keyset: enviar afterId = nextAfterId de la pagina anterior.
     * Con search se filtra por texto completo sobre titulo y contenido.
     */
    @GetMapping
    public PagedResult<NoteSummary> list(
            @CurrentKeyId String ownerKeyId,
            @RequestParam(defaultValue = "0") @Min(0) int afterId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String search) {
        return repository.getPage(ownerKeyId, afterId, pageSize, search);
    }

    /** Obtiene una nota por su id, con su contenido completo. */
    @GetMapping("/{id}")
    public ResponseEntity<Note> get(@CurrentKeyId String ownerKeyId, @PathVariable int id) {
        return repository.getById(ownerKeyId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Crea una nota nueva. */
    @PostMapping
    public ResponseEntity<Note> create(@CurrentKeyId String ownerKeyId, @Valid @RequestBody NoteRequest request) {
        Note created = repository.create(ownerKeyId, request);
        URI location = UriComponentsBuilder.fromPath("/api/notes/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Actualiza una nota existente. */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @CurrentKeyId String ownerKeyId, @PathVariable int id, @Valid @RequestBody NoteRequest request) {
        return repository.update(ownerKeyId, id, request)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** Elimina una nota. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentKeyId String ownerKeyId, @PathVariable int id) {
        return repository.delete(ownerKeyId, id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
