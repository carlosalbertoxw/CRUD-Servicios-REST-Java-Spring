package com.carlosalbertoxw.notes.support;

import com.carlosalbertoxw.notes.web.ProblemResponder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Utilidades compartidas por las pruebas unitarias de los filtros, que escriben
 * sus errores a traves de {@link ProblemResponder}.
 */
public final class TestProblems {

    public static final JsonMapper JSON = JsonMapper.builder().build();

    private TestProblems() {
    }

    public static ProblemResponder responder() {
        return new ProblemResponder(JSON);
    }
}
