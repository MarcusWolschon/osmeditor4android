_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Introducción a Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Editar con Vespucci

Dependiendo del tamaño de la pantalla y la antigüedad de tu dispositivo, puedes acceder a las acciones de edición directamente mediante los iconos de la barra superior, a través de un menú desplegable a la derecha de la barra superior, desde la barra inferior (si está presente) o mediante la tecla de menú.

<a id="download"></a>

### Descargar datos de OSM

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display eleven options:

* **Upload data to OSM server...** - review and upload changes to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Review changes...** - review current changes
* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory, including pending changes, and then download the area visible on the screen *(requires network connectivity)*
* **Query Overpass...** - run a query against a Overpass API server *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Clear data** - remove any OSM data in memory, including pending changes.
* **File...** - saving and loading OSM data to/from on device files.
* **Tasks...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

La manera más fácil de descargar datos al dispositivo es hacer zoom y desplazarte hasta la ubicación que quieres editar y luego seleccionar "Descargar vista actual". Puedes hacer zoom utilizando gestos, los botones de zoom o los botones de control de volumen del dispositivo. Vespucci debería descargar entonces los datos de la vista actual. No se requiere autenticación para descargar datos a tu dispositivo.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Editar

<a id="lock"></a>

#### Bloquear, desbloquear, cambio de modo

Para evitar ediciones accidentales, Vespucci inicia en modo "bloqueado", un modo que solo permite hacer zoom y mover el mapa. Toca el ícono ![Bloqueado](../images/locked.png) para desbloquear la pantalla. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Toque simple, toque doble y pulsación larga

De forma predeterminada, los nodos y vías seleccionables tienen un área naranja a su alrededor que indica aproximadamente dónde debes tocar para seleccionar un objeto. Tienes tres opciones:

* Toque simple: selecciona el objeto.
* Un nodo/vía aislado se resalta inmediatamente.
* Sin embargo, si intentas seleccionar un objeto y Vespucci determina que la selección podría referirse a varios objetos, presentará un menú de selección que te permitirá elegir el objeto que deseas seleccionar.
* Los objetos seleccionados se resaltan en amarillo.
* Para obtener más información, consulta [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada](Relation%20selected.md).
* Toque doble: inicia el [modo de selección múltiple](Multiselect.md).
* Pulsación larga: crea una "cruz", que te permite agregar nodos; consulta a continuación y [Crear nuevos objetos](Creating%20new%20objects.md). Esto solo está habilitado si el "Modo simple" está desactivado.

Es una buena estrategia acercar el zoom si intentas editar un área de alta densidad.

Vespucci tiene un buen sistema de "deshacer/rehacer", así que no tengas miedo de experimentar en tu dispositivo. Sin embargo, no subas ni guardes datos de prueba puros.

#### Seleccionar / Deseleccionar (toque simple y "menú de selección")

Toca un objeto para seleccionarlo y resaltarlo. Tocar la pantalla en una región vacía anulará la selección. Si has seleccionado un objeto y necesitas seleccionar otro, simplemente toca el objeto en cuestión; no es necesario deseleccionar primero. Un doble toque en un objeto iniciará el [modo de selección múltiple](Multiselect.md).

Ten en cuenta que si intentas seleccionar un objeto y Vespucci determina que la selección podría referirse a varios objetos (como un nodo en una vía u otros objetos que se superponen), presentará un menú de selección: toca el objeto que deseas seleccionar y el objeto se seleccionará. 

Los objetos seleccionados se indican mediante un borde amarillo delgado. El borde amarillo puede ser difícil de detectar, dependiendo del fondo del mapa y el factor de zoom. Una vez realizada una selección, verás una notificación que confirma la selección.

Una vez que se haya completado la selección, verás (ya sea como botones o como elementos del menú) una lista de operaciones compatibles para el objeto seleccionado. Para obtener más información, consulta [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada](Relation%20selected.md).

#### Objetos seleccionados: Editar etiquetas

Un segundo toque en el objeto seleccionado abre el editor de etiquetas y puedes editar las etiquetas asociadas al objeto.

Ten en cuenta que para los objetos que se superponen (como un nodo en una vía), el menú de selección vuelve a aparecer por segunda vez. Seleccionar el mismo objeto abre el editor de etiquetas; seleccionar otro objeto simplemente selecciona el otro objeto.

#### Objetos seleccionados: Mover un nodo o vía

Una vez que hayas seleccionado un objeto, puedes moverlo. Ten en cuenta que los objetos solo se pueden arrastrar/mover cuando están seleccionados. Simplemente arrastra cerca (es decir, dentro de la zona de tolerancia) del objeto seleccionado para moverlo. Si seleccionas el área de arrastre grande en las [preferencias](Preferences.md), obtendrás un área grande alrededor del nodo seleccionado que facilita la colocación del objeto. 

#### Agregar un nuevo nodo/punto o vía 

Al iniciar la aplicación por primera vez, se inicia en "Modo simple". Esto se puede cambiar en el menú principal desmarcando la casilla correspondiente.

##### Modo simple

Al tocar el botón flotante verde grande en la pantalla del mapa, se mostrará un menú. Después de que hayas seleccionado uno de los elementos, se te pedirá que toques la pantalla en la ubicación donde deseas crear el objeto. Desplazar y hacer zoom continúa funcionando si necesitas ajustar la vista del mapa. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Modo avanzado (pulsación larga)

Mantén pulsado donde quieras que esté el nodo o donde quieras que comience la vía. Verás un símbolo de "cruz" negra.
* Si quieres crear un nuevo nodo (no conectado a un objeto), toca lejos de los objetos existentes.
* Si quieres extender una vía, toca dentro de la "zona de tolerancia" de la vía (o un nodo en la vía). La zona de tolerancia se indica mediante las áreas alrededor de un nodo o vía.

Una vez que veas el símbolo de la cruz, tienes estas opciones:

* *Pulsación normal en el mismo lugar*.
* Si la cruz no está cerca de un nodo, tocar la misma ubicación nuevamente crea un nuevo nodo. Si estás cerca de una vía (pero no cerca de un nodo), el nuevo nodo estará en la vía (y conectado a la vía).
* Si la cruz está cerca de un nodo (es decir, dentro de la zona de tolerancia del nodo), tocar la misma ubicación simplemente selecciona el nodo (y se abre el editor de etiquetas). No se crea ningún nodo nuevo. La acción es la misma que la selección anterior.
* *Toque normal en otro lugar*. Tocar otra ubicación (fuera de la zona de tolerancia de la cruz) agrega un segmento de vía desde la posición original a la posición actual. Si la cruz estaba cerca de una vía o nodo, el nuevo segmento se conectará a ese nodo o vía.

Simplemente toca la pantalla donde quieras agregar más nodos de la vía. Para terminar, toca el nodo final dos veces. Si el nodo final se encuentra en una vía o nodo, el segmento se conectará automáticamente a la vía o nodo. 

También puedes usar un elemento de menú: consulta [Crear nuevos objetos](Creating%20new%20objects.md) para obtener más información.

#### Agregar un área

OpenStreetMap actualmente no tiene un tipo de objeto "área" a diferencia de otros sistemas de geodatos. El editor en línea "iD" intenta crear una abstracción de área a partir de los elementos OSM subyacentes, lo que funciona bien en algunas circunstancias y no tan bien en otras. Actualmente, Vespucci no intenta hacer nada similar, por lo que necesitas saber un poco sobre cómo se representan las áreas:

* _vías cerradas (polígonos)_: la variante de área más simple y común son las vías que comparten un primer y último nodo, formando un "anillo" cerrado (por ejemplo, la mayoría de los edificios son de este tipo). Son muy fáciles de crear en Vespucci, simplemente conéctate de nuevo al primer nodo cuando hayas terminado de dibujar el área. Nota: la interpretación de la vía cerrada depende de su etiquetado: por ejemplo, si una vía cerrada está etiquetada como un edificio, se considerará un área; si está etiquetada como una rotonda, no. En algunas situaciones en las que ambas interpretaciones pueden ser válidas, una etiqueta de "área" puede aclarar el uso previsto.
* _multipolígonos_: algunas áreas tienen varias partes, agujeros y anillos que no se pueden representar con una sola vía. OSM utiliza un tipo específico de relación (nuestro objeto de propósito general que puede modelar relaciones entre elementos) para solucionar esto: un multipolígono. Un multipolígono puede tener varios anillos "externos" y varios anillos "internos". Cada anillo puede ser una vía cerrada como se describe anteriormente o varias vías individuales que tienen nodos finales comunes. Si bien los multipolígonos grandes son difíciles de manejar con cualquier herramienta, los pequeños no son difíciles de crear en Vespucci.
* _líneas costeras_: para objetos muy grandes, continentes e islas, incluso el modelo de multipolígono no funciona de manera satisfactoria. Para las vías natural=línea costera, asumimos una semántica dependiente de la dirección: la tierra está en el lado izquierdo de la vía, el agua en el lado derecho. Un efecto secundario de esto es que, en general, no debes invertir la dirección de una vía con etiquetado de línea costera. Puedes encontrar más información en la [wiki de OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorar la geometría de la vía

Si acercas el zoom lo suficiente en una vía seleccionada, verás una pequeña "x" en el medio de los segmentos de la vía que son lo suficientemente largos. Arrastrar la "x" creará un nodo en la vía en esa ubicación. Nota: para evitar la creación accidental de nodos, el área de tolerancia táctil para esta operación es bastante pequeña.

#### Cortar, copiar y pegar

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Agregar direcciones de forma eficiente

Vespucci admite funciones que hacen que el levantamiento de direcciones sea más eficiente al predecir los números de casas (lados izquierdo y derecho de las calles por separado) y agregar automáticamente etiquetas _addr:street_ o _addr:place_ según el último valor utilizado y la proximidad. En el mejor de los casos, esto permite agregar una dirección sin necesidad de escribir nada.   

Puedes activar la adición de etiquetas pulsando ![Dirección](../images/address.png): 

* después de una pulsación larga (solo en modo no simple): Vespucci agregará un nodo en la ubicación, hará una mejor estimación del número de casa y agregará etiquetas de dirección que hayas estado usando últimamente. Si el nodo está en el contorno de un edificio, agregará automáticamente una etiqueta "entrance=yes" al nodo. El editor de etiquetas se abrirá para el objeto en cuestión y te permitirá realizar los cambios adicionales necesarios.
* en los modos de nodo/vía seleccionados: Vespucci agregará etiquetas de dirección como se indicó anteriormente e iniciará el editor de etiquetas.
* en el editor de propiedades.

Para agregar nodos de dirección individuales directamente mientras estás en el "Modo simple" predeterminado, cambia al modo de edición "Dirección" (pulsación larga en el botón de bloqueo). "Agregar nodo de dirección" agregará un nodo de dirección en la ubicación y, si está en el contorno de un edificio, le agregará una etiqueta de entrada como se describe arriba.

La predicción del número de casa generalmente requiere que se ingresen al menos dos números de casa a cada lado de la carretera para que funcione; cuantos más números haya en los datos, mejor.

Considera usar esto con uno de los modos de [Descarga automática](#download).  

#### Agregar restricciones de giro

Vespucci tiene una forma rápida de agregar restricciones de giro. Si es necesario, dividirá las vías automáticamente y te pedirá que vuelvas a seleccionar los elementos. 

* Selecciona una vía con una etiqueta highway (las restricciones de giro solo se pueden agregar a carreteras; si necesitas hacer esto para otras vías, utiliza el modo genérico "crear relación").
* Selecciona "Agregar restricción" en el menú.
* Selecciona el nodo o vía "vía" (solo los elementos "vía" posibles mostrarán el área táctil).
* Selecciona la vía "a" (es posible retroceder y configurar el elemento "a" en el elemento "desde"; Vespucci asumirá que estás agregando una restricción no_u_turn).
* Establece el tipo de restricción.

### Vespucci en modo "bloqueado"

Cuando se muestra el candado rojo, todas las acciones que no son de edición están disponibles. Además, una pulsación larga en o cerca de un objeto mostrará la pantalla de información detallada si se trata de un objeto OSM.

### Guardar los cambios

*(requiere conectividad de red)*

Selecciona el mismo botón o elemento de menú que usaste para la descarga y ahora selecciona "Subir datos al servidor OSM".

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Si quieres guardar tu trabajo y no tienes acceso a Internet, puedes guardarlo en un archivo .osm compatible con JOSM y subirlo más tarde con Vespucci o con JOSM. 

#### Resolver conflictos en las subidas

Vespucci tiene un solucionador de conflictos simple. Sin embargo, si sospechas que hay problemas importantes con tus ediciones, exporta los cambios a un archivo .osc (elemento del menú "Exportar" en el menú "Transferir"), corrígelos y súbelos con JOSM. Consulta la ayuda detallada sobre [resolución de conflictos](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Usar GPS y pistas GPX

Con la configuración estándar, Vespucci intentará habilitar el GPS (y otros sistemas de navegación basados en satélites) y recurrirá a determinar la posición mediante la llamada "ubicación de red" si esto no es posible. Este comportamiento asume que, en el uso normal, tienes tu dispositivo Android configurado para usar solo ubicaciones generadas por GPX (para evitar el seguimiento), es decir, que tienes desactivada la opción eufemísticamente llamada "Mejorar la precisión de la ubicación". Si quieres habilitar la opción pero quieres evitar que Vespucci recurra a la "ubicación de red", debes desactivar la opción correspondiente en las [Preferencias avanzadas](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Para grabar una pista GPX y mostrarla en tu dispositivo, selecciona la opción "Iniciar pista GPX" en el menú ![GPS](../images/menu_gps.png). Esto agregará una capa a la pantalla con la pista grabada actual; puedes subir y exportar la pista desde la entrada en el [control de capas](Main%20map%20display.md). Se pueden agregar más capas desde archivos GPX locales y pistas descargadas de la API de OSM.

Nota: De forma predeterminada, Vespucci no grabará datos de elevación con tu pista GPX debido a algunos problemas específicos de Android. Para habilitar la grabación de elevación, instala un modelo gravitacional o, más sencillo, ve a las [Preferencias avanzadas](Advanced%20preferences.md) y configura la entrada NMEA.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci admite la descarga, los comentarios y el cierre de notas de OSM (anteriormente errores de OSM) y la funcionalidad equivalente para los "errores" producidos por la [herramienta de control de calidad OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos tienen que descargarse explícitamente o puedes usar la función de descarga automática para acceder a los elementos en tu área inmediata. Una vez editados o cerrados, puedes subir el error o la nota inmediatamente o subir todo a la vez. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtrado

Además de habilitar globalmente la visualización de notas y errores, puedes configurar un filtro de visualización de grano grueso para reducir el desorden. Se puede acceder a la configuración del filtro desde la entrada de la capa de tareas en el [control de capas](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Modo de interiores

Mapear en interiores es un desafío debido a la gran cantidad de objetos que, con mucha frecuencia, se superpondrán entre sí. Vespucci tiene un modo interior dedicado que te permite filtrar todos los objetos que no están en el mismo nivel y que agregará automáticamente el nivel actual a los nuevos objetos que se creen allí.

Puedes habilitar el modo manteniendo pulsado el elemento de bloqueo; consulta [Bloquear, desbloquear, cambio de modo](#lock) y seleccionando la entrada de menú correspondiente.

<a id="c-mode"></a>

## Modo-C

En el Modo C, solo se muestran los objetos que tienen un indicador de advertencia establecido; esto facilita la detección de objetos que tienen problemas específicos o coinciden con comprobaciones configurables. Si se selecciona un objeto y se inicia el Editor de propiedades en Modo C, se aplicará automáticamente el preset que mejor coincida.

Puedes habilitar el modo manteniendo pulsado el elemento de bloqueo; consulta [Bloquear, desbloquear, cambio de modo](#lock) y seleccionando la entrada de menú correspondiente.

### Configurar comprobaciones

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Entradas de revisión

Las entradas de revisión tienen las siguientes propiedades:

* **Clave**: clave de la etiqueta de interés.
* **Valor**: valor que debe tener la etiqueta de interés; si está vacío, se ignorará el valor de la etiqueta.
* **Antigüedad**: cuántos días después de la última modificación del elemento se debe revisar el elemento. Si hay una etiqueta _check_date_ presente, se utilizará; de lo contrario, se utilizará la fecha en que se creó la versión actual. Si se establece el valor en cero, la comprobación simplemente coincidirá con la clave y el valor.
* **Expresión regular**: si está marcada, se asume que **Valor** es una expresión regular de JAVA.

**Clave** y **Valor** se comparan con las etiquetas _existentes_ del objeto en cuestión.

El grupo _Anotaciones_ en los preset estándar contiene un elemento que agregará automáticamente una etiqueta _check_date_ con la fecha actual.

#### Entradas de comprobación

Las entradas de comprobación tienen las siguientes dos propiedades:

* **Clave**: clave que debe estar presente en el objeto según el preset coincidente.
* **Requerir opcional**: requiere la clave incluso si la clave está en las etiquetas opcionales del preset coincidente.

Esta comprobación funciona determinando primero el preset coincidente y luego comprobando si **Clave** es una clave "recomendada" para este objeto según el preset. **Requerir opcional** ampliará la comprobación a las etiquetas que son "opcionales" en el objeto. Nota: actualmente no se comprueban los presets enlazados.

## Filtros

### Filtro basado en etiquetas

El filtro se puede habilitar desde el menú principal; luego se puede cambiar tocando el icono del filtro. Puedes encontrar más documentación aquí: [Filtro de etiquetas](Tag%20filter.md).

### Filtro basado en presets

Como alternativa a lo anterior, los objetos se filtran por presets individuales o por grupos de presets. Al tocar el icono del filtro, se mostrará un cuadro de diálogo de selección de presets similar al utilizado en otras partes de Vespucci. Se pueden seleccionar presets individuales con un clic normal, grupos de presets con un clic largo (un clic normal entra en el grupo). Puedes encontrar más documentación aquí: [Filtro de presets](Preset%20filter.md).

## Personalizar Vespucci

Muchos aspectos de la app se pueden personalizar; si buscas algo específico y no lo encuentras, el [sitio web de Vespucci](https://vespucci.io/) se puede buscar y contiene información adicional sobre lo que está disponible en el dispositivo.

<a id="layers"></a>

### Ajustes de capa

Puedes cambiar los ajustes de capa a través del control de capas (menú de "hamburguesa" en la esquina superior derecha); todos los demás ajustes se pueden acceder a través del botón de preferencias del menú principal. Se pueden habilitar, deshabilitar y ocultar temporalmente las capas.

Tipos de capas disponibles:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Puedes encontrar más información en la sección sobre la [visualización del mapa](Main%20map%20display.md).

#### Preferencias

* Mantener la pantalla encendida. Predeterminado: desactivado.
* Área de arrastre de nodo grande. Mover nodos en un dispositivo con entrada táctil es problemático, ya que tus dedos oscurecerán la posición actual en la pantalla. Activar esto proporcionará un área grande que se puede usar para arrastrar fuera del centro (la selección y otras operaciones aún utilizan el área de tolerancia táctil normal). Predeterminado: desactivado.

Puedes encontrar la descripción completa aquí: [Preferencias](Preferences.md).

#### Preferencias avanzadas

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

Puedes encontrar la descripción completa aquí: [Preferencias avanzadas](Advanced%20preferences.md).

## Reporting and Resolving Issues

Si Vespucci falla o detecta un estado inconsistente, se te pedirá que envíes el volcado de memoria. Hazlo si eso sucede, pero solo una vez por situación específica. Si quieres proporcionar más información o abrir un problema para una solicitud de función o similar, hazlo aquí: [Rastreador de problemas de Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). La función "Enviar comentarios" del menú principal abrirá un nuevo problema e incluirá la información relevante de la aplicación y el dispositivo sin necesidad de escribir más.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


