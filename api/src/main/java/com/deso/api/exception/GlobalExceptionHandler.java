package com.deso.api.exception;

import com.deso.api.dto.ErrorResponseDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

/*
    GlobalExceptionHandler: 
         Se encarga de hacer catch de las excepciones en cualquier parte del codigo y centralizar el formateo
        de los errores a un DTO común llamado ErrorResponseDTO, donde cada una de los ExceptionHandler internos
        devolveran una entidad de respuesta http de la clase ResponseEntity.

    -> El objetivo principal de formatear las excepciones hacia el frontend es facilitar el rastreo de errores.
*/
@Slf4j  // Anotación para utilzar la log function de la lombok
@RestControllerAdvice 
public class GlobalExceptionHandler {

    // 1. Validaciones de DTOs (@Valid) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotValidArguments(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ErrorResponseDTO errorDto = new ErrorResponseDTO(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Argumentos no validos",
            "Los datos enviados contienen errores en uno o más campos",
            request.getRequestURI(),
            errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    // 2. JSON mal formado o tipos incompatibles en el body -> 400 Bad Request
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Petición Inválida",
            "El cuerpo de la petición (JSON) está mal formado o contiene tipos de datos incompatibles",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 3. Parámetro de URL con tipo erroneo (ej: /puestos/abc en vez de /puestos/1) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensaje = String.format(
            "El parámetro '%s' con valor '%s' no es válido. Se esperaba un valor de tipo '%s'",
            ex.getName(), 
            ex.getValue(), 
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido"
        );
        
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.BAD_REQUEST.value(),
            "Parámetro Inválido",
            mensaje,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 4. Violación de restricciones de base de datos (UNIQUE / FK) -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violación de integridad de datos en {}: {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.CONFLICT.value(),
            "Conflicto de Datos",
            "La operación no se pudo completar porque viola una restricción de unicidad o clave foránea",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // 5. Método HTTP no soportado (ej: POST en endpoint GET) -> 405 Method Not Allowed
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "Método no permitido",
            String.format("El método HTTP '%s' no está soportado para esta ruta", ex.getMethod()),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }
    
    // 6. Handler general para cualquier error no previsto -> 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllUncaughtExceptions(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}: ", request.getRequestURI(), ex);
        
        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Error Interno del Servidor",
            "Ocurrió un error inesperado en el servidor. Por favor, intente más tarde",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    // 7. Credenciales inválidas en autenticación -> 401 Unauthorized
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> handleCredencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request) {
        log.warn("Intento de acceso fallido en {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponseDTO error = new ErrorResponseDTO(
            HttpStatus.UNAUTHORIZED.value(),
            "No Autorizado",
            ex.getMessage(), // Aquí viajará el mensaje "Contraseña incorrecta" o "Consultor no encontrado"
            request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

}
