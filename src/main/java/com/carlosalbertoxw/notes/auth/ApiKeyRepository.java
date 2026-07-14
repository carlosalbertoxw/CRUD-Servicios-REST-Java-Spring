package com.carlosalbertoxw.notes.auth;

import java.util.Optional;

public interface ApiKeyRepository {

    /** Devuelve la key activa (no revocada ni expirada) con ese id, o vacio. */
    Optional<ApiKey> getActiveKey(String keyId);

    /** Registra el uso de la key (actualizacion laxa de last_used_at). */
    void registerUse(String keyId);
}
