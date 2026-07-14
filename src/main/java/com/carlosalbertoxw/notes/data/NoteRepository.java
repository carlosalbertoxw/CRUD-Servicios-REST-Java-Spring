package com.carlosalbertoxw.notes.data;

import com.carlosalbertoxw.notes.model.Note;
import com.carlosalbertoxw.notes.model.NoteRequest;
import com.carlosalbertoxw.notes.model.NoteSummary;
import com.carlosalbertoxw.notes.model.PagedResult;

import java.util.Optional;

/**
 * Todas las operaciones estan acotadas al dueno (ownerKeyId, el key_id
 * autenticado): un cliente nunca ve ni modifica notas de otro.
 */
public interface NoteRepository {

    PagedResult<NoteSummary> getPage(String ownerKeyId, int afterId, int pageSize, String search);

    Optional<Note> getById(String ownerKeyId, int id);

    Note create(String ownerKeyId, NoteRequest request);

    boolean update(String ownerKeyId, int id, NoteRequest request);

    boolean delete(String ownerKeyId, int id);
}
