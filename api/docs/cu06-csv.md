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

Los colores, el modal y la selección de archivos corresponden a las futuras
subtareas de frontend. Los endpoints corresponden a la subtarea de controller.
Este cambio implementa los métodos de servicio que van a utilizar.

## Previsualización y confirmación en el servicio

Los únicos DTO públicos de esta funcionalidad son:

- `PrevisualizacionCandidatosDTO`: contiene la preview. Agrupa los tipos anidados
  `Fila`, `Accion`, `ErrorFila` y `ResumenErrores`.
- `ResultadoImportacionCandidatosDTO`: contiene los totales y errores después del
  guardado; reutiliza los tipos de error y resumen de la previsualización.

Las estructuras de datos convertidos, fila parseada y resultado de parseo son
records internos de `GestorCandidatos`. El controller solo necesita los dos
métodos públicos siguientes y sus DTO de respuesta.

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
