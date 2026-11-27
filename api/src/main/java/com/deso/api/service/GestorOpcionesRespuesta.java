package com.deso.api.service;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GestorOpcionesRespuesta {

    private static final int OFFSET_ORDEN_TEMPORAL = 1_000_000;

    private final OpcionRespuestaDAO opcionRespuestaDAO;

    public GestorOpcionesRespuesta(OpcionRespuestaDAO opcionRespuestaDAO) {
        this.opcionRespuestaDAO = opcionRespuestaDAO;
    }

    @Transactional
    public OpcionRespuestaDTO darDeAlta(OpcionRespuestaDTO datos) {
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
        OpcionRespuesta existente = opcionRespuestaDAO.buscarPorId(datos.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la opción de respuesta con ID " + datos.getId() + "."));

        if (Boolean.TRUE.equals(existente.getEliminada())) {
            throw new RecursoActualmenteEliminadoException(
                    "La opción de respuesta con ID " + datos.getId() + " se encuentra eliminada.");
        }

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

    public List<ListadoOpcionRespuestaDTO> buscarPorNombre(String nombre, Integer offset, Integer cantidad) {
        return opcionRespuestaDAO.buscarActivasPorNombre(nombre, offset, cantidad)
                .stream()
                .map(this::mapearAListadoDTO)
                .toList();
    }

    public OpcionRespuestaDTO buscarPorId(Long id) {
        OpcionRespuesta opcion = opcionRespuestaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la opción de respuesta con ID " + id + "."));

        if (Boolean.TRUE.equals(opcion.getEliminada())) {
            throw new RecursoActualmenteEliminadoException(
                    "La opción de respuesta con ID " + id + " se encuentra eliminada.");
        }

        return mapearADTO(opcion);
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
