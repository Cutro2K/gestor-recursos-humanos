package com.deso.api.service;

import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GestorOpcionesRespuesta {

    private final OpcionRespuestaDAO opcionRespuestaDAO;

    public GestorOpcionesRespuesta(OpcionRespuestaDAO opcionRespuestaDAO) {
        this.opcionRespuestaDAO = opcionRespuestaDAO;
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
