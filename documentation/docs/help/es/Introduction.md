# Introducción a Vespucci

Vespucci es un editor OpenStreetMap con todas las funciones que admite la mayoría de las operaciones que proporcionan los editores de escritorio. Se ha probado con éxito en Android 2.3 a 10.0 de Google y en varias variantes basadas en AOSP. Una advertencia: aunque las capacidades de los dispositivos móviles se han puesto al día con sus rivales de escritorio, particularmente los dispositivos más antiguos tienen una memoria muy limitada disponible y tienden a ser bastante lentos. Debe tener esto en cuenta al usar Vespucci y mantener, por ejemplo, las áreas que está editando a un tamaño razonable. 

## Utilizar por primera vez

Al iniciarse, Vespucci le muestra el cuadro de diálogo "Descargar otra ubicación" / "Área de carga" después de solicitar los permisos necesarios y mostrar un mensaje de bienvenida. Si tiene coordenadas mostradas y desea descargar de inmediato, puede seleccionar la opción adecuada y establecer el radio alrededor de la ubicación que desea descargar. No seleccione un área grande en dispositivos lentos. 

Como alternativa, puede cerrar el cuadro de diálogo presionando el botón "Ir al mapa" y desplazarse y hacer zoom a la ubicación que desea editar y luego descargar los datos (Ver más abajo: "Edición con Vespucci").

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

* **Normal** - el modo de edición por defecto, se pueden añadir nuevos objetos, editar los existentes, desplazados o eliminados. Se muestra un icono sencillo de un candado blanco.
* **Sólo etiquetado** - al seleccionar un objeto existente se iniciará el Editor de Propiedades, una pulsación larga en la pantalla principal añadirá objetos, pero no funcionarán otras operaciones geométricas. Se mostrará el icono de un candado blanco con una "T".
* **Interiores** - activa el modo Interiores, ver [modo Interiores](#indoor). Se muestra el icono de un candado blanco con una "I".
* **Modo-C** - activa el Modo-C, sólo se muestran los objetos que tengan una señal de aviso, ver [Modo-C](#c-mode). Se muestra el icono de un candado blanco con una "C".

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

Una vez que haya seleccionado un objeto, puede moverlo. Tenga en cuenta que los objetos sólo se pueden arrastrar/mover cuando se seleccionan. Simplemente arrastre cerca (es decir, dentro de su zona de tolerancia) el objeto seleccionado para moverlo. Si selecciona el área de arrastre grande en las preferencias, obtiene un área grande alrededor del nodo seleccionado que facilita la colocación del objeto. 

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

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

También puede usar un elemento de menú: consulte [Creación de nuevos objetos](Creating%20new%20objects.md) para obtener más información.

#### Añadiendo un Área

Actualmente OpenStreetMap no tiene un objeto tipo "área" al contrario que otros sistemas de geo-datos. El editor en línea "iD" intenta crear una abstracción de área a partir de los elementos OSM subyacentes que funciona bien en algunas circunstancia y no tanto en otras. Actualmente Vespucci no intenta hacer nada similar, así que necesitará conocer un poco sobre la forma en que se representan las áreas.

* _rutas cerradas («polígonos»)_: la variante de área más simple y más común, son rutas que tienen un primer y último nodo compartido formando un «anillo» cerrado (por ejemplo, la mayoría de los edificios son de este tipo). Estos son muy fáciles de crear en Vespucci, simplemente conéctese de nuevo al primer nodo cuando haya terminado de dibujar el área. Nota: la interpretación del camino cerrado depende de su etiquetado: por ejemplo, si un camino cerrado se etiqueta como un edificio, se considerará un área, si se etiqueta como una rotonda, no. En algunas situaciones en las que ambas interpretaciones pueden ser válidas, una etiqueta de «área» puede aclarar el uso previsto.
* _multi-polígonos_: algunas áreas tienen múltiples partes, agujeros y anillos que no se pueden representar de una sola manera. OSM usa un tipo específico de relación (nuestro objeto de propósito general que puede modelar las relaciones entre los elementos) para sortear esto, un multi-polígono. Un multi-polígono puede tener múltiples anillos «externos» y múltiples anillos «internos». Cada anillo puede ser cerrado como se describe anteriormente, o múltiples formas individuales que tienen nodos finales comunes. Mientras que los grandes multi-polígonos son difíciles de manejar con cualquier herramienta, los pequeños no son difíciles de crear en Vespucci. 
* _costas_: para objetos muy grandes, continentes e islas, incluso el modelo de multi-polígonos no funciona de manera satisfactoria. Para las formas naturales=litorales asumimos una semántica dependiente de la dirección: la tierra está en el lado izquierdo del camino, el agua en el lado derecho. Un efecto secundario de esto es que, en general, no debe invertir la dirección de una ruta con el etiquetado de la costa. Puede encontrarse más información en la [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorando la geometría de vías

Si hace el suficiente zoom en una ruta seleccionada, verá pequeños segmentos «x» en medio de la ruta que son lo suficientemente largos. Al arrastrar la «x» se creará un nodo en la ruta en esa ubicación. Nota: para evitar la creación accidental de nodos, el área de tolerancia táctil para esta operación es bastante pequeña.

#### Cortar, copiar y pegar

Usted puede copiar o cortar los nodos y caminos seleccionados, y después pegarlos una o múltiples veces en una nueva ubicación. Cortar retendrá la ID y la versión de osm. Para pegar presione un rato la ubicación en la que desea pegar (verá una línea cruzada creando la ubicación). Después seleccione "Pegar" desde el menú.

#### Añadiendo direcciones de manera eficiente

Vespucci tiene una función ![Dirección](../images/address.png) "agregar etiquetas de dirección" que intenta hacer que las direcciones de topografía sean más eficientes al predecir el número de casa actual. Se puede seleccionar:

* después de una pulsación larga (_solo en modo no simple:): Vespucci agregará un nodo en la ubicación y adivinará el número de la casa y agregará las etiquetas de dirección que ha estado utilizando últimamente. Si el nodo está en el contorno de un edificio, agregará automáticamente una etiqueta "entrance=yes" al nodo. El editor de etiquetas se abrirá para el objeto en cuestión y le permitirá realizar los cambios necesarios.
* en los modos seleccionados nodo / vía: Vespucci agregará etiquetas de dirección como se indicó anteriormente e iniciará el editor de etiquetas.
* en el editor de propiedades.

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

Vespucci tiene un solucionador de conflictos simple. Sin embargo, si sospecha que hay problemas importantes con sus ediciones, exporte sus cambios a un archivo .osc (elemento de menú "Exportar" en el menú "Transferencia") corríjalos y cárguelos con JOSM. Consulte la ayuda detallada sobre [resolución de conflictos](Conflict%20resolution.md).  

## Usando GPS

Puede utilizar Vespucci para crear una pista GPX y mostrarla en su dispositivo. Además puede mostrar la ubicación GPS actual (seleccione "Mostrar ubicación" en el menú GPS) y/o centrar la pantalla y seguir su posición (seleccione "Seguir ubicación GPS" en el menú GPS). 

Si tiene este último conjunto, mover la pantalla manualmente o editar hará que se deshabilite el modo «Seguir GPS» y la flecha azul del GPS cambiará de un esquema a una flecha llena. Para volver rápidamente al modo «seguir», simplemente toque el botón GPS o vuelva a verificar la opción de menú.

## Notas y errores

Vespucci soporta la descarga, comentar y cerrar Notas de OSM (anteriormente Errores OSM) y la funcionalidad equivalente para "Errores" producidos por la [herramienta de aseguramiento de calidad OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos deben de, o bien haber sido descargados explícitamente, o puedes usar el servicio de auto descarga para acceder a los elementos en su área inmediata. Una vez editados o cerrados, puede o bien subir el error o Nota al servidor de forma inmediata o subirlos todos de una vez.

En el mapa las Notas y errores son representados mediante un pequeño icono de error ![Error](../images/bug_open.png); los verdes son cerrados/resueltos, los azules han sido creados o editados por usted, y los amarillos indican que aún está activo y no ha sido cambiado. 

La exposición de error OSMOSE proveerá un enlace al objeto azul afectado, tocar el enlace seleccionará el objeto, centrará la pantalla sobre él y descargará el área con antelación si fuera necesario. 

### Filtrado

Además de habilitar globalmente la visualización de notas y errores, puede configurar un filtro de visualización de grano grueso para reducir el desorden. En [Preferencias avanzadas] (Avanced%20preferences.md) puede seleccionar individualmente:

* Notas
* Error de OSMOSE
* Advertencia de OSMOSE
* Problema menor de OSMOSE
* Personalizado

<a id="indoor"></a>

## Modo de interiores

El mapeo en interiores es un desafío debido a la gran cantidad de objetos que muy a menudo se superponen entre sí. Vespucci tiene un modo interior dedicado que le permite filtrar todos los objetos que no están en el mismo nivel y que agregará automáticamente el nivel actual a los nuevos objetos creados allí.

El modo puede activarse con una pulsación larga en el icono del cansado, ver [Cambiando de modo, bloquear, desbloquear](#lock) y seleccionando la correspondiente entrada de menú.

<a id="c-mode"></a>

## Modo-C

En el Modo-C sólo se muestran los objetos que tienen activa una señal de aviso, esto facilita resaltar objetos que tienen problemas específicos o coinciden con chequeos configurables. Si un objeto es seleccionado y el Editor de Propiedades iniciado en Modo-C se aplicará automáticamente el predefinido que mejor se ajuste.

El modo puede activarse con una pulsación larga en el icono del cansado, ver [Cambiando de modo, bloquear, desbloquear](#lock) y seleccionando la correspondiente entrada de menú.

### Configurando chequeos

Actualmente hay dos comprobaciones configurables (hay una comprobación para las etiquetas FIXME y una prueba para las etiquetas de tipo faltantes en las relaciones que actualmente no son configurables) ambas se pueden configurar seleccionando "Configuración del validador" en las "Preferencias". 

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

* **Clave**: clave que debe estar presente en el objeto de acuerdo con el predefinido correspondiente.
* **Requerir opcional** - Requerir la clave incluso si la clave está en las etiquetas opcionales del predefinido coincidente.

Esta verificación funciona primero determinando el predefinido coincidente y luego verificando si **Clave** es una clave "recomendada" para este objeto de acuerdo con el predefinido, **Requerir opcional** expandirá la verificación a las etiquetas que son "opcionales* en el objeto. Nota: los predefinidos vinculados actualmente no están comprobados.

## Filtros

### Filtro basado en etiqueta

El filtro se puede habilitar desde el menú principal, luego se puede cambiar tocando el icono del filtro. Puede encontrar más documentación aquí [Filtro de etiquetas](Tag%20filter.md).

### Filtro basado en predefinido

Una alternativa a lo anterior, los objetos se filtran en predefinidos individuales o en grupos de predefinidos. Al tocar el icono del filtro, se mostrará un cuadro de diálogo de selección predefinidos similar al que se usa en otros lugares de Vespucci. Los predefinidos individuales se pueden seleccionar con un clic normal, los grupos de predefinidos con un clic largo (el clic normal ingresa al grupo). Puede encontrar más documentación aquí [Filtro de predefinidos] (Preset%20filter.md).

## Personalizando Vespucci

Muchos aspectos de la aplicación se pueden personalizar, si está buscando algo específico y no puede encontrarlo, [el sitio web de Vespucci](https://vespucci.io/) se puede buscar y contiene información adicional sobre lo que está disponible en el dispositivo.

### Configuraciones de capa

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

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


