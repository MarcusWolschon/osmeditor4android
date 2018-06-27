# Editor de horarios de apertura de OpenStreetMap

La especificación de horarios de apertura en OpenStreetMap es bastante compleja y no permite una interfaz de usuario simple e intuitiva.

Sin embargo, en la mayoría de ocasiones basta con usar una pequeña parte de la especificación. El editor tiene esto en cuenta y trata de ocultar las características menos intuitivas en menús, reduciendo la mayor parte del tiempo su uso a realizar pequeños cambios en plantillas predefinidas.

_Esta documentación es preliminar y está en progreso_

## Uso del editor de horarios de apertura

En un flujo de trabajo típico, el objeto que está editando ya tendrá una etiqueta de horas de apertura (opening_hours, service_times y collection_times) o puede volver a aplicar el ajuste preestablecido para el objeto para obtener un campo de horario de apertura vacío. Si necesita agregar el campo manualmente y está utilizando Vespucci, puede ingresar la clave en la página de detalles y luego volver a la pestaña basada en el formulario para editar. Si cree que la etiqueta de las horas de apertura debería haber sido parte del ajuste preestablecido, abra una publicación para su editor.

Si ha definido una plantilla predeterminada (haga esto mediante el artículo del menú «Administrar Plantillas») se cargará automáticamente cuando el editor se inicie con un valor vacío. Con la función «Cargar plantilla» puede cargar cualquier plantilla guardada y con el menú «Guardar plantilla» puede guardar el valor actual como una plantilla. Puede definir plantillas separadas y valores predeterminados para las etiquetas «opening_hours», «collection_times» y »service_times». 

Naturalmente, puedes crear un valor de horarios de apertura desde cero, pero recomendamos usar una de las plantillas existentes para empezar.

Si se carga un valor existente, se intentará autocorregir para ajustarlo a las especificaciones de horarios de apertura. Si no es posible, la ubicación aproximada del error se marcará el la pantalla del valor en texto plano de los horarios de apertura, y podrás intentar corregirlo manualmente. Aproximadamente, una cuarta parte de los valores de horarios de apertura en la base de datos de OpenStreetMap tienen problemas, pero menos del 10% no son corregibles; ver [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) para más información sobre qué desviaciones sobre las especificaciones son toleradas.

### Botón del menú principal

* __Añadir regla__: añadir una nueva regla.
* __Añadir regla para vacaciones__: añada una nueva regla para unas vacaciones junto con un cambio de estado.
* __Añadir regla para 24/7__: añada una regla para un objeto que siempre está abierto, la especificación de horas de apertura no admite ningún otro valor secundario para 24/7 sin embargo, sí permitimos añadir selectores de mayor nivel (por ejemplo, rangos de año).
* __Cargar plantilla__: cargue una plantilla existente.
* __Guardar a plantilla__: guarde el valor actual de las horas de apertura como una plantilla para uso futuro.
* __Administrar plantilla__: editar, por ejemplo cambiar el nombre, y eliminar plantillas existentes.
* __Refrescar__: volver a analizar el valor de la hora de apertura.

### Reglas

Se añaden reglas predeterminadas como_reglas_ normales, esto implica que anularán los valores de las reglas anteriores para los mismos días. Esto puede ser una preocupación al especificar tiempos extendidos, por lo general, querrá cambiar las reglas a través de la entrada del menú _Mostrar tipo de regla_ a _aditivo_.

#### Menú Regla

* __Añadir modificador/comentario__: cambia el efecto de esta regla y añade un comentario opcional.
* __Añadir vacaciones__: añadir un selector para vacaciones públicas o escolares.
* __Añadir período de tiempo...__
    * __Hora - tiempo__: una hora de inicio para una hora de finalización en el mismo día.
    * __Hora - tiempo prolongado__: una hora de inicio a un tiempo de finalización al día siguiente (ejemplo 26:00 es 02:00 (am) el día siguiente.
    * __Var. hora - hora__: desde una hora variable de inicio (amanecer, atardecer, amanecer y atardecer) a una hora de finalización en el mismo día.
    * __Var. hora - tiempo prolongado__: desde una hora variable de inicio hasta una hora de finalización al día siguiente.
    * __Hora - var. tiempo__: una hora de inicio a una hora variable final.
    * __Var. time - var. time__: un tiempo variable de inicio a un tiempo variable final.
    * __Time__: un punto en el tiempo.
    * __Time-open end__: desde un punto de inicio en el tiempo en adelante.
    * __Variable time__: en el tiempo variable
    * __Variable time-open end__: a partir de un tiempo variable de inicio en adelante
* __Add week day range__: añade un selector basado en el día de la semana.
* __Añadir rango de fechas...__
    * __Fecha - fecha__: desde una fecha de inicio (año, mes, día) hasta una fecha final.
    * __Fecha variable - fecha__: desde una fecha variable de inicio (actualmente la especificación solo define _Pascua_) hasta una fecha final.
    * __Fecha - fecha variable__: desde una fecha de inicio hasta una fecha variable.
    * __Fecha variable - fecha variable__: desde una fecha variable de inicio hasta una fecha variable final.
    * __Ocurrencia en mes - ocurrencia en mes__: desde una ocurrencia de inicio de día de semana en un mes al mismo.
    * __Ocurrencia en mes - fecha__: desde una ocurrencia de un día laborable en un mes hasta una fecha final.
    * __Fecha - ocurrencia en mes__: desde una fecha de inicio hasta una ocurrencia de fin de día de la semana en un mes.
    * __Ocurrencia en mes - fecha variable__: desde una ocurrencia de un día laborable de inicio en un mes a una fecha variable final.
    * __Fecha variable - ocurrencia en mes__: desde una fecha variable de inicio hasta el final de un a ocurrencia de día de la semana en un mes.
    * __Fecha - final abierto__: desde una fecha de inicio en adelante,
    * __Fecha variable - final abierto__: desde una fecha de inicio variable en adelante.
    * __Ocurrencia en mes - final abierto__: a partir de una ocurrencia de un día de la semana en un mes en adelante.
    * __Con compensaciones...__: las mismas entradas que arriba pero con compensaciones especificadas (esto no se utiliza a menudo).
* __Añadir rango de año__: añadir un selector basado en el año.
* __Añadir rango de semana__: agregar un selector basado en el número de la semana.
* __Mostrar tipo de regla__: mostrar y permitir el cambio del tipo de regla _normal_, _additive_ y _fallback_ (no disponible en la primera regla).
* __Ascender__: subir esta regla una posición (no disponible en la primera regla).
* __Bajar__: bajar esta regla una posición.
* __Eliminar__: eliminar esta regla.

### Lapsos de tiempo

Para que el tiempo de edición sea lo más fácil posible, tratamos de elegir un rango de tiempo y una granularidad óptimos para las barras de rango al cargar valores existentes. Para nuevos tiempos, las barras comienzan a las 6:00 (am) y tienen incrementos de 15 minutos, esto se puede cambiar a través del menú.

#### Menú de lapso de tiempo

* __Mostrar el selector de tiempo__: mostrar un selector de número grande para seleccionar la hora de inicio y finalización, en pantallas muy pequeñas esta es la forma preferida de cambiar los horarios.
* __Cambiar a marcas de 15 minutos__: use una granularidad de 15 minutos para la barra de rango.
* __Cambiar a ticks de 5 minutos__: use una granularidad de 5 minutos para la barra de rango.
* __Cambiar a marcas de 1 minuto__: use una granularidad de 1 minuto para la barra de rango, muy difícil de usar en un teléfono.
* __Comienzar a la medianoche__: iniciar la barra de rango a la medianoche.
* __Mostrar intervalo__: mostrar el campo de intervalo para especificar un intervalo en minutos.
* __Eliminar__: eliminar este lapso de tiempo.

