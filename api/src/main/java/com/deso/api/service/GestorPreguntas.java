package com.deso.api.service;

import com.deso.api.dao.FactorDAO;
import com.deso.api.dao.OpcionRespuestaDAO;
import com.deso.api.dao.PreguntaDAO;
import com.deso.api.dto.ListadoPreguntaDTO;
import com.deso.api.dto.PonderacionDTO;
import com.deso.api.dto.PreguntaDTO;
import com.deso.api.exception.RecursoActualmenteEliminadoException;
import com.deso.api.exception.RecursoDuplicadoException;
import com.deso.api.exception.RecursoNoEncontradoException;
import com.deso.api.model.entity.Factor;
import com.deso.api.model.entity.ItemOpcion;
import com.deso.api.model.entity.OpcionRespuesta;
import com.deso.api.model.entity.Pregunta;
import com.deso.api.model.entity.PreguntaItemOpcion;
import com.deso.api.model.entity.PreguntaItemOpcionId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GestorPreguntas {

    private final PreguntaDAO preguntaDAO;
    private final FactorDAO factorDAO;
    private final OpcionRespuestaDAO opcionRespuestaDAO;

    public GestorPreguntas(
            PreguntaDAO preguntaDAO,
            FactorDAO factorDAO,
            OpcionRespuestaDAO opcionRespuestaDAO
    ) {
        this.preguntaDAO = preguntaDAO;
        this.factorDAO = factorDAO;
        this.opcionRespuestaDAO = opcionRespuestaDAO;
    }

    @Transactional
    public PreguntaDTO darDeAlta(PreguntaDTO datos) {
        // 1. Validar existencia y estado del factor padre
        Factor factor = factorDAO.buscarPorId(datos.getFactorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El factor especificado no existe."));

        if (Boolean.TRUE.equals(factor.getEliminado())) {
            throw new RecursoActualmenteEliminadoException("No se pueden agregar preguntas a un factor eliminado.");
        }

        // 2. Validar existencia y estado de la opción de respuesta
        OpcionRespuesta opcionRespuesta = opcionRespuestaDAO.buscarPorId(datos.getOpcionRespuestaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La opción de respuesta especificada no existe."));

        if (Boolean.TRUE.equals(opcionRespuesta.getEliminada())) {
            throw new RecursoActualmenteEliminadoException("No se puede asignar una opción de respuesta eliminada.");
        }

        // 3. Normalizar y validar unicidad dentro del factor (UQ en DER)
        String nombre = datos.getNombre().strip();
        String enunciado = datos.getEnunciado().strip();
        String descripcion = normalizarTexto(datos.getDescripcion());

        if (preguntaDAO.existePorFactorYNombre(factor.getId(), nombre)) {
            throw new RecursoDuplicadoException("nombre", "Ya existe una pregunta con ese nombre para este factor.");
        }

        if (preguntaDAO.existePorFactorYEnunciado(factor.getId(), enunciado)) {
            throw new RecursoDuplicadoException("enunciado", "Ya existe una pregunta con ese enunciado para este factor.");
        }

        // 4. Validar ponderaciones contra los ítems de la opción de respuesta
        Map<Long, ItemOpcion> itemsPorId = opcionRespuesta.getItems().stream()
                .collect(Collectors.toMap(ItemOpcion::getId, Function.identity()));

        validarPonderaciones(datos.getPonderaciones(), itemsPorId);

        // 5. Mapear y persistir la entidad
        Pregunta nuevaPregunta = new Pregunta();
        nuevaPregunta.setFactor(factor);
        nuevaPregunta.setOpcionRespuesta(opcionRespuesta);
        nuevaPregunta.setNombre(nombre);
        nuevaPregunta.setEnunciado(enunciado);
        nuevaPregunta.setDescripcion(descripcion);
        nuevaPregunta.setActiva(true);
        nuevaPregunta.setEliminada(false);

        for (PonderacionDTO p : datos.getPonderaciones()) {
            ItemOpcion item = itemsPorId.get(p.getItemOpcionId());
            PreguntaItemOpcion ponderacion = new PreguntaItemOpcion();
            ponderacion.setId(new PreguntaItemOpcionId(null, item.getId()));
            ponderacion.setPregunta(nuevaPregunta);
            ponderacion.setItemOpcion(item);
            ponderacion.setPonderacion(p.getPonderacion());
            nuevaPregunta.getPonderaciones().add(ponderacion);
        }

        Pregunta guardada = preguntaDAO.guardar(nuevaPregunta);
        return mapearAPreguntaDTO(guardada, datos.getPonderaciones());
    }

    public List<ListadoPreguntaDTO> buscarConFiltros(
            Long competenciaId,
            Long factorId,
            String nombre,
            String enunciado,
            Boolean activa,
            Integer offset,
            Integer cantidad
    ) {
        return preguntaDAO.buscarConFiltros(competenciaId, factorId, nombre, enunciado, activa, offset, cantidad)
                .stream()
                .map(this::mapearAListadoDTO)
                .toList();
    }

    public PreguntaDTO buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la pregunta no puede ser nulo.");
        }

        Pregunta pregunta = preguntaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la pregunta con ID " + id + "."));

        if (Boolean.TRUE.equals(pregunta.getEliminada())) {
            throw new RecursoActualmenteEliminadoException("La pregunta con ID " + id + " se encuentra eliminada.");
        }

        return mapearAPreguntaDTO(pregunta, List.of());
    }

    private void validarPonderaciones(List<PonderacionDTO> ponderaciones, Map<Long, ItemOpcion> itemsValidos) {
        if (ponderaciones == null || ponderaciones.isEmpty()) {
            throw new IllegalArgumentException("Debe informar las ponderaciones para los ítems de la opción de respuesta.");
        }

        Set<Long> idsRecibidos = ponderaciones.stream()
                .map(PonderacionDTO::getItemOpcionId)
                .collect(Collectors.toSet());

        if (idsRecibidos.size() != ponderaciones.size()) {
            throw new IllegalArgumentException("Hay ítems de opción repetidos en las ponderaciones.");
        }

        if (!idsRecibidos.equals(itemsValidos.keySet())) {
            throw new IllegalArgumentException("Debe informar exactamente una ponderación por cada ítem de la opción de respuesta.");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.strip();
        return recortado.isEmpty() ? null : recortado;
    }

    private PreguntaDTO mapearAPreguntaDTO(Pregunta pregunta, List<PonderacionDTO> ponderaciones) {
        return new PreguntaDTO(
                pregunta.getId(),
                pregunta.getFactor().getId(),
                pregunta.getOpcionRespuesta().getId(),
                pregunta.getNombre(),
                pregunta.getEnunciado(),
                pregunta.getDescripcion(),
                pregunta.getActiva(),
                ponderaciones
        );
    }

    private ListadoPreguntaDTO mapearAListadoDTO(Pregunta pregunta) {
        return new ListadoPreguntaDTO(
                pregunta.getId(),
                pregunta.getNombre(),
                pregunta.getEnunciado(),
                pregunta.getFactor().getId(),
                pregunta.getFactor().getNombre(),
                pregunta.getFactor().getCompetencia().getId(),
                pregunta.getFactor().getCompetencia().getNombre(),
                pregunta.getOpcionRespuesta().getId(),
                pregunta.getOpcionRespuesta().getNombre(),
                pregunta.getActiva()
        );
    }
}
