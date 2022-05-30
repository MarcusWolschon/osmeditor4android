_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too._

# Introducción a Vespucci

Vespucci es un editor OpenStreetMap con todas las funciones que admite la mayoría de las operaciones que proporcionan los editores de escritorio. Se ha probado con éxito en Android 2.3 a 10.0 de Google y en varias variantes basadas en AOSP. Una advertencia: aunque las capacidades de los dispositivos móviles se han puesto al día con sus rivales de escritorio, particularmente los dispositivos más antiguos tienen una memoria muy limitada disponible y tienden a ser bastante lentos. Debe tener esto en cuenta al usar Vespucci y mantener, por ejemplo, las áreas que está editando a un tamaño razonable.

## Editando con Vespucci

Dependiendo del tamaño de pantalla y antigüedad de su dispositivo, las acciones de edición pueden estar accesibles directamente por medio de iconos en la barra superior, en un menú desplegable a la derecha de la barra superior, desde la barra inferior (si está presente) o por medio de la tecla de menú.

<a id="download"></a>

## Descargando datos OSM

Seleccione el ícono de transferencia ![Transferir](../images/menu_transfer.png) o la opción del menú «Transferir». Esto mostrará siete opciones:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

La manera más fácil de descargar información al dispositivo es hacer zoom y desplazar hasta la ubicación que quiere editar y después seleccionar "Descargar vista actual". Puede hacer zoom utilizando gestos, como los botones de zoom o los botones de control de volumen del dispositivo. Vespucci debería descargar entonces información de la vista actual. No se requiere autenticación para descargar información a su dispositivo.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

## Editando

<a id="lock"></a>

#### Cambio de modo, bloquear, desbloquear

Para evitar ediciones accidentales, Vespucci comienza en modo "bloqueado", un modo que sólo permite hacer zoom y mover el mapa. Toque el icono ![Bloqueado](../images/locked.png) para desbloquear la pantalla. 

Una pulsación larga en el icono del candado mostrará un menú con cuatro opciones:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Pulsación simple, pulsación doble y pulsación larga

Por defecto, los nodos y vías selecionables tienen un área naranja a su alrededor indicando aproximadamente dónde tiene que tocar para seleccionar un objeto. Tiene tres opciones:

* Solo toque: selecciona el objeto. 
* Un nodo / vía aislado se resalta inmediatamente. 
* Sin embargo, si intenta seleccionar un objeto y Vespucci determina que la selección podría significar varios objetos, presentará un menú de selección, permitiéndole elegir el objeto que desea seleccionar. 
* Los objetos seleccionados se resaltan en amarillo. 
* Para obtener más información, consulte [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada] (Relation%20selected.md).
* Doble toque: Inicio [Modo de selección múltiple](Multiselect.md)
* Pulsación larga: crea un "punto de mira", que le permite agregar nodos, ver más abajo y [Crear nuevos objetos](Creating%20new%20objects.md). Esto solo está habilitado si el "Modo simple" está desactivado.

Es una buena estrategia acercar el mapa si intenta editar un área con alta densidad.

Vespucci tiene un buen sistema «deshacer/rehacer» así que no tenga miedo de experimentar en su dispositivo, sin embargo no suba ni guarde los datos de prueba.

#### Seleccionando / Deseleccionando (pulsación simple y "menú de selección")

Toque un objeto para seleccionarlo y resaltarlo. Al tocar la pantalla en una región vacía, se anulará la selección. Si ha seleccionado un objeto y necesita seleccionar otra cosa, simplemente toque el objeto en cuestión, no hay necesidad de anular la selección primero. Un doble toque en un objeto iniciará [Modo de selección múltiple](Multiselect.md)

Tenga en cuenta que si intenta seleccionar un objeto y Vespucci determina que la selección se podría referir a varios objetos (tales como un nodo en una vía u otros objetos superpuestos) se presentará un menú de selección: pulse en el objeto que quiera seleccionar y el objeto se seleccionará. 

Los objetos seleccionados se indican con un fino borde amarillo. Puede que el borde amarillo sea difícil de ver, dependiendo en el fondo del mapa y el nivel de zoom. Una vez que se ha hecho una selección, verá una notificación confirmando la selección.

Una vez que la selección se haya completado, verá (ya sea como botones o como elementos de menú) una lista de operaciones admitidas para el objeto seleccionado: Para obtener más información, consulte [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada](Relation%20selected.md).

#### Objetos seleccionados: Editando etiquetas

Un segundo toque en el objeto seleccionado abre el editor de etiquetas y puede editar las etiquetas asociadas al objeto.

Tenga en cuenta que para superponer objetos (como un nodo en un camino) el menú de selección vuelve a subir por segunda vez. Al seleccionar el mismo objeto, aparece el editor de etiquetas; seleccionar otro objeto simplemente selecciona el otro objeto.

#### Objetos seleccionados: Moviendo un nodo o vía

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Añadiendo un nuevo Nodo/Punto o Vía 

En el primer inicio, la aplicación se inicia en "Modo simple", esto se puede cambiar en el menú principal desmarcando la casilla correspondiente.

##### Modo simple

Al tocar el gran botón verde flotante en la pantalla del mapa, aparecerá un menú. Después de seleccionar uno de los elementos, se le pedirá que toque la pantalla en la ubicación donde desea crear el objeto, desplazarse y acercarse continuarán funcionando si necesita ajustar la vista del mapa. 

Consulte [Creación de nuevos objetos en modo de acciones simples](Creating%20new%20objects%20in%20simple%20actions%20mode.md) para obtener más información.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Una vez pueda ver el símbolo de la cruz, tiene estas opciones:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simplemente toque la pantalla donde desea agregar más nodos del camino. Para terminar, toque el nodo final dos veces. Si el nodo final se encuentra en una vía o nodo, el segmento se conectará a la vía o nodo automáticamente. 

También puede usar un elemento de menú: consulte [Creación de nuevos objetos](Creating%20new%20objects.md) para obtener más información.

#### Añadiendo un Área

Actualmente OpenStreetMap no tiene un objeto tipo "área" al contrario que otros sistemas de geo-datos. El editor en línea "iD" intenta crear una abstracción de área a partir de los elementos OSM subyacentes que funciona bien en algunas circunstancia y no tanto en otras. Actualmente Vespucci no intenta hacer nada similar, así que necesitará conocer un poco sobre la forma en que se representan las áreas.

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorando la geometría de vías

Si hace el suficiente zoom en una ruta seleccionada, verá pequeños segmentos «x» en medio de la ruta que son lo suficientemente largos. Al arrastrar la «x» se creará un nodo en la ruta en esa ubicación. Nota: para evitar la creación accidental de nodos, el área de tolerancia táctil para esta operación es bastante pequeña.

#### Cortar, copiar y pegar

Usted puede copiar o cortar los nodos y caminos seleccionados, y después pegarlos una o múltiples veces en una nueva ubicación. Cortar retendrá la ID y la versión de osm. Para pegar presione un rato la ubicación en la que desea pegar (verá una línea cruzada creando la ubicación). Después seleccione "Pegar" desde el menú.

#### Agregar direcciones de manera eficiente

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

La predicción de números de casas normalmente requiere al menos dos números de casas a cada lado de la vía para que funcione; cuantos más números presentes en los datos, mejor.

Consider using this with one of the [Auto-download](#download) modes.  

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

Vespucci tiene un solucionador de conflictos simple. Sin embargo, si sospecha que hay problemas importantes con sus ediciones, exporte sus cambios a un archivo .osc (elemento de menú "Exportar" en el menú "Transferencia") corríjalos y cárguelos con JOSM. Consulte la ayuda detallada sobre [resolución de conflictos](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notas y errores

Vespucci soporta la descarga, comentar y cerrar Notas de OSM (anteriormente Errores OSM) y la funcionalidad equivalente para "Errores" producidos por la [herramienta de aseguramiento de calidad OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos deben de, o bien haber sido descargados explícitamente, o puedes usar el servicio de auto descarga para acceder a los elementos en su área inmediata. Una vez editados o cerrados, puede o bien subir el error o Nota al servidor de forma inmediata o subirlos todos de una vez.

En el mapa las Notas y errores son representados mediante un pequeño icono de error ![Error](../images/bug_open.png); los verdes son cerrados/resueltos, los azules han sido creados o editados por usted, y los amarillos indican que aún está activo y no ha sido cambiado. 

La exposición de error OSMOSE proveerá un enlace al objeto azul afectado, tocar el enlace seleccionará el objeto, centrará la pantalla sobre él y descargará el área con antelación si fuera necesario. 

### Filtrado

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Custom

<a id="indoor"></a>

## Modo de interiores

El mapeo en interiores es un desafío debido a la gran cantidad de objetos que muy a menudo se superponen entre sí. Vespucci tiene un modo interior dedicado que le permite filtrar todos los objetos que no están en el mismo nivel y que agregará automáticamente el nivel actual a los nuevos objetos creados allí.

El modo puede activarse con una pulsación larga en el icono del cansado, ver [Cambiando de modo, bloquear, desbloquear](#lock) y seleccionando la correspondiente entrada de menú.

<a id="c-mode"></a>

## Modo-C

En el Modo-C sólo se muestran los objetos que tienen activa una señal de aviso, esto facilita resaltar objetos que tienen problemas específicos o coinciden con chequeos configurables. Si un objeto es seleccionado y el Editor de Propiedades iniciado en Modo-C se aplicará automáticamente el predefinido que mejor se ajuste.

El modo puede activarse con una pulsación larga en el icono del cansado, ver [Cambiando de modo, bloquear, desbloquear](#lock) y seleccionando la correspondiente entrada de menú.

### Configurando chequeos

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the [preferences](Preferences.md). 

La lista de entradas se divide en dos, la mitad superior enumera las entradas de "relevamiento", la mitad inferior "las entradas de control". Las entradas se pueden editar haciendo clic en ellas, el botón de menú verde permite agregar entradas.

#### Entradas de relevamiento

Las entradas de relevamiento tienen las siguientes propiedades:

* **Clave** - Clave de la etiqueta de interés.
* **Valor**: Valor que debe tener la etiqueta de interés, si está vacío, se ignorará el valor de la etiqueta.
* **Edad**: Cuántos días después de que el elemento se modificó por última vez, se debe volver a encuestar el elemento, si hay una etiqueta _check_date_ que se usará, de lo contrario, la fecha en que se creó la versión actual. Establecer el valor a cero llevará a la verificación simplemente haciendo coincidir la clave y el valor.
* **Expresión regula ** - si está marcada **Se asume que el valor** es una expresión regular JAVA.

**Clave** y **Valor** se comparan con las etiquetas _existentes_ del objeto en cuestión.

El grupo _Annotations_ en los predefinidos estándar contiene un elemento que agregará automáticamente una etiqueta _check_date_ con la fecha actual.

#### Entradas de control

Las entradas de control tienen las siguientes dos propiedades:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

Esta verificación funciona primero determinando el predefinido coincidente y luego verificando si **Clave** es una clave "recomendada" para este objeto de acuerdo con el predefinido, **Requerir opcional** expandirá la verificación a las etiquetas que son "opcionales* en el objeto. Nota: los predefinidos vinculados actualmente no están comprobados.

## Filtros

### Filtro basado en etiqueta

El filtro se puede habilitar desde el menú principal, luego se puede cambiar tocando el icono del filtro. Puede encontrar más documentación aquí [Filtro de etiquetas](Tag%20filter.md).

### Filtro basado en predefinido

Una alternativa a lo anterior, los objetos se filtran en predefinidos individuales o en grupos de predefinidos. Al tocar el icono del filtro, se mostrará un cuadro de diálogo de selección predefinidos similar al que se usa en otros lugares de Vespucci. Los predefinidos individuales se pueden seleccionar con un clic normal, los grupos de predefinidos con un clic largo (el clic normal ingresa al grupo). Puede encontrar más documentación aquí [Filtro de predefinidos] (Preset%20filter.md).

## Personalizando Vespucci

Muchos aspectos de la aplicación se pueden personalizar, si está buscando algo específico y no puede encontrarlo, [el sitio web de Vespucci](https://vespucci.io/) se puede buscar y contiene información adicional sobre lo que está disponible en el dispositivo.

<a id="layers"></a>

### Configuraciones de capa

La configuración de la capa se puede cambiar a través del control de capa (menú "hamburguesa" en la esquina superior derecha), todas las demás configuraciones son accesibles a través del botón de preferencias del menú principal. Las capas se pueden habilitar, deshabilitar y ocultar temporalmente.

Tipos de capas disponibles:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferencias

* Mantener la pantalla encendida. Predeterminado: apagado.
* Área de arrastre de nodo grande. Mover nodos en un dispositivo con entrada táctil es problemático ya que sus dedos oscurecerán la posición actual en la pantalla. Activar esto proporcionará un área grande que se puede usar para arrastrar fuera del centro (la selección y otras operaciones aún usan el área de tolerancia táctil normal). Predeterminado: apagado.

La descripción completa se puede encontrar aquí [Preferencias](Preferences.md)

Preferencias avanzadas

* Iconos de nodos. Predeterminado: activado.
* Mostrar siempre el menú contextual. Cuando se activa, todos los procesos de selección mostrarán el menú contextual; el menú desactivado se muestra solo cuando no se puede determinar una selección inequívoca. Predeterminado: apagado (solía estar encendido).
* Habilitar tema ligero. En los dispositivos modernos, esto está activado de forma predeterminada. Si bien puede habilitarlo para versiones anteriores de Android, es probable que el estilo sea inconsistente. 

La descripción completa se puede encontrar aquí [Preferencias avanzadas](Advanced%20preferences.md)

## Informar de problemas

Si Vespucci falla o detecta un estado inconsistente, se le pedirá que envíe el volcado de memoria. Hágalo si eso sucede, pero solo una vez por situación específica. Si desea dar más información o abrir un problema para una solicitud de función o similar, hágalo aquí: [Rastreador de problemas de Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). La función "Proporcionar comentarios" del menú principal abrirá un nuevo problema e incluirá la aplicación relevante y la información del dispositivo sin necesidad de escribir más.

Si desea discutir algo relacionado con Vespucci, puede comenzar una discusión en el [grupo de Vespucci Google](https://groups.google.com/forum/#!forum/osmeditor4android) o en el [foro de Android OpenStreetMap] (http://forum.openstreetmap.org/viewforum.php?id=56)


