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

Bloqueo, desbloqueo, "sólo edición de etiquetas", modo interiores. 

Para evitar ediciones accidentales, Vespucci comienza en modo "bloqueado", un modo que sólo permite hacer zoom y mover el mapa. Toque el icono ![Bloqueado](../images/locked.png) para desbloquear la pantalla. 

Una pulsación larga sobre el icono de bloqueo activará el modo "Sólo edición de etiquetas", el cual está estará indicado con un icono de bloqueo blanco ligeramente distinto. Este modo no le permitirá cambiar la geometría de los objetos o moverlos, sin embargo podrá crear nuevos nodos y vías con una pulsación larga, de la forma normal.

Otra pulsación larga activará el [Modo interiores](#indoor), y con una más se volverá al modo de edición normal.

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

A second touch on the selected object opens the tag editor and you can edit the tags associated with the object.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Objetos seleccionados: Moviendo un nodo o vía

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Añadiendo un nuevo nodo/punto o vía (pulsación larga)

Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Once you can see the crosshair symbol, you have these options:

* Touch in the same place.
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* Touch another place. Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is  located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](../en/Creating%20new%20objects.md) for more information.

#### Añadiendo un Área

OpenStreetMap currently doesn't have an "area" object type contrary to other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorando la geometría de vías

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Cortar, copiar y pegar

Usted puede copiar o cortar los nodos y caminos seleccionados, y después pegarlos una o múltiples veces en una nueva ubicación. Cortar retendrá la ID y la versión de osm. Para pegar presione un rato la ubicación en la que desea pegar (verá una línea cruzada creando la ubicación). Después seleccione "Pegar" desde el menú.

#### Añadiendo direcciones de manera eficiente

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

La predicción de números de casas normalmente requiere al menos dos números de casas a cada lado de la vía para que funcione; cuantos más números presentes en los datos, mejor.

Consider using this with the [Auto-download](#download) mode.  

#### Añadiendo restricciones de giro

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci en modo «bloqueado»

Cuando se muestra el candado rojo todas las acciones no editables están disponibles. Adicionalmente una larga presión sobre o cerca de un objeto mostrará la pantalla de información detallada si es un objeto OSM.

### Guardando sus cambios

*(requiere conectividad de red)*

Seleccione el mismo botón o ítem del menú que hizo para la descarga y ahora seleccione «Subir datos al servidor OSM».

Vespucci soporta la autorización OAuth y el clásico método usuario y contraseña. OAuth es preferible ya que evita el envío de contraseñas en texto plano.

Las nuevas instalaciones de Vespucci tendrán habilitado OAuth por defecto. En su primer intento de cargar información modificada, se carga una página de OSM. Cuando usted se ha conectado (con una conexión encriptada) se le pedirá que autorice a Vespucci editar utilizando su cuenta. Si quiere o necesita autorizar el acceso OAuth a su cuenta antes de editar hay un artículo correspondiente en el menú "Herramientas".

Si quiere guardar su trabajo y no tiene acceso a Internet, puede guardar un archivo .osm compatible con JOSM y luego subir ya sea con Vespucci o con JOSM. 

#### Resolviendo conflictos al subir

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict%20resolution.md).  

## Usando GPS

Puede utilizar Vespucci para crear una pista GPX y mostrarla en su dispositivo. Además puede mostrar la ubicación GPS actual (seleccione "Mostrar ubicación" en el menú GPS) y/o centrar la pantalla y seguir su posición (seleccione "Seguir ubicación GPS" en el menú GPS). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Notas y errores

Vespucci soporta descarga, comentarios y cerrado de Notas OSM (anteriormente errores OSM) y la equivalente funcionalidad para "Errores" producida por la [herramienta de aseguramiento de la calidad OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambas han de ser descargadas explícitamente o puede utilizar el servicio autodescarga para acceder a los elementos en su área cercana. Una vez editado o cerrado, puede cargar el error o Nota inmediatamente o cargarlo todo de una vez.

En el mapa las Notas y errores son representados mediante un pequeño icono de error ![Error](../images/bug_open.png); los verdes son cerrados/resueltos, los azules han sido creados o editados por usted, y los amarillos indican que aún está activo y no ha sido cambiado. 

La exposición de error OSMOSE proveerá un enlace al objeto azul afectado, tocar el enlace seleccionará el objeto, centrará la pantalla sobre él y descargará el área con antelación si fuera necesario. 

### Filtrado

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

* Notas
* Error Osmose
* Advertencia Osmose
* Problema menor Osmose


<a id="indoor"></a>

## Modo de interiores

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created their.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, "tag editing only", indoor mode](#lock).

## Filtros

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](../en/Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](../en/Preset%20filter.md).

## Personalizando Vespucci

### Los ajustes que podría querer cambiar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

Preferencias avanzadas

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Informar de problemas

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


