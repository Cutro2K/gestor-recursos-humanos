package com.deso.api.service;

import com.deso.api.dao.AuditoriaDAO;
import com.deso.api.dao.ConsultorDAO;
import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoEnUsoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Consultor;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.RegistroAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GestorOpcionesRespuesta {

    private static final int OFFSET_ORDEN_TEMPORAL = 1_000_000;
    private static final String ACCION_BAJA = "BAJA";
    private static final String ENTIDAD_OPCION_RESPUESTA = "OPCION_RESPUESTA";

    private final OpcionRespuestaDAO opcionRespuestaDAO;
    private final ConsultorDAO consultorDAO;
    private final AuditoriaDAO auditoriaDAO;

    public GestorOpcionesRespuesta(
            OpcionRespuestaDAO opcionRespuestaDAO,
            ConsultorDAO consultorDAO,
            AuditoriaDAO auditoriaDAO) {
        this.opcionRespuestaDAO = opcionRespuestaDAO;
        this.consultorDAO = consultorDAO;
        this.auditoriaDAO = auditoriaDAO;
    }

    @Transactional
    public OpcionRespuestaDTO darDeAlta(OpcionRespuestaDTO datos) {
        validarItems(datos.getItems());
        String nombreLimpio = datos.getNombre().strip();
        if (opcionRespuestaDAO.existeActivaPorNombre(nombreLimpio)) {
            throw new RecursoDuplicadoException(
                    "nombre",
                    "Ya existe una opción de respuesta activa con el nombre '" + nombreLimpio + "'."
            );
        }

        OpcionRespuesta nuevaOpcion = new OpcionRespuesta();
        nuevaOpcion.setNombre(nombreLimpio);
        nuevaOpcion.setDescripcion(datos.getDescripcion().strip());
        nuevaOpcion.setEliminada(false);

        List<ItemOpcion> itemsOrdenados = datos.getItems().stream()
                .sorted(Comparator.comparing(ItemOpcionDTO::getOrden))
                .map(dto -> {
                    ItemOpcion item = new ItemOpcion();
                    item.setOpcionRespuesta(nuevaOpcion);
                    item.setOrden(dto.getOrden());
                    item.setDescripcion(dto.getDescripcion().strip());
                    return item;
                })
                .toList();
        nuevaOpcion.setItems(new ArrayList<>(itemsOrdenados));

        OpcionRespuesta guardada = opcionRespuestaDAO.guardar(nuevaOpcion);
        return mapearADTO(guardada);
    }

    @Transactional
    public OpcionRespuestaDTO actualizar(OpcionRespuestaDTO datos) {
        validarItems(datos.getItems());
        OpcionRespuesta existente = obtenerActivaPorId(datos.getId());

        if (datos.getNombre() != null && !datos.getNombre().isBlank()
                && !datos.getNombre().strip().equals(existente.getNombre().strip())) {
            throw new IllegalArgumentException(
                    "El nombre de la opción de respuesta es de sólo lectura y no puede modificarse.");
        }

        Map<Long, ItemOpcionDTO> recibidosPorId = new HashMap<>();
        for (ItemOpcionDTO dto : datos.getItems()) {
            if (dto.getId() == null || recibidosPorId.put(dto.getId(), dto) != null) {
                throw new IllegalArgumentException(
                        "Los ítems de opción enviados deben tener IDs existentes y sin repetir.");
            }
        }
        boolean mismoConjunto = recibidosPorId.size() == existente.getItems().size()
                && existente.getItems().stream().allMatch(i -> recibidosPorId.containsKey(i.getId()));
        if (!mismoConjunto) {
            throw new IllegalArgumentException(
                    "Los ítems de opción enviados deben coincidir exactamente con los existentes: no se permiten altas ni bajas.");
        }

        // Valores temporales no colisionantes: las restricciones únicas de orden y descripción
        // no son diferibles en H2, por lo que los intercambios exigen un paso intermedio.
        for (ItemOpcion item : existente.getItems()) {
            item.setOrden(OFFSET_ORDEN_TEMPORAL + item.getOrden());
            item.setDescripcion("__tmp__" + item.getId());
        }
        opcionRespuestaDAO.sincronizar();

        existente.setDescripcion(datos.getDescripcion().strip());
        for (ItemOpcion item : existente.getItems()) {
            ItemOpcionDTO dto = recibidosPorId.get(item.getId());
            item.setOrden(dto.getOrden());
            item.setDescripcion(dto.getDescripcion().strip());
        }
        existente.getItems().sort(Comparator.comparing(ItemOpcion::getOrden));

        OpcionRespuesta guardada = opcionRespuestaDAO.guardar(existente);
        return mapearADTO(guardada);
    }

    @Transactional
    public void eliminarLogicamente(Long id, Long consultorId) {
        Consultor consultor = consultorDAO.buscarPorId(consultorId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new CredencialesInvalidasException("La sesión no es válida o venció"));

        OpcionRespuesta opcion = obtenerActivaPorId(id);

        if (opcionRespuestaDAO.estaEnUso(id)) {
            throw new RecursoEnUsoException(
                    "La opción de respuesta " + opcion.getNombre()
                            + " está siendo usada en la base de datos y no puede eliminarse.");
        }

        opcionRespuestaDAO.eliminarLogicamente(opcion.getId());
        auditoriaDAO.guardar(new RegistroAuditoria(
                null,
                consultor,
                LocalDateTime.now(),
                ACCION_BAJA,
                ENTIDAD_OPCION_RESPUESTA,
                opcion.getId(),
                "Baja lógica de la opción de respuesta " + opcion.getNombre()
        ));
    }

    /**
     * Indica si la opción de respuesta activa tiene preguntas activas asociadas (CU 31, paso 2):
     * la interfaz lo consulta antes de pedir la confirmación de la baja.
     */
    public boolean estaEnUso(Long id) {
        return opcionRespuestaDAO.estaEnUso(obtenerActivaPorId(id).getId());
    }

    public List<ListadoOpcionRespuestaDTO> buscarPorNombre(String nombre, Integer offset, Integer cantidad) {
        return opcionRespuestaDAO.buscarActivasPorNombre(nombre, offset, cantidad)
                .stream()
                .map(this::mapearAListadoDTO)
                .toList();
    }

    public OpcionRespuestaDTO buscarPorId(Long id) {
        return mapearADTO(obtenerActivaPorId(id));
    }

    /**
     * Valida que los ítems de opción tengan órdenes consecutivos 1..N sin huecos ni repetidos
     * (CU 29 2.C / CU 30 2.B) y que no repitan descripción ignorando espacios extremos y
     * mayúsculas/minúsculas (CU 29 2.D / CU 30 2.C). Las omisiones las cubre la validación del DTO.
     */
    private void validarItems(List<ItemOpcionDTO> items) {
        if (items == null || items.size() < 2) {
            return;
        }

        List<String> mensajes = new ArrayList<>();

        boolean todosConOrdenPositivo = items.stream()
                .allMatch(item -> item != null && item.getOrden() != null && item.getOrden() > 0);
        if (todosConOrdenPositivo) {
            int n = items.size();
            Set<Integer> ordenesVistos = new HashSet<>();
            boolean secuenciaValida = items.stream()
                    .allMatch(item -> item.getOrden() <= n && ordenesVistos.add(item.getOrden()));
            if (!secuenciaValida) {
                mensajes.add("El orden de visualización de los ítems de opción debe ser consecutivo comenzando en 1 sin saltos ni repeticiones.");
            }
        }

        Set<String> descripcionesVistas = new HashSet<>();
        boolean descripcionDuplicada = items.stream()
                .filter(item -> item != null && item.getDescripcion() != null && !item.getDescripcion().isBlank())
                .anyMatch(item -> !descripcionesVistas.add(item.getDescripcion().strip().toLowerCase(Locale.ROOT)));
        if (descripcionDuplicada) {
            mensajes.add("La descripción de los ítems de opción no puede repetirse dentro de la misma opción de respuesta.");
        }

        if (!mensajes.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", mensajes));
        }
    }

    private OpcionRespuesta obtenerActivaPorId(Long id) {
        OpcionRespuesta opcion = opcionRespuestaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la opción de respuesta con ID " + id + "."));

        if (Boolean.TRUE.equals(opcion.getEliminada())) {
            throw new RecursoActualmenteEliminadoException(
                    "La opción de respuesta con ID " + id + " se encuentra eliminada.");
        }

        return opcion;
    }

    private ListadoOpcionRespuestaDTO mapearAListadoDTO(OpcionRespuesta opcion) {
        return new ListadoOpcionRespuestaDTO(
                opcion.getId(),
                opcion.getNombre(),
                opcion.getDescripcion()
        );
    }

    private OpcionRespuestaDTO mapearADTO(OpcionRespuesta opcion) {
        List<ItemOpcionDTO> items = opcion.getItems() == null
                ? List.of()
                : opcion.getItems().stream()
                        .sorted(Comparator.comparing(ItemOpcion::getOrden))
                        .map(item -> new ItemOpcionDTO(
                                item.getId(),
                                item.getOrden(),
                                item.getDescripcion()))
                        .toList();

        return new OpcionRespuestaDTO(
                opcion.getId(),
                opcion.getNombre(),
                opcion.getDescripcion(),
                items
        );
    }
}
