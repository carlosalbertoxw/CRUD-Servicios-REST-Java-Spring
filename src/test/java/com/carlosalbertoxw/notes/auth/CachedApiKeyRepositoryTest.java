package com.carlosalbertoxw.notes.auth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del cache en memoria de API keys: se sustituye el
 * repositorio JDBC por un doble que cuenta las llamadas, sin tocar la BD.
 */
class CachedApiKeyRepositoryTest {

    private static final ApiKey KEY = new ApiKey("cliente-1", new byte[32], "Cliente 1");

    private final CountingJdbcRepository delegate = new CountingJdbcRepository();

    private CachedApiKeyRepository cacheWithTtl(int seconds) {
        return new CachedApiKeyRepository(delegate, new AuthProperties(seconds));
    }

    @Test
    void hitsDatabaseOnceWhileTheEntryIsFresh() {
        delegate.stored = KEY;
        CachedApiKeyRepository repository = cacheWithTtl(60);

        assertThat(repository.getActiveKey("cliente-1")).contains(KEY);
        assertThat(repository.getActiveKey("cliente-1")).contains(KEY);
        assertThat(repository.getActiveKey("cliente-1")).contains(KEY);

        assertThat(delegate.lookups).containsExactly("cliente-1");
    }

    @Test
    void cachesTheAbsenceOfAKeyToAvoidHammeringTheDatabase() {
        delegate.stored = null;
        CachedApiKeyRepository repository = cacheWithTtl(60);

        assertThat(repository.getActiveKey("desconocida")).isEmpty();
        assertThat(repository.getActiveKey("desconocida")).isEmpty();

        assertThat(delegate.lookups).containsExactly("desconocida");
    }

    @Test
    void keepsEachKeyIdInItsOwnEntry() {
        delegate.stored = KEY;
        CachedApiKeyRepository repository = cacheWithTtl(60);

        repository.getActiveKey("cliente-1");
        repository.getActiveKey("cliente-2");

        assertThat(delegate.lookups).containsExactly("cliente-1", "cliente-2");
    }

    @Test
    void goesBackToTheDatabaseWhenTheEntryExpired() {
        // TTL 0: cada consulta esta vencida al instante, que es el comportamiento
        // que hace efectiva una revocacion al cumplirse el TTL configurado.
        delegate.stored = KEY;
        CachedApiKeyRepository repository = cacheWithTtl(0);

        repository.getActiveKey("cliente-1");
        repository.getActiveKey("cliente-1");

        assertThat(delegate.lookups).containsExactly("cliente-1", "cliente-1");
    }

    @Test
    void registersUseOnlyOncePerTtlWindow() {
        CachedApiKeyRepository repository = cacheWithTtl(60);

        repository.registerUse("cliente-1");
        repository.registerUse("cliente-1");
        repository.registerUse("cliente-2");

        assertThat(delegate.uses).containsExactly("cliente-1", "cliente-2");
    }

    /**
     * Doble del repositorio JDBC: hereda de la clase concreta que exige el
     * constructor del cache y no llega a usar el JdbcTemplate.
     */
    private static final class CountingJdbcRepository extends JdbcApiKeyRepository {

        private ApiKey stored;
        private final List<String> lookups = new ArrayList<>();
        private final List<String> uses = new ArrayList<>();

        CountingJdbcRepository() {
            super(null);
        }

        @Override
        public Optional<ApiKey> getActiveKey(String keyId) {
            lookups.add(keyId);
            return Optional.ofNullable(stored).filter(key -> key.keyId().equals(keyId));
        }

        @Override
        public void registerUse(String keyId) {
            uses.add(keyId);
        }
    }
}
