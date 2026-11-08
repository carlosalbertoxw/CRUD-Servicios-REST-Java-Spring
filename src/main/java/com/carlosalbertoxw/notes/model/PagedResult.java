package com.carlosalbertoxw.notes.model;

import java.util.List;

/**
 * Pagina de resultados con paginacion por keyset: para obtener la siguiente
 * pagina se envia ?afterId={nextAfterId}. Costo constante en la BD sin importar
 * que tan profunda sea la pagina (a diferencia de LIMIT/OFFSET). No incluye un
 * total: contarlo exigiria recorrer todas las notas del cliente en cada pagina.
 *
 * @param nextAfterId cursor para la siguiente pagina, o {@code null} si no hay mas resultados.
 */
public record PagedResult<T>(List<T> items, int pageSize, Integer nextAfterId) {}
