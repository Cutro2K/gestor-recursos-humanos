package com.deso.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void recursoEnUsoDevuelve409ConErrorResponseDtoYPreservaElMensajeDeNegocio() throws Exception {
        String mensajeEsperado =
                "La opción de respuesta VERDADERO/FALSO está siendo usada en la base de datos y no puede eliminarse.";

        mockMvc.perform(delete("/api/opciones-respuesta/5"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").value(mensajeEsperado))
                .andExpect(jsonPath("$.path").value("/api/opciones-respuesta/5"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.erroresValidacion").doesNotExist());
    }

    @RestController
    private static class ControladorDePrueba {

        @DeleteMapping("/api/opciones-respuesta/{id}")
        void eliminar(@PathVariable Long id) {
            throw new RecursoEnUsoException(
                    "La opción de respuesta VERDADERO/FALSO está siendo usada en la base de datos y no puede eliminarse.");
        }
    }
}
