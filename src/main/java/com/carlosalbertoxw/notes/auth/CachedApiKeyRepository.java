package com.carlosalbertoxw.notes.auth;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorador con cache en memoria sobre el repositorio de API keys: el caso
 * caliente autentica sin tocar la BD (ni lookup ni registro de uso), dejando la
 * peticion en un solo round-trip. El costo asumido y documentado es que una
 * revocacion tarda hasta el TTL del cache en surtir efecto.
 */
@Repository
@Primary
public class CachedApiKeyRepository implements ApiKeyRepository {

    private final ApiKeyRepository delegate;
    private final long keyTtlNanos;
    // Menor que el INTERVAL 1 HOUR del UPDATE: la BD sigue siendo la fuente de
    // verdad aunque haya varias replicas con caches independientes.
    private static final long USED_TTL_NANOS = Duration.ofMinutes(50).toNanos();

    private final ConcurrentHashMap<String, Entry<Optional<ApiKey>>> keyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> usedCache = new ConcurrentHashMap<>();

    public CachedApiKeyRepository(JdbcApiKeyRepository delegate, AuthProperties properties) {
        this.delegate = delegate;
        this.keyTtlNanos = Duration.ofSeconds(properties.keyCacheSeconds()).toNanos();
    }

    @Override
    public Optional<ApiKey> getActiveKey(String keyId) {
        long now = System.nanoTime();
        Entry<Optional<ApiKey>> cached = keyCache.get(keyId);
        if (cached != null && cached.expiresAt - now > 0) {
            return cached.value;
        }
        Optional<ApiKey> key = delegate.getActiveKey(keyId);
        // Tambien se cachea el "no existe" para no martillar la BD con key_ids invalidos.
        keyCache.put(keyId, new Entry<>(key, now + keyTtlNanos));
        return key;
    }

    @Override
    public void registerUse(String keyId) {
        long now = System.nanoTime();
        Long until = usedCache.get(keyId);
        if (until != null && until - now > 0) {
            return;
        }
        delegate.registerUse(keyId);
        usedCache.put(keyId, now + USED_TTL_NANOS);
    }

    private record Entry<T>(T value, long expiresAt) {
    }
}
