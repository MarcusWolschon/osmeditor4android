# Introducción a Vespucci

Vespucci es un completo editor de OpenStreetMap que soporta la mayoría de operaciones que proveen los editores de escritorio. Ha sido probado con éxito en Android 2.3 a 7.0 de Google y varias variantes basadas en AOSP. Un aviso: aunque las capacidades de los dispositivos móviles han alcanzado a sus rivales de escritorio, particularmente los dispositivos más antiguos tienen memoria disponible muy limitada y tienden a ser bastante lentos. Debería tener esto en cuenta cuando utilice Vespucci y mantenga, por ejemplo, el tamaño de las áreas que está editando en un tamaño razonable. 

## Utilizar por primera vez

Al iniciarse Vespucci le muestra el diálogo "Descargar otra ubicación"/"Cargar área". Si usted tiene coordenadas expuestas y quiere descargar inmediatamente, puede seleccionar la opción apropiada e introducir el radio de alrededor de la ubicación que quiera descargar. No seleccione un área amplia en dispositivos lentos. 

Como alternativa, puede cerrar el diálogo pulsando el botón «Ir al mapa» y alejar o acercar a una ubicación que quiera editar y descargar los datos a continuación (vea más adelante: «Editando con Vespucci»).

## Editando con Vespucci

Dependiendo del tamaño de pantalla y antigüedad de su dispositivo, las acciones de edición pueden estar accesibles directamente por medio de iconos en la barra superior, en un menú desplegable a la derecha de la barra superior, desde la barra inferior (si está presente) o por medio de la tecla de menú.

<a id="download"></a>

## Descargando datos OSM

Seleccione el ícono de transferencia ![Transferir](../images/menu_transfer.png) o la opción del menú «Transferir». Esto mostrará siete opciones:

* **Descargar la vista actual**: descarga el área visible en la pantalla y reemplaza cualquier información existente *(requiere conexión de red)*
* **Añadir la vista actual a descarga**: descarga el área visible en la pantalla y la combina con la información existente *(requiere conexión de red)*
* **Descargar otra ubicación**: muestra un formulario que le permite introducir coordenadas, buscar una ubicación o utilizar la ubicación actual, y después descargar un área de alrededor de la ubicación *(requiere conexión de red)*
* **Subir datos al servidor OSM**: carga ediciones a OpenStreetMap *(requiere autenticación)* *(requiere conexión de red)*
* **Autodescarga**: descarga un área alrededor de la ubicación actual automáticamente *(requiere conexión de red)* *(requiere GPS)*
* **Archivo...**: Guardar y cargar información OSM a/desde archivos del dispositivo.
* **Nota/Errores...**: descarga (automática y manualmente) Notas y "Errores" OSM desde herramientas QA (actualmente OSMOSE) *(requiere conexión de red)*

La manera más fácil de descargar información al dispositivo es hacer zoom y marcar la ubicación que quiere editar y después seleccionar "Descargar vista actual". Puede hacer zoom utilizando gestos, como los botones de zoom o los botones de control de volumen del dispositivo. Vespucci debería descargar entonces información de la vista actual. No se requiere autenticación para descargar información a su dispositivo.

## Editando

<a id="lock"></a>

#### Lock, unlock, mode switching

Para evitar ediciones accidentales, Vespucci comienza en modo "bloqueado", un modo que sólo permite hacer zoom y mover el mapa. Toque el icono ![Bloqueado](../images/locked.png) para desbloquear la pantalla. 

A long press on the lock icon will display a menu currently offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Pulsación simple, pulsación doble y pulsación larga

Por defecto, los nodos y vías selecionables tienen un área naranja a su alrededor indicando aproximadamente dónde tiene que tocar para seleccionar un objeto. Tiene tres opciones:

* Pulsación simple: Selecciona objeto. 
    * Un nodo/vía aislado se marca inmediatamente. 
    * Sin embargo, si intenta seleccionar un objeto y Vespucci determina que la selección podría referirse a varios objetos, se presentará un menú de selección, dándole la posibilidad de escoger el objeto que quiera seleccionar. 
    * Los objetos seleccionados se marcan en amarillo. 
    * Para más información, véase [Nodo seleccionado](../en/Node%20selected.md), [Vía seleccionada](../en/Way%20selected.md) y [Relación seleccionada](../en/Relation%20selected.md).
* Pulsación Doble: Inicia el [Modo de multiselección](../en/Multiselect.md)
* Pulsación larga: Crea un "punto de mira", dándole la posibilidad de añadir nodos; véase más adelante y en [Crear nuevos objetos](../en/Creating%20new%20objects.md)

Es una buena estrategia acercar el mapa si intenta editar un área con alta densidad.

Vespucci tiene un buen sistema «deshacer/rehacer» así que no tenga miedo de experimentar en su dispositivo, sin embargo no suba ni guarde los datos de prueba.

#### Seleccionando / Deseleccionando (pulsación simple y "menú de selección")

Toque un objeto para seleccionarlo y resaltarlo. Tocando la pantalla en una región vacía lo deseleccionará. Si ha seleccionado un objeto y necesita seleccionar otra cosa, simplemente toque el objeto en cuestión; no es necesario deseleccionar primero. Un doble toque en un objeto iniciará el [Modo multiselección](../en/Multiselect.md).

Tenga en cuenta que si intenta seleccionar un objeto y Vespucci determina que la selección se podría referir a varios objetos (tales como un nodo en una vía u otros objetos superpuestos) se presentará un menú de selección: pulse en el objeto que quiera seleccionar y el objeto se seleccionará. 

Los objetos seleccionados se indican con un fino borde amarillo. Puede que el borde amarillo sea difícil de ver, dependiendo en el fondo del mapa y el nivel de zoom. Una vez que se ha hecho una selección, verá una notificación confirmando la selección.

Una vez que la selección se ha completado, verá (como botones o como items de menú) una lista de operaciones soportadas para el objeto seleccionado: para más información véase [Node seleccionado](../en/Node%20selected.md), [Vía selected](../en/Way%20selected.md) y [Relación seleccionada](../en/Relation%20selected.md).

#### Objetos seleccionados: Editando etiquetas

Un segundo toque en el objeto seleccionado abre el editor de etiquetas y puede editar las etiquetas asociadas al objeto.

Tenga en cuenta que para superponer objetos (como un nodo en un camino) el menú de selección vuelve a subir por segunda vez. Al seleccionar el mismo objeto, aparece el editor de etiquetas; seleccionar otro objeto simplemente selecciona el otro objeto.

#### Objetos seleccionados: Moviendo un nodo o vía

Una vez que haya seleccionado un objeto, puede moverlo. Tenga en cuenta que los objetos sólo se pueden arrastrar/mover cuando se seleccionan. Simplemente arrastre cerca (es decir, dentro de su zona de tolerancia) el objeto seleccionado para moverlo. Si selecciona el área de arrastre grande en las preferencias, obtiene un área grande alrededor del nodo seleccionado que facilita la colocación del objeto. 

#### Añadiendo un nuevo nodo/punto o vía (pulsación larga)

Mantenga presionada la tecla donde quieras que esté el nodo o la forma de comenzar. Verás un símbolo negro de «cruz». 
* Si desea crear un nuevo nodo (no conectado a un objeto), haga clic fuera de los objetos existentes.
* Si desea extender un camino, haga clic dentro de la «zona de tolerancia» del camino (o de un nodo en el camino). La zona de tolerancia está indicada por las áreas alrededor de un nodo o camino.

Una vez pueda ver el símbolo de la cruz, tiene estas opciones:

* Toque en el mismo lugar.
    * Si el punto de mira no está cerca de un nodo, tocar nuevamente la misma ubicación crea un nuevo nodo. Si usted está cerca de un camino (pero no cerca de un nodo), el nuevo nodo estará en camino (y conectado al camino).
    * Si la cruz está cerca de un nodo (es decir, dentro de la zona de tolerancia del nodo), al tocar la misma ubicación solo se selecciona el nodo (y se abre el editor de etiquetas. No se crea un nuevo nodo. La acción es la misma que la selección anterior.
* Toque otro lugar. Al tocar otra ubicación (fuera de la zona de tolerancia de la cruz) se añade un segmento de camino desde la posición original a la posición actual. Si la cruz estaba cerca de un camino o nodo, el nuevo segmento se conectará a ese nodo o camino.

Simplemente toque la pantalla donde desea agregar más nodos del camino. Para finalizar, toque el nodo final dos veces. Si el nodo final está ubicado en una ruta o nodo, el segmento se conectará automáticamente a la ruta o al nodo. 

También puede usar un elemento del menú: Vea [Creando nuevos objetos](../en/Creating%20new%20objects.md) para más información.

#### Añadiendo un Área

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _rutas cerradas («polígonos»)_: la variante de área más simple y más común, son rutas que tienen un primer y último nodo compartido formando un «anillo» cerrado (por ejemplo, la mayoría de los edificios son de este tipo). Estos son muy fáciles de crear en Vespucci, simplemente conéctese de nuevo al primer nodo cuando haya terminado de dibujar el área. Nota: la interpretación del camino cerrado depende de su etiquetado: por ejemplo, si un camino cerrado se etiqueta como un edificio, se considerará un área, si se etiqueta como una rotonda, no. En algunas situaciones en las que ambas interpretaciones pueden ser válidas, una etiqueta de «área» puede aclarar el uso previsto.
* _multi-polígonos_: algunas áreas tienen múltiples partes, agujeros y anillos que no se pueden representar de una sola manera. OSM usa un tipo específico de relación (nuestro objeto de propósito general que puede modelar las relaciones entre los elementos) para sortear esto, un multi-polígono. Un multi-polígono puede tener múltiples anillos «externos» y múltiples anillos «internos». Cada anillo puede ser cerrado como se describe anteriormente, o múltiples formas individuales que tienen nodos finales comunes. Mientras que los grandes multi-polígonos son difíciles de manejar con cualquier herramienta, los pequeños no son difíciles de crear en Vespucci. 
* _costas_: para objetos muy grandes, continentes e islas, incluso el modelo de multi-polígonos no funciona de manera satisfactoria. Para las formas naturales=litorales asumimos una semántica dependiente de la dirección: la tierra está en el lado izquierdo del camino, el agua en el lado derecho. Un efecto secundario de esto es que, en general, no debe invertir la dirección de una ruta con el etiquetado de la costa. Puede encontrarse más información en la [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorando la geometría de vías

Si hace el suficiente zoom en una ruta seleccionada, verá pequeños segmentos «x» en medio de la ruta que son lo suficientemente largos. Al arrastrar la «x» se creará un nodo en la ruta en esa ubicación. Nota: para evitar la creación accidental de nodos, el área de tolerancia táctil para esta operación es bastante pequeña.

#### Cortar, copiar y pegar

Usted puede copiar o cortar los nodos y caminos seleccionados, y después pegarlos una o múltiples veces en una nueva ubicación. Cortar retendrá la ID y la versión de osm. Para pegar presione un rato la ubicación en la que desea pegar (verá una línea cruzada creando la ubicación). Después seleccione "Pegar" desde el menú.

#### Añadiendo direcciones de manera eficiente

Vespucci tiene una función de «agregar etiquetas de dirección» que intenta hacer las direcciones topográficas más eficientes. Puede ser seleccionado:

* tras una larga presión: Vespucci agregará un nodo en la ubicación y hará una mejor estimación del número de la casa y añadirá las etiquetas de dirección que ha estado utilizando últimamente. Si el nodo está en el contorno de un edificio, añadirá automáticamente una etiqueta de «entrada=sí» al nodo. El editor de etiquetas se abrirá para el objeto en cuestión y le permitirá realizar los cambios adicionales necesarios.
* en los modos nodo/ruta seleccionados: Vespucci agregará las etiquetas de dirección como se indica arriba e iniciará el editor de etiquetas.
* en el editor de etiquetas.

La predicción de números de casas normalmente requiere al menos dos números de casas a cada lado de la vía para que funcione; cuantos más números presentes en los datos, mejor.

Considere utilizar esto con el modo [Auto-descarga](#download).  

#### Añadiendo restricciones de giro

Vespucci tiene una manera rápida de agregar restricciones de giro. si es necesario, dividirá las formas automáticamente y le pedirá que vuelva a seleccionar los elementos. 

* seleccione una forma con una etiqueta de autovía (las restricciones de giro solo se pueden añadir a las autovías, si necesita hacer esto de otras maneras, utilice el modo genérico de «crear relación»)
* seleccione «Añadir restricción» desde el menú
* seleccione el nodo «vía» o ruta (solo los posibles elementos «via» tendrán el área táctil mostrada)
* seleccione la ruta «a» (es posible volver a doblar y configurar el elemento «a» al elemento «desde», Vespucci asumirá que está añadiendo una restricción no_u_turn)
* establezca el tipo de restricción

### Vespucci en modo «bloqueado»

Cuando se muestra el candado rojo todas las acciones no editables están disponibles. Adicionalmente una larga presión sobre o cerca de un objeto mostrará la pantalla de información detallada si es un objeto OSM.

### Guardando sus cambios

*(requiere conectividad de red)*

Seleccione el mismo botón o ítem del menú que hizo para la descarga y ahora seleccione «Subir datos al servidor OSM».

Vespucci soporta la autorización OAuth y el clásico método usuario y contraseña. OAuth es preferible ya que evita el envío de contraseñas en texto plano.

Las nuevas instalaciones de Vespucci tendrán habilitado OAuth por defecto. En su primer intento de cargar información modificada, se carga una página de OSM. Cuando usted se ha conectado (con una conexión encriptada) se le pedirá que autorice a Vespucci editar utilizando su cuenta. Si quiere o necesita autorizar el acceso OAuth a su cuenta antes de editar hay un artículo correspondiente en el menú "Herramientas".

Si quiere guardar su trabajo y no tiene acceso a Internet, puede guardar un archivo .osm compatible con JOSM y luego subir ya sea con Vespucci o con JOSM. 

#### Resolviendo conflictos al subir

Vespucci tiene un solucionador de conflictos simple. Sin embargo, si sospecha que hay problemas importantes con sus ediciones, exporte los cambios a un archivo .osc (elemento del menú «Exportar» en el menú «Transferir») y arreglarlos y subirlos con JOSM. Ver la ayuda detallada en [resolución de conflictos](../en/Conflict%20resolution.md).  

## Usando GPS

Puede utilizar Vespucci para crear una pista GPX y mostrarla en su dispositivo. Además puede mostrar la ubicación GPS actual (seleccione "Mostrar ubicación" en el menú GPS) y/o centrar la pantalla y seguir su posición (seleccione "Seguir ubicación GPS" en el menú GPS). 

Si tiene este último conjunto, mover la pantalla manualmente o editar hará que se deshabilite el modo «Seguir GPS» y la flecha azul del GPS cambiará de un esquema a una flecha llena. Para volver rápidamente al modo «seguir», simplemente toque el botón GPS o vuelva a verificar la opción de menú.

## Notas y errores

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

En el mapa las Notas y errores son representados mediante un pequeño icono de error ![Error](../images/bug_open.png); los verdes son cerrados/resueltos, los azules han sido creados o editados por usted, y los amarillos indican que aún está activo y no ha sido cambiado. 

La exposición de error OSMOSE proveerá un enlace al objeto azul afectado, tocar el enlace seleccionará el objeto, centrará la pantalla sobre él y descargará el área con antelación si fuera necesario. 

### Filtrado

Además de habilitar globalmente la visualización de notas y errores, puede configurar un filtro de visualización de grano grueso para reducir el desorden. En las «Preferencias avanzadas» puede seleccionar individualmente:

* Notas
* Error Osmose
* Advertencia Osmose
* Problema menor Osmose


<a id="indoor"></a>

## Modo de interiores

El mapeo en interiores es un desafío debido a la gran cantidad de objetos que muy a menudo se superponen unos a otros. Vespucci tiene un modo interior dedicado que le permite filtrar todos los objetos que no están en el mismo nivel y que agregarán automáticamente el nivel actual a los nuevos objetos creados.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half check "entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a check_date field is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Check optional** - Check the optional tags of the matching preset.

This check works be first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Check optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtros

### Filtro basado en etiqueta

El filtro se puede habilitar desde el menú principal, luego se puede cambiar tocando el ícono de filtro. Más documentación se puede encontrar aquí [Filtro de etiqueta](../en/Tag%20filter.md).

### Filtro basado en preajustes

Como alternativa a lo anterior, los objetos se filtran en presets individuales o en grupos preestablecidos. Al tocar en el ícono del filtro aparecerá un cuadro de diálogo de selección preestablecido similar al utilizado en Vespucci. Los preajustes individuales se pueden seleccionar con un clic normal, los grupos predefinidos con un clic prolongado (con un clic normal entra al grupo). Más documentación se puede encontrar aquí [Filtro preestablecido](../en/Preset%20filter.md).

## Personalizando Vespucci

### Los ajustes que podría querer cambiar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

Preferencias avanzadas

* Mostrar siempre el menú contextual. Cuando se enciende, cada proceso de selección muestra el menú contextual, apagado el menú se muestra solo cuando no se puede determinar una selección inequívoca.. Predeterminado: desactivado (usar para activarlo).
* Habilitar tema de luz. En dispositivos modernos, esto está activado de manera predeterminada. Si bien puede habilitarlo para versiones anteriores de Android, es probable que el estilo sea inconsistente.
* Mostrar estadísticas. Mostrará algunas estadísticas para la depuración, no es realmente útil. Predeterminado: desactivado (usar para activarlo).  

## Informar de problemas

Si Vespucci se bloquea o detecta un estado incoherente, se le pedirá que envíe el volcado. Por favor, hágalo si eso sucede, pero sólo una vez por situación específica, por favor. Si desea dar más información o abrir un problema para una solicitud de función o similar, hágalo aquí: [Rastreador de problemas de Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Si desea analizar algo relacionado con Vespucci, puede iniciar una discusión en el [Grupo de Vespucci de Google](https://groups.google.com/forum/#!forum/osmeditor4android) o en el [Foro de Android de OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


