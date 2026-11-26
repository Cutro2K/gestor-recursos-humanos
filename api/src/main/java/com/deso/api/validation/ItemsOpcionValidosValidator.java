package com.deso.api.validation;

import com.deso.api.dto.ItemOpcionDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ItemsOpcionValidosValidator implements ConstraintValidator<ItemsOpcionValidos, List<ItemOpcionDTO>> {

    @Override
    public boolean isValid(List<ItemOpcionDTO> items, ConstraintValidatorContext context) {
        if (items == null || items.size() < 2) {
            return true;
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

        if (mensajes.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(String.join(" ", mensajes))
                .addConstraintViolation();
        return false;
    }
}
