# Vespucci Introducción

Vespucci é un editor OpenStreetMap con tódalas funcións que admite a maioría das operacións que proporcionan os editores de escritorio. Foi probado con éxito no Android de Google 2.3 a 7.0 e varias variantes baseadas na AOSP. Unha palabra de precaución: mentres as capacidades do dispositivo móbil atópanse cos seus rivais de escritorio, especialmente os dispositivos máis antigos teñen memoria moi limitada dispoñible e adoitan ser bastante lentos. Debes ter en conta a miña conta ao usar Vespucci e manter, por exemplo, as áreas que estás a editar nun tamaño razoable. 

## Primeira vez de uso

No inicio Vespucci amosa a caixa de diálogo "Descargar outra localización" / "Cargar área". Se tes as coordenadas que se amosan e queres descargar de xeito inmediato, podes seleccionar a opción adecuada e configurar o radio ao redor da localización que queres descargar. Non seleccione unha área grande en dispositivos lentos. 

Alternativamente, pode descartar o diálogo premendo o botón "Ir ao mapa" e xirar e ampliar a unha localización onde desexa editar e descargar os datos (ver a continuación: "Edición con Vespucci").

## Editando con Vespucci

Dependendo do tamaño da pantalla e da idade do seu dispositivo, as accións de edición poden ser accesibles directamente a través das iconas na barra superior, a través dun menú desplegable á dereita da barra superior, desde a barra inferior (se está presente) ou a través da tecla de menú.

<a id="download"></a>

### Descargando información OSM

Selecciona a icona de transferencia! [Transfer](../images/menu_transfer.png) ou o "Transfer" artigo do menu. Isto amosará sete opcións:

* **Baixar vista actual** - descargue a área visible na pantalla e substitúa os datos existentes *(require conexión a internet)*
* **Engadir vista actual para descargar** - descargue a área visible na pantalla e fúndea cos datos existentes *(require conexión a internet)*
* **Descargar noutro lugar** - amosa un formulario que lle permite introducir coordenadas, buscar unha localización ou usar a posición actual e, a continuación, descargar unha área en torno a esa localización *(require conexión a internet)*
* **Subir datos ao server de OSM** - subir e editar OpenStreetMap *(require autenticación)* *(require conexión a internet)*
* **Auto baixada** - descargue unha área ao redor da situación xeográfica actual automaticamente *(require conexión a internet)* *(require GPS)*
* **Arquivo...** - salvar e cargar información en OSM a/dende arquivos do dispositivo.
* **Nota/Bugs...** - baixar (automaticamente e manualmente) OSM Notas e "Bugs" dende QA ferramentas (actualmente OSMOSE) *(require conexión a internet)*

A forma máis sinxela de descargar datos ao dispositivo é achegar e panoramizar a localización que desexa editar e logo seleccionar "Descargar vista actual". Podes achegar os xestos, os botóns de zoom ou os botóns de control de volume do dispositivo. Vespucci debería entón descargar datos para a vista actual. Non se precisa autenticación para descargar datos no dispositivo.

### Editando

<a id="lock"></a>

#### Lock, unlock, mode switching

Para evitar edicións accidentais Vespucci comeza no modo "bloqueado", un modo que só permite achegar e mover o mapa. Tócao! [Locked](../images/locked.png) icona para desbloquear a pantalla. 

A long press on the lock icon will display a menu currently offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

Toque único, dobre toque e prema longa

De xeito predeterminado, os nodos e os modos seleccionables teñen unha área en laranxa ao redor dela, indicando aproximadamente onde ten que tocar para seleccionar un obxecto. Tes tres opcións:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md)

Ista é unha boa estratexia facer zoom se ti tentas editar unha área de alta densidade.

Vespucci ten un bo sistema "desfacer/refacer" así que non teñas medo de experimentar no teu dispositivo, pero non cargues e gardes datos de proba ao chou.

#### Selección / Deselección (toque único e "menú de selección")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

Teña en conta que se intentas seleccionar un obxecto e Vespucci determina que a selección podería significar varios obxectos (como un nodo dun xeito ou outros obxectos solapados) presentará un menú de selección: toque o obxecto que desexe seleccionar e o obxecto é seleccionado. 

Os obxectos seleccionados son indicados a través dun delgado borde amarelo. O bordo amarelo pode ser difícil de detectar, dependendo do fondo do mapa e do factor de zoom. Unha vez feita unha selección, verás unha notificación que confirma a selección.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Seleccione obxectos: Editando etiquetas

Un segundo toque no obxecto seleccionado abre o editor de etiquetas e pode editar as etiquetas asociadas ao obxecto.

Teña en conta que para superponer obxectos (como un nodo de certa forma) o menú de selección volve a subir por segunda vez. Ao seleccionar o mesmo obxecto aparecerá o editor de etiquetas; Seleccionar outro obxecto simplemente selecciona o outro obxecto.

#### Obxectos seleccionados: mover un nodo ou camiño

Despois de seleccionar un obxecto, pode moverse. Teña en conta que os obxectos poden ser arrastrados/movidos só cando están seleccionados. Simplemente arrastre preto (isto é, dentro da zona de tolerancia de) o obxecto seleccionado para movelo. Se selecciona a área de arrastre grande nas preferencias, obtén unha gran área ao redor do nodo seleccionado que facilita a posicionamento do obxecto. 

#### Engadir un novo Nodo/Punto ou Camiño (prema prolongadamente)

Manteña premido onde quere que o nodo sexa ou o xeito de comezar. Verás un símbolo de "cruzamento" negro. 
*Se desexa crear un novo nodo (non conectado a un obxecto), faga clic fóra de obxectos existentes.
* Se queres estender un camiño, faga clic dentro da "zona de tolerancia" do camiño (ou un nodo no camiño). A zona de tolerancia está indicada polas áreas ao redor dun nó ou camiño.

Unha vez que podes ver o símbolo da mira, tes estas opcións:

* Toque no mesmo lugar.
    *Se a mira non está preto dun nodo, tocando a mesma localización nuevamente crea un novo nodo. Se estás preto dun xeito (pero non preto dun nodo), o novo nodo estará en camiño (e conectado ao modo).
    * Se a mira está preto dun nodo (é dicir, dentro da zona de tolerancia do nodo), tocar na mesma localización só selecciona o nodo (e se abre o editor de etiquetas. Non se crea ningún nodo novo. A acción é a mesma que a selección anterior.
* Toca outro lugar. Tocar noutro lugar (fóra da zona de tolerancia da cruz) engade un segmento de forma desde a posición orixinal ata a posición actual. Se a mira estaba preto dun xeito ou nodo, o novo segmento estará conectado a ese nodo ou a outro xeito.

Simplemente toque a pantalla onde desexa engadir máis nodos do camiño. Para rematar, toque o nodo final dúas veces. Se o nodo final está situado nun xeito ou nodo, o segmento estará conectado automaticamente ao camiño ou ao nodo. 

You can also use a menu item: See [Creating new objects](/Creating%20new%20objects.md) for more information.

#### Engadindo un Área

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: A variante de área máis sinxela e máis común, son formas que teñen un primeiro e último nodo compartido que forman un "anel" pechado (por exemplo, a maioría dos edificios son deste tipo). Son moi fáciles de crear en Vespucci, simplemente conéctate ao primeiro nodo cando termine de debuxar a área. Nota: a interpretación do camiño pechado depende da súa etiquetaxe: por exemplo, se un camiño pechado está etiquetado como un edificio considerado como unha área, se está marcado como unha rotonda, non vai. Nalgunhas situacións nas que ambas interpretacións poden ser válidas, unha etiqueta de "área" pode aclarar o uso desexado.
* _multi-ploygons_:Algunhas áreas teñen múltiples partes, buratos e aneis que non se poden representar con só un xeito. OSM usa un tipo específico de relación (o noso obxecto de propósito xeral que pode modelar as relacións entre elementos) para evitar isto, un multipolígono. Un multipolígono pode ter varios aneis "externos" e múltiples aneis "internos". Cada anel pode ser un xeito pechado como se describe arriba, ou varias formas individuais que teñen nós extremos comúns. Mentres os grandes multipolígonos son difíciles de manexar con calquera ferramenta, os pequenos non son difíciles de crear en Vespucci.
* _coastlines_: Para obxectos moi grandes, continentes e illas, mesmo o modelo multipolígono non funciona de forma satisfactoria. Por formas natural=coastline tomamos a semántica dependente de dirección: a terra está no lado esquerdo do camiño, o auga no lado dereito. Un efecto secundario disto é que, en xeral, non debe revertir a dirección dun xeito co etiquetado da costa. Pode atopar máis información na páxina [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mellorar a Xeometría do Camiño

Se achegaches o suficiente de forma seleccionada, verás un pequeno "x" no medio dos segmentos que son o suficientemente longo. Arrastrando o "x" creará un nodo no camiño dese lugar. Nota: para evitar a creación de nodos accidentalmente, a área de tolerancia táctil para esta operación é bastante pequena.

#### Cortar, Copiar & Pegar

Pode copiar ou cortar nodos e xeitos seleccionados e, a continuación, pegar unha ou varias veces nunha nova ubicación. O corte conservará o id e versión osm. Para pegar prema a posición na que desexa pegar (verá un pelo cruzado marcando a localización). A continuación, selecciona "Pegar" no menú.

#### Eficiente Engadindo Enderezos

Vespucci ten unha función de "engadir etiquetas de enderezos" que intenta facer que os enderezos de topografía sexan máis eficientes. Pode ser seleccionado:

* despois dunha longa prensión: Vespucci engadirá un nodo no lugar e fará unha mellor adiviñación no número da casa e engadirá as etiquetas de enderezos que estivo a usar recentemente. Se o nodo está nun contorno do edificio engadirá automaticamente unha etiqueta "entrada = si" ao nodo. O editor de etiquetas abrirase para o obxecto en cuestión e permitirá que realice as mudanzas precisadas.
* no modos nodo / modo seleccionado: Vespucci engadirá as etiquetas de enderezo como se menciona arriba e inicie o editor de etiquetas.
* no editor de etiquetas.

A predicción de números de casa normalmente require que polo menos dous números de casa a cada lado da estrada para ser ingresados ao traballo, cantos máis números presentes nos datos mellor.

Considerar usando isto con modo [Auto-download](#download).  

#### Engadindo Restriccións de Xiro

Vespucci ten un xeito rápido de engadir restricións de xiro. Se é necesario, dividirase automaticamente e pediralle que volva seleccionar elementos. 

* Seleccione un camiño cunha etiqueta de estrada (as restricións de turno só se poden engadir ás estradas, se hai que facer isto por outras formas, use o modo xenérico "crear relación").
* Selecciona "Engadir restrición" no menú
* Seleccione o nodo "vía" ou o camiño (só os elementos "via" posibles terán a área táctil amosada)
* Seleccione o modo "a" (é posible dobrar e configurar o elemento "a" no elemento "de", Vespucci asumirá que está engadindo un xiro sen restricións no_u_turn restriction)
* Establecer o tipo de restrición

### Vespucci  en modo "pechado"

Cando se amosa o bloqueo vermello, tódalas accións non editadas están dispoñíbeis. Adicionalmente, unha prensa longa ou próxima a un obxecto amosará a pantalla de información detallada se é un obxecto OSM.

### Gardar os teus Trocos

*(require conectividade na rede)*

Seleccione o mesmo botón ou elemento de menú que fixeches para a descarga e agora seleccione "Cargar datos no servidor OSM".

Vespucci admite a autorización de OAuth e o método clásico de nome de usuario e contrasinal. OAuth é preferible xa que evita o envío de contrasinais.

As novas instalacións de Vespucci terán OAuth activado por defecto. No primeiro intento de cargar datos modificados, carga unha páxina do sitio web de OSM. Despois de iniciar sesión (a través dunha conexión cifrada) pediráselle que autorice a Vespucci a editar usando a súa conta. Se queres ou necesitas autorizar o acceso de OAuth á túa conta antes de editar hai un elemento correspondente no menú "Ferramentas".

Se desexa gardar o seu traballo e non ten acceso a Internet, pode gardar nun ficheiro .osm compatíbel con JOSM e cargar máis tarde con Vespucci ou con JOSM. 

#### Resolvendo conflitos en subidas

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Usando GPS

Podes usar Vespucci para crear unha pista GPX e visualizalo no teu dispositivo. Ademais, pode amosar a posición actual do GPS (configurar "Mostrar localización" no menú GPS) e/ou ter a pantalla en torno e seguir a posición (configure "Seguir posición GPS" no menú GPS). 

Se ten este último axustado, mover a pantalla manualmente ou editar fará que o modo "seguir o GPS" se desactive e que a frecha GPS azul cambie dun esquema a unha frecha chea. Para volver rapidamente ao modo "seguir", simplemente toque o botón GPS ou volva a verificar a opción do menú.

## Notas e Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

No mapa, as notas e os erros están representados por unha pequena icona de erro! [Bug](../images/bug_open.png), os verdes están pechados/resoltos, os que foron creados ou editados son azuis e o amarelo indica que aínda está activo e non foi modificado. 

A visualización de erros de OSMOSE proporcionará unha ligazón ao obxecto afectado en azul, tocando a ligazón seleccionará o obxecto, centrará a pantalla nel e baixará a área previamente se fose necesario. 

### Filtrado

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the [Advanced preferences](Advanced%20preferences.md) you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Custom

<a id="indoor"></a>

## Modo Interior

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

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

###  Filgros baseados en Etiquetas

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Base do filtro Presente

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Persoalizando Vespucci

### Axustes que ti debes querer trocar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Preferencias Avanzadas

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reportando Problemas

Se Vespucci falla ou detecta un estado inconsistente, pediráselle que envíe ao garete. Faga isto se isto ocorre, pero por favor só unha vez por situación específica. Se desexa dar máis información ou abrir un problema para unha solicitude de funcións ou similar, faga isto aquí: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Se queres discutir algo relacionado con Vespucci, podes iniciar unha discusión sobre a [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) ou en [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


