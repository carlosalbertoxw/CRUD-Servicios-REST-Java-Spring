package com.carlosalbertoxw.notes.auth;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@code api_keys} contra MySQL. La comparacion del secreto la hace
 * el filtro (hash en tiempo constante); aqui solo se recupera la key activa.
 */
@Repository
public class JdbcApiKeyRepository implements ApiKeyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcApiKeyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ApiKey> MAPPER = (rs, i) -> new ApiKey(
            rs.getString("key_id"),
            rs.getBytes("key_hash"),
            rs.getString("client_name"));

    @Override
    public Optional<ApiKey> getActiveKey(String keyId) {
        List<ApiKey> rows = jdbc.query(
                "SELECT key_id, key_hash, client_name FROM api_keys "
                        + "WHERE key_id = :keyId AND revoked_at IS NULL "
                        + "AND (expires_at IS NULL OR expires_at > UTC_TIMESTAMP())",
                new MapSqlParameterSource("keyId", keyId),
                MAPPER);
        return rows.stream().findFirst();
    }

    @Override
    public void registerUse(String keyId) {
        // Actualizacion laxa: solo escribe si paso mas de una hora desde el
        // ultimo registro, para no convertir cada request en una escritura.
        jdbc.update(
                "UPDATE api_keys SET last_used_at = UTC_TIMESTAMP() "
                        + "WHERE key_id = :keyId "
                        + "AND (last_used_at IS NULL OR last_used_at < UTC_TIMESTAMP() - INTERVAL 1 HOUR)",
                new MapSqlParameterSource("keyId", keyId));
    }
}
