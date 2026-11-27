package com.deso.api.controller;

import com.deso.api.dto.ItemOpcionDTO;
import com.deso.api.dto.ListadoOpcionRespuestaDTO;
import com.deso.api.dto.OpcionRespuestaDTO;
import com.deso.api.exception.CredencialesInvalidasException;
import com.deso.api.security.EmisorTokens;
import com.deso.api.service.GestorAutenticacion;
import com.deso.api.service.GestorOpcionesRespuesta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Controlador REST para la gestión del catálogo de Opciones de Respuesta (/api/opciones-respuesta).
 *
 * Endpoints expuestos:
 * - POST   /api/opciones-respuesta      (CU 29): Da de alta una nueva opción de respuesta con sus ítems de opción.
 * - GET    /api/opciones-respuesta      (CU 24): Busca opciones de respuesta activas filtrando por nombre (paginado).
 * - PUT    /api/opciones-respuesta/{id} (CU 30): Modifica descripción e ítems de una opción de respuesta activa in-place.
 * - DELETE /api/opciones-respuesta/{id} (CU 31): Da de baja lógica una opción de respuesta no usada y registra auditoría.
 * - GET    /api/opciones-respuesta/{id} (CU 21 / CU 22 / CU 30): Obtiene el detalle de una opción de respuesta activa con sus ítems ordenados.
 */
@RestController
@RequestMapping(value = "/api/opciones-respuesta", produces = MediaType.APPLICATION_JSON_VALUE)
public class OpcionRespuestaController {

    private final GestorOpcionesRespuesta gestorOpcionesRespuesta;
    private final EmisorTokens emisorTokens;

    public OpcionRespuestaController(
            GestorOpcionesRespuesta gestorOpcionesRespuesta,
            EmisorTokens emisorTokens) {
        this.gestorOpcionesRespuesta = gestorOpcionesRespuesta;
        this.emisorTokens = emisorTokens;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpcionRespuestaDTO> darDeAlta(
            @Valid @RequestBody OpcionRespuestaDTO request) {
        validarItems(request.getItems());
        OpcionRespuestaDTO creada = gestorOpcionesRespuesta.darDeAlta(request);
        URI location = URI.create("/api/opciones-respuesta/" + creada.getId());
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping
    public List<ListadoOpcionRespuestaDTO> buscarPorNombre(
            @RequestParam(required = false)
            @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
            String nombre,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "El offset no puede ser negativo.")
            Integer offset,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "La cantidad mínima es 1.")
            @Max(value = 100, message = "La cantidad máxima es 100.")
            Integer cantidad) {
        return gestorOpcionesRespuesta.buscarPorNombre(nombre, offset, cantidad);
    }

    @GetMapping("/{id}")
    public OpcionRespuestaDTO obtenerPorId(
            @PathVariable @Positive(message = "El ID de la opción de respuesta debe ser positivo.") Long id) {
        return gestorOpcionesRespuesta.buscarPorId(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpcionRespuestaDTO> modificar(
            @PathVariable @Positive(message = "El ID de la opción de respuesta debe ser positivo.") Long id,
            @Valid @RequestBody OpcionRespuestaDTO request) {
        validarItems(request.getItems());
        request.setId(id);
        OpcionRespuestaDTO actualizada = gestorOpcionesRespuesta.actualizar(request);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Positive(message = "El ID de la opción de respuesta debe ser positivo.") Long id,
            @CookieValue(name = AuthController.COOKIE_ACCESS, required = false) String accessToken) {
        EmisorTokens.DatosAccess datos = emisorTokens.leerAccess(accessToken)
                .filter(d -> GestorAutenticacion.ROL_CONSULTOR.equals(d.rol()))
                .orElseThrow(() -> new CredencialesInvalidasException("La sesión no es válida o venció"));
        gestorOpcionesRespuesta.eliminarLogicamente(id, datos.sujetoId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida que la lista de ítems de opción tenga órdenes consecutivos 1..N comenzando en 1
     * sin huecos ni repetidos y que no repita descripciones ignorando espacios extremos y mayúsculas/minúsculas.
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
            boolean secuenciaValida = true;
            for (ItemOpcionDTO item : items) {
                int orden = item.getOrden();
                if (orden < 1 || orden > n || !ordenesVistos.add(orden)) {
                    secuenciaValida = false;
                    break;
                }
            }
            if (!secuenciaValida) {
                mensajes.add("El orden de visualización de los ítems de opción debe ser consecutivo comenzando en 1 sin saltos ni repeticiones.");
            }
        }

        Set<String> descripcionesVistas = new HashSet<>();
        boolean descripcionDuplicada = false;
        for (ItemOpcionDTO item : items) {
            if (item != null && item.getDescripcion() != null && !item.getDescripcion().isBlank()) {
                String clave = item.getDescripcion().strip().toLowerCase(Locale.ROOT);
                if (!descripcionesVistas.add(clave)) {
                    descripcionDuplicada = true;
                    break;
                }
            }
        }
        if (descripcionDuplicada) {
            mensajes.add("La descripción de los ítems de opción no puede repetirse dentro de la misma opción de respuesta.");
        }

        if (!mensajes.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", mensajes));
        }
    }
}
