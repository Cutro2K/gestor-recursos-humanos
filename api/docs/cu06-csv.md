# Parseo CSV de candidatos (CU06)

`GestorCandidatos` lee un CSV UTF-8 (con o sin BOM), separado por punto y coma
(`;`). El parseo es interno al servicio y cierra el stream recibido.

La primera fila debe contener exactamente estos nombres, sin las aclaraciones
de formato entre paréntesis del enunciado:

```csv
Nro candidato;Apellido;Nombre;Nro documento;Tipo documento;Fecha de nacimiento;Género;Nacionalidad;Email;Escolaridad;Eliminado
1;Pérez;Ana;00123456;DNI;1996-02-29;M;Argentina;ana@example.test;Universitario;False
```

Se admite cambiar el orden de las columnas. Los nombres del encabezado se comparan
sin distinguir mayúsculas ni tildes y se eliminan espacios externos.

Todos los campos son obligatorios:

| Campo | Formato |
| --- | --- |
| Nro candidato | Entero positivo representable por `Long` |
| Apellido / Nombre | Texto de hasta 50 caracteres cada uno |
| Nro documento | Texto de hasta 20 caracteres; conserva ceros iniciales y admite pasaportes alfanuméricos |
| Tipo documento | `DNI`, `LE`, `LC` o `PP` |
| Fecha de nacimiento | Fecha real con formato exacto `aaaa-mm-dd` |
| Género | `H` o `M` |
| Nacionalidad | Texto de hasta 50 caracteres |
| Email | Email válido de hasta 255 caracteres |
| Escolaridad | Texto de hasta 50 caracteres |
| Eliminado | `True` o `False` |

Tipo de documento, género y booleanos se aceptan sin distinguir mayúsculas.
Se eliminan espacios externos de los valores. Se admiten LF y CRLF, campos
entre comillas con `;` o saltos de línea, y comillas escapadas como `""`.
Las líneas vacías se omiten después del encabezado.

El parseo conserva cada fila con sus valores originales, sus datos convertidos
cuando son válidos y sus errores (fila, campo y mensaje). Una fila inválida puede
generar varios errores; las demás se siguen procesando.

El resumen cuenta cuántas filas distintas fallan por campo y cuántas tienen algún
problema. Varios errores del mismo campo en una fila se cuentan una sola vez.
La fila es la línea física donde comienza el registro, contando el encabezado
como línea 1.

Un archivo vacío, un encabezado inválido o sintaxis CSV corrupta generan
`FormatoCsvException`. Los fallos de lectura pueden producir `IOException`.
No se devuelve un resultado parcial ante estos fallos.

El parser no persiste ni decide actualizaciones, duplicados o bajas:
`Eliminado` se interpreta como dato del archivo.

## Flujo acordado para CU06

1. En la página Candidatos, el botón **Importar candidatos** abre un modal.
2. El modal permite arrastrar un CSV o seleccionarlo desde el equipo.
3. Se solicita una previsualización al backend. Cada fila válida indica la acción
   prevista: creación (verde), actualización (amarillo) o eliminación (rojo).
   Las filas rechazadas conservan sus valores y errores para poder revisarlas.
4. Se muestra el resumen de filas con problemas en cada campo.
5. El usuario confirma con el botón de finalizar. Se importan **solo las filas
   válidas** y se informa cuáles se rechazaron, junto con los totales realizados.

El frontend implementa este flujo como un modal dentro de `/candidatos`.
No se crea `candidatos/importar/page.tsx`: la importación conserva la URL de
Candidatos y los componentes del modal viven en `src/components/candidatos/`.
Los endpoints HTTP delegan en los métodos del servicio que se describen a continuación.

La página consulta candidatos activos, permite filtrar por apellido, nombre y
número, y carga páginas adicionales por cursor. Al terminar la importación se
actualiza el listado conservando los filtros aplicados.

El modal utiliza `Modal`, `Button`, `Surface`, `Stack`, `Tabs`, `Spinner` y
`useToast` de ReactUILib. La tabla y el área de arrastre utilizan HTML semántico.
Los colores de las acciones también se acompañan con etiquetas de texto.
La preview muestra 25 filas por página, permite filtrar por acción y abrir el
detalle de los 11 campos y sus errores. El reporte final también se pagina.

Se valida en el navegador que se seleccione un solo CSV no vacío y de hasta
2 MiB; el backend vuelve a validar su contenido. No se puede confirmar sin filas
válidas. Durante el guardado se deshabilitan los controles de confirmación,
cambio de archivo y cierre para evitar envíos repetidos. Ante un fallo de
confirmación se actualiza el listado y se solicita volver a analizar el archivo,
sin reintentos automáticos.

El CSV de ejemplo está disponible en `/candidatos-ejemplo.csv` desde el propio modal.

## Previsualización y confirmación en el servicio

Los DTO públicos de esta funcionalidad son:

- `PrevisualizacionCandidatosDTO`: contiene la preview. Agrupa los tipos anidados
  `Fila`, `Accion`, `ErrorFila` y `ResumenErrores`.
- `ResultadoImportacionCandidatosDTO`: contiene los totales y errores después del
  guardado; reutiliza los tipos de error y resumen de la previsualización.
- `ListadoCandidatosDTO`: contiene una página de candidatos activos y el cursor
  para continuar. Su tipo `CandidatoResumen` está anidado en el mismo archivo.

Las estructuras de datos convertidos, fila parseada y resultado de parseo son
records internos de `GestorCandidatos`. El controller utiliza los métodos públicos
de previsualización, importación y listado y sus DTO de respuesta.

`previsualizarCSV(InputStream)` consulta la base sin guardar ni modificar entidades.
Devuelve las filas con `accion` (`CREAR`, `ACTUALIZAR`, `ELIMINAR`, `RECHAZAR`),
sus errores y los contadores `aCrear`, `aActualizar`, `aEliminar`, `rechazadas`.
`resumenErrores` incluye `filasConErrores` y `filasPorCampo`.

La identidad se determina por **Nro candidato del CSV**:

- Si no existe y `Eliminado=False`, se crea.
- Si existe y `Eliminado=False`, se actualizan sus datos y queda activo.
- Si existe y `Eliminado=True`, se aplica baja lógica conservando sus otros datos.
- Si no existe y `Eliminado=True`, se rechaza la fila.
- Los candidatos ausentes del CSV permanecen sin cambios.

Se rechazan todas las apariciones de números repetidos entre filas de formato
válido. También se rechazan documentos repetidos entre filas de alta o actualización,
y documentos que la base asigna a otro número de candidato, incluidas las bajas
lógicas. El email no se utiliza como identidad ni se exige que sea único.

`importarCSV(InputStream)` es la confirmación: recibe nuevamente el archivo,
vuelve a parsearlo y consulta el estado actual de la base. Las acciones calculadas
en una previsualización anterior no se toman como autorización de escritura.
Guarda las filas válidas en una transacción y devuelve `creados`, `actualizados`,
`eliminados`, `rechazados`, los errores y su resumen. Los contadores corresponden a
filas procesadas por acción: una actualización con valores iguales o una baja ya
aplicada siguen contando en su categoría.

Los errores de filas permiten importar las restantes. Los errores de estructura
o lectura del archivo interrumpen la confirmación antes de guardar. Un fallo de
persistencia (por ejemplo, una restricción única por un cambio concurrente) revierte
todo el lote válido y se propaga; no se devuelve un resumen de éxito parcial.

Pruebas unitarias sin base de datos:
`mvn -Dtest=GestorCandidatosTests,ImportacionCandidatosTests test`.
Las pruebas de integración del servicio usan H2 en memoria.

## Endpoints HTTP

Base local: `http://localhost:8080/api/candidatos`.
Las respuestas utilizan JSON y el formato existente `ErrorResponseDTO` para
errores HTTP (`timestamp`, `status`, `error`, `message`, `path` y, cuando aplica,
`erroresValidacion`). Las rutas se mantienen bajo `/api`; las extensiones futuras
del contrato deben ser aditivas.

### Previsualizar y confirmar

| Método | Ruta | Resultado con HTTP 200 |
| --- | --- | --- |
| POST | `/previsualizar` | `PrevisualizacionCandidatosDTO`, sin persistir |
| POST | `/importar` | `ResultadoImportacionCandidatosDTO`, después de guardar las filas válidas |

Ambas rutas reciben `multipart/form-data` con un archivo en el campo `archivo`.
Debe tener extensión `.csv` (sin distinguir mayúsculas) y contenido UTF-8.
Se acepta el MIME enviado por el navegador; el parser valida el contenido.
El archivo puede ocupar hasta 2 MiB y la petición multipart completa hasta 3 MiB,
según `spring.servlet.multipart.max-file-size` y `max-request-size`.

```bash
curl -F 'archivo=@candidatos.csv' http://localhost:8080/api/candidatos/previsualizar
curl -F 'archivo=@candidatos.csv' http://localhost:8080/api/candidatos/importar
```

La confirmación debe volver a enviar el archivo. Las acciones de una preview
anterior no se reciben como instrucciones de escritura: se calculan nuevamente
contra la base. Reenviar un CSV vuelve a procesarlo por número de candidato y
no crea duplicados de ese número; no hay caché de respuestas de importación y
las estadísticas de un reintento pueden cambiar (por ejemplo, un alta pasa a
contar como actualización).

Los errores de filas se devuelven con HTTP 200 dentro del reporte, incluso si
todas fueron rechazadas. El consumidor debe revisar `rechazadas` en la preview
y `rechazados` en la confirmación, además de los totales de operaciones.

### Listar candidatos para CU25

`GET /api/candidatos` devuelve candidatos activos ordenados por ID ascendente.

| Parámetro opcional | Comportamiento |
| --- | --- |
| `apellido` | Contiene el texto, sin distinguir mayúsculas; máximo 50 caracteres |
| `nombre` | Contiene el texto, sin distinguir mayúsculas; máximo 50 caracteres |
| `nroCandidato` | Coincidencia exacta; entero positivo |
| `limite` | Tamaño de página: 50 por defecto, entre 1 y 100 |
| `cursor` | Último ID de la página anterior; entero positivo |

Los filtros se combinan con AND, omiten espacios externos y tratan `%`, `_` y
`!` como texto literal. Filtros de texto vacíos no restringen resultados.

```bash
curl --get http://localhost:8080/api/candidatos \
  --data-urlencode 'apellido=Pérez' \
  --data-urlencode 'limite=50'
```

Ejemplo de una página con más resultados:

```json
{
  "candidatos": [
    {
      "id": 21,
      "nroCandidato": 100,
      "apellido": "Pérez",
      "nombre": "Ana",
      "tipoDocumento": "DNI",
      "nroDocumento": "00123456",
      "email": "ana@example.test"
    }
  ],
  "siguienteCursor": 21
}
```

Para continuar, enviar `cursor=21` conservando los filtros y el límite.
`siguienteCursor` es `null` cuando no hay más resultados. Una búsqueda sin
coincidencias devuelve `{"candidatos": [], "siguienteCursor": null}` con HTTP 200.
El cursor utiliza el ID interno, no el número de candidato del CSV.

### Errores HTTP

| Estado | Situación |
| --- | --- |
| 400 | Archivo faltante o vacío, extensión/UTF-8/estructura CSV inválidos, parámetros de consulta inválidos o multipart mal formado |
| 406 | El cliente solicita en `Accept` un formato de respuesta incompatible |
| 409 | Fallo de integridad al guardar, por ejemplo por un cambio concurrente; se revierte el lote válido |
| 413 | Archivo o petición multipart por encima del límite configurado |
| 415 | `Content-Type` incompatible; las cargas deben usar `multipart/form-data` |
| 500 | Error interno inesperado o fallo técnico de lectura |

`CandidatoControllerTests` prueba las rutas con un servidor HTTP real en un puerto
aleatorio y una base H2 en memoria, incluidos los límites de carga multipart.
