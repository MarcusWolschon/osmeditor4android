# Editor de horario de apertura de OpenStreetMap

La especificación de horario de apertura en OpenStreetMap es bastante compleja y no se presta fácilmente a una interfaz de usuario simple e intuitiva.

Sin embargo, la mayoría de las veces probablemente solo usarás una pequeña parte de la definición. El editor tiene esto en cuenta al intentar ocultar las funciones más confusas en los menús y, la mayoría de las veces, reduce el uso "en la calle" a pequeñas personalizaciones de plantillas predefinidas.

_Esta documentación es preliminar y un trabajo en progreso._

## Uso del editor de horario de apertura

En un flujo de trabajo típico, el objeto que estás editando ya tendrá una etiqueta de horario de apertura (`opening_hours`, `service_times` y `collection_times`) o puedes volver a aplicar el preajuste para el objeto y obtener un campo de horario de apertura vacío. Si necesitas agregar el campo manualmente y estás usando Vespucci, puedes ingresar la clave en la página de detalles y luego volver a la pestaña basada en formulario para editar. Si crees que la etiqueta de horario de apertura debería haber sido parte del preajuste, abre un reporte de error para tu editor.

Si has definido una plantilla predeterminada (hazlo a través del elemento del menú "Administrar plantillas"), se cargará automáticamente cuando se inicie el editor con un valor vacío. Con la función "Cargar plantilla" puedes cargar cualquier plantilla guardada y con el menú "Guardar plantilla" puedes guardar el valor actual como plantilla. Puedes definir plantillas separadas y valores predeterminados para claves específicas, por ejemplo, "opening_hours", "collection_times" y "service_times" o valores personalizados. Además, puedes limitar la aplicabilidad de una plantilla a una región y un identificador específico, generalmente una etiqueta de nivel superior de OSM (por ejemplo, `amenity=restaurant`).

Naturalmente, puedes crear un valor de horario de apertura desde cero, pero recomendamos usar una de las plantillas existentes como punto de partida.

Si se carga un valor de horario de apertura existente, se intentará corregirlo automáticamente para que se ajuste a la especificación de horario de apertura. Si eso no es posible, se resaltará la ubicación aproximada donde se produjo el error en la visualización del valor bruto del horario de apertura, y puedes intentar corregirlo manualmente. Aproximadamente una cuarta parte de los valores de horario de apertura en la base de datos de OpenStreetMap tienen problemas, pero menos del 10 % no se pueden corregir; consulta [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) para obtener más información sobre qué desviaciones de la especificación se toleran.

### Botón del menú principal

* __Añadir regla__: añade una nueva regla.
* __Añadir regla para días festivos__: añade una nueva regla para un día festivo junto con un cambio de estado.
* __Añadir regla para 24/7__: añade una regla para un objeto que siempre está abierto; la especificación de horario de apertura no admite ningún otro subvalor para 24/7; sin embargo, sí permitimos añadir selectores de nivel superior (por ejemplo, rangos de años).
* __Cargar plantilla__: carga una plantilla existente.
* __Guardar a plantilla__: guarda el valor actual del horario de apertura como una plantilla para uso futuro.
* __Administrar plantillas__: edita (por ejemplo, cambia el nombre) y elimina las plantillas existentes.
* __Actualizar__: vuelve a analizar el valor del horario de apertura.
* __Eliminar todo__: elimina todas las reglas.

### Reglas

Las reglas predeterminadas se añaden como reglas _normales_; esto implica que anularán los valores de las reglas anteriores para los mismos días. Esto puede ser un problema al especificar horarios extendidos; por lo general, en ese caso querrás cambiar las reglas a _aditivas_ a través de la entrada del menú _Mostrar tipo de regla_.

#### Menú Regla

* __Añadir modificador/comentario__: cambia el efecto de esta regla y añade un comentario opcional.
* __Añadir día festivo__: añade un selector para días festivos o vacaciones escolares.
* __Añadir intervalo de tiempo...__
    * __Hora - Hora__: una hora de inicio y una hora de fin en el mismo día.
    * __Hora - Hora extendida__: una hora de inicio hasta una hora de fin al día siguiente (por ejemplo, 26:00 es las 02:00 (a. m.) del día siguiente).
    * __Hora var. - Hora__: desde una hora de inicio variable (amanecer, atardecer, alba y crepúsculo) hasta una hora de fin en el mismo día.
    * __Hora var. - Hora extendida__: desde una hora de inicio variable hasta una hora de fin al día siguiente.
    * __Hora - Hora var.__: una hora de inicio y una hora de fin variable.
    * __Hora var. - Hora var.__: una hora de inicio variable y una hora de fin variable.
    * __Hora__: un punto en el tiempo.
    * __Hora - Fin abierto__: desde un punto de inicio en el tiempo en adelante.
    * __Hora variable__: a la hora variable.
    * __Hora variable - Fin abierto__: desde una hora de inicio variable en adelante.
* __Añadir rango de días de la semana__: añade un selector basado en el día de la semana.
* __Añadir rango de fechas...__
    * __Fecha - Fecha__: desde una fecha de inicio (año, mes, día) hasta una fecha de fin.
    * __Fecha variable - Fecha__: desde una fecha de inicio variable (actualmente la especificación solo define _Pascua_) hasta una fecha de fin.
    * __Fecha - Fecha variable__: desde una fecha de inicio hasta una fecha variable.
    * __Fecha variable - Fecha variable__: desde una fecha de inicio variable hasta una fecha de fin variable.
    * __Ocurrencia en mes - Ocurrencia en mes__: desde una ocurrencia de día de la semana de inicio en un mes hasta la misma.
    * __Ocurrencia en mes - Fecha__: desde una ocurrencia de día de la semana de inicio en un mes hasta una fecha de fin.
    * __Fecha - Ocurrencia en mes__: desde una fecha de inicio hasta una ocurrencia de día de la semana de fin en un mes.
    * __Ocurrencia en mes - Fecha variable__: desde una ocurrencia de día de la semana de inicio en un mes hasta una fecha variable de fin.
    * __Fecha variable - Ocurrencia en mes__: desde una fecha variable de inicio hasta una ocurrencia de día de la semana de fin en un mes.
    * __Fecha - Fin abierto__: desde una fecha de inicio en adelante.
    * __Fecha variable - Fin abierto__: desde una fecha de inicio variable en adelante.
    * __Ocurrencia en mes - Fin abierto__: desde una ocurrencia de un día de la semana en un mes en adelante.
    * __Con desfases…__: las mismas entradas que las anteriores, pero con desfases especificados (esto se utiliza con poca frecuencia).
* __Añadir rango de años…__    
    * __Añadir rango de años__: añade un selector basado en el año.
    * __Añadir año de inicio__: añade un rango de años con final abierto.
* __Añadir rango de semana__: agrega un selector basado en el número de semana.
* __Duplicar__: crea una copia de esta regla y la inserta después de la posición actual.
* __Mostrar tipo de regla__: muestra y permite cambiar el tipo de regla: _normal_, _aditiva_ y _de reserva_ (no disponible en la primera regla).
* __Ascender__: sube esta regla una posición (no disponible en la primera regla).
* __Bajar__: baja esta regla una posición.
* __Eliminar__: elimina esta regla.

### Intervalos de tiempo

Para que la edición de intervalos de tiempo sea lo más sencilla posible, intentamos elegir un rango de tiempo y una granularidad óptimos para las barras de rango al cargar valores existentes. Para intervalos de tiempo nuevos, las barras comienzan a las 6:00 (a. m.) y tienen incrementos de 15 minutos; esto se puede cambiar a través del menú.

Al hacer clic en la barra de tiempo (no en los marcadores), se abrirá el selector de tiempo grande, cuando el uso directo de las barras sea demasiado difícil. Los selectores de tiempo se extienden hasta el día siguiente, por lo que son una forma sencilla de extender un rango de tiempo sin tener que eliminar y volver a agregar el rango.

#### Menú de intervalo de tiempo

* __Mostrar selector de tiempo__: muestra un selector de tiempo grande para seleccionar la hora de inicio y finalización; en pantallas muy pequeñas, esta es la forma preferida de cambiar las horas.
* __Cambiar a intervalos de 15 minutos__: usa una granularidad de 15 minutos para la barra de rango.
* __Cambiar a intervalos de 5 minutos__: usa una granularidad de 5 minutos para la barra de rango.
* __Cambiar a intervalos de 1 minuto__: usa una granularidad de 1 minuto para la barra de rango (muy difícil de usar en un teléfono).
* __Comenzar a medianoche__: inicia la barra de rango a medianoche.
* __Mostrar intervalo__: muestra el campo de intervalo para especificar un intervalo en minutos.
* __Eliminar__: elimina este intervalo de tiempo.

### Administrar plantillas

El cuadro de diálogo de administración de plantillas te permite agregar, editar y eliminar plantillas.

En Android 4.4 y versiones posteriores, la siguiente funcionalidad adicional está disponible en el botón de menú.

* __Mostrar todo__: muestra todas las plantillas en la base de datos.
* __Guardar en archivo__: escribe el contenido de la base de datos de plantillas en un archivo.
* __Cargar desde archivo (reemplazar)__: carga plantillas desde un archivo reemplazando el contenido actual de la base de datos.
* __Cargar desde archivo__: carga plantillas desde un archivo conservando el contenido actual.

#### Cuadros de diálogo para guardar y editar plantillas

El cuadro de diálogo te permite configurar:

* __Nombre__: un nombre descriptivo para la plantilla.
* __Predeterminado__: si está marcado, se considerará como una plantilla predeterminada (normalmente más restringida por los otros campos).
* __Clave__: la clave para la que esta plantilla es relevante; si se establece en _Clave personalizada_, puedes agregar un valor no estándar en el campo de abajo. Los valores de clave admiten comodines SQL, es decir, _%_ coincide con cero o más caracteres, *_* coincide con un solo carácter. Ambos caracteres comodín se pueden escapar con _\\_ para coincidencias literales.
* __Región__: la región a la que se aplica la plantilla.
* __Objeto__: una cadena específica de la aplicación para usar en la coincidencia.

