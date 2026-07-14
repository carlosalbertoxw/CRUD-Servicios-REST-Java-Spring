package com.carlosalbertoxw.notes.data;

import com.carlosalbertoxw.notes.model.Note;
import com.carlosalbertoxw.notes.model.NoteRequest;
import com.carlosalbertoxw.notes.model.NoteSummary;
import com.carlosalbertoxw.notes.model.PagedResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a notas con Spring JDBC (ligero, estilo Dapper: SQL explicito, sin ORM).
 * Cada consulta filtra por {@code owner_key_id} para aislar las notas por cliente.
 */
@Repository
public class JdbcNoteRepository implements NoteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcNoteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Note> NOTE_MAPPER = (rs, i) -> new Note(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("text"),
            rs.getObject("created_at", java.time.LocalDateTime.class),
            rs.getObject("updated_at", java.time.LocalDateTime.class));

    private static final RowMapper<NoteSummary> SUMMARY_MAPPER = (rs, i) -> new NoteSummary(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getObject("created_at", java.time.LocalDateTime.class),
            rs.getObject("updated_at", java.time.LocalDateTime.class));

    @Override
    public PagedResult<NoteSummary> getPage(String ownerKeyId, int afterId, int pageSize, String search) {
        boolean hasSearch = search != null && !search.isBlank();
        String filter = "owner_key_id = :ownerKeyId"
                + (hasSearch ? " AND MATCH(title, text) AGAINST(:search IN NATURAL LANGUAGE MODE)" : "");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerKeyId", ownerKeyId)
                .addValue("search", hasSearch ? search : null);

        long totalCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notes WHERE " + filter, params, Long.class);

        // Keyset: se pide un elemento extra para saber si hay pagina siguiente.
        MapSqlParameterSource pageParams = new MapSqlParameterSource()
                .addValues(params.getValues())
                .addValue("afterId", afterId)
                .addValue("limit", pageSize + 1);

        List<NoteSummary> items = jdbc.query(
                "SELECT id, title, created_at, updated_at "
                        + "FROM notes WHERE " + filter + " AND id > :afterId "
                        + "ORDER BY id LIMIT :limit",
                pageParams, SUMMARY_MAPPER);

        boolean hasMore = items.size() > pageSize;
        if (hasMore) {
            items = items.subList(0, pageSize);
        }
        Integer nextAfterId = hasMore ? items.get(items.size() - 1).id() : null;

        return new PagedResult<>(items, pageSize, totalCount, nextAfterId);
    }

    @Override
    public Optional<Note> getById(String ownerKeyId, int id) {
        List<Note> rows = jdbc.query(
                "SELECT id, title, text, created_at, updated_at "
                        + "FROM notes WHERE id = :id AND owner_key_id = :ownerKeyId",
                new MapSqlParameterSource().addValue("id", id).addValue("ownerKeyId", ownerKeyId),
                NOTE_MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public Note create(String ownerKeyId, NoteRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO notes (owner_key_id, title, text) VALUES (:ownerKeyId, :title, :text)",
                new MapSqlParameterSource()
                        .addValue("ownerKeyId", ownerKeyId)
                        .addValue("title", request.title())
                        .addValue("text", request.text()),
                keyHolder);

        int id = keyHolder.getKey().intValue();
        // Releer la fila para devolver los timestamps generados por la BD.
        return getById(ownerKeyId, id).orElseThrow();
    }

    @Override
    public boolean update(String ownerKeyId, int id, NoteRequest request) {
        int rows = jdbc.update(
                "UPDATE notes SET title = :title, text = :text "
                        + "WHERE id = :id AND owner_key_id = :ownerKeyId",
                new MapSqlParameterSource()
                        .addValue("title", request.title())
                        .addValue("text", request.text())
                        .addValue("id", id)
                        .addValue("ownerKeyId", ownerKeyId));
        return rows > 0;
    }

    @Override
    public boolean delete(String ownerKeyId, int id) {
        int rows = jdbc.update(
                "DELETE FROM notes WHERE id = :id AND owner_key_id = :ownerKeyId",
                new MapSqlParameterSource().addValue("id", id).addValue("ownerKeyId", ownerKeyId));
        return rows > 0;
    }
}
