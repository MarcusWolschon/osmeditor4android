# Introdución ó Vespucci

O Vespucci é un editor do OpenStreetMap con tódalas funcións que admite a maioría das operacións que fornecen os editores de escritorio. Foi probado con éxito no Android de Google 2.3 a 7.0 e varias variantes baseadas na AOSP. Un consello: mentres que as capacidades do dispositivo móbil atópanse cos seus rivais de escritorio, de xeito especial os dispositivos máis antigos teñen a memoria moi limitada dispoñíbel e adoitan ser moi lentos. Debe ter en conta isto cando empregue o Vespucci e manteña, por exemplo, as áreas que estás a editar nun tamaño razoábel. 

## Empregar por primeira vez

No inicio do Vespucci amosa o cadro do diálogo "Baixar noutra localización"/"Carregar área". Se vostede ten as coordenadas que se amosan e desexa baixar de xeito inmediato, pode escolle-la opción axeitada e inseri-lo raio ó redor da localización que desexa baixar. Non escolla unha área ampla nos dispositivos máis lentos. 

De xeito alternativo, pode pecha-lo diálogo premendo o botón "Ir ó mapa" e afastar ou achegar cara unha localización onde desexe editar e baixa-los datos (ollar máis adiante: "Edición co Vespucci").

## Editando co Vespucci

Dependendo do tamaño da pantalla e da idade do seu dispositivo, as accións de edición poden ser accesíbeis de xeito directo a través das iconas na barra superior, a través dun menú despregábel á dereita da barra superior, dende a barra inferior (se está presente) ou a través da tecla do menú.

<a id="download"></a>

### Estasen a baixar datos do OSM

Selecciona a icona de transferencia! [Transfer](../images/menu_transfer.png) ou o "Transfer" artigo do menu. Isto amosará sete opcións:

* **Baixa-la vista actual**: baixe a área visíbel na pantalla e substitúa os datos existentes *(precisa dunha conexión á rede)*
* **Engadi-la vista actual para baixar**: baixe a área visíbel na pantalla e fúndea cos datos existentes *(precisa dunha conexión á rede)*
* **Baixar noutra localización**: amosa un formulario que lle permite inserir coordenadas, procurar unha localización ou emprega-la ubicación actual e, deseguido, baixar unha área arredor desa ubicación *(precisa dunha conexión á rede)*
* **Subir datos ó servidor do OSM**: sube edicións ó OpenStreetMap *(precisa dunha autenticación)* *(precisa dunha conexión á rede)*
* **Autobaixada**: baixe unha área ó redor da ubicación xeográfica actual de xeito automático *(precisa dunha conexión á rede)* *(require GPS)*
* **Ficheiro...**: gardar e carregar información no OSM cara/dende ficheiros do dispositivo.
* **Nota/Erros...**: baixar (de xeito automático e manual) Notas e "Erros" do OSM dende ferramentas QA ferramentas (actualmente OSMOSE) *(precisa dunha conexión á rede)*

O xeito máis sinxelo de baixar datos ó dispositivo é achegar e marca-la localización que desexa editar e despois escoller "Baixar vista actual". Pode achegar empregando xestos, coma os botóns de achegamento ou os botóns de control do volume do dispositivo. O Vespucci tería que baixar entón datos da vista actual. Non se precisa dunha autenticación para baixar datos no seu dispositivo.

### Estase a editar

<a id="lock"></a>

#### Alternancia do modo bloquear e desbloquear

Para evitar edicións accidentais Vespucci comeza no modo "bloqueado", un modo que só permite achegar e mover o mapa. Tócao! [Locked](../images/locked.png) icona para desbloquear a pantalla. 

Una premida longa na icona do candeado amosará un menú con 4 opcións:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Premido único, dobre premido e premido longo

De xeito predeterminado, os nós e as vías seleccionábeis teñen unha área laranxa ó redor dela, indicando de xeito aproximado onde ten que premer para escoller un obxecto. Ten tres opcións:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

É unha boa estratexia achega-lo mapa se tenta editar unha área con alta densidade.

O Vespucci ten un bo sistema "desfacer/refacer" así que non teñas medo de experimentar no teu dispositivo, pero non subas e gardes datos de proba ó chou.

#### Selección / Deselección (premido único e "menú de selección")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

Teña en conta que se tenta escoller un obxecto e Vespucci determina que a selección podería significar varios obxectos (coma un nó nunha vía ou outros obxectos sobrepostos) presentará un menú de selección: prema o obxecto que desexe seleccionar e o obxecto é seleccionado. 

Os obxectos seleccionados son indicados a través dun delgado bordo amarelo. O bordo amarelo pode ser difícil de detectar, dependendo do fondo do mapa e do factor de achegamento. Unha vez feita unha selección, verás unha notificación que confirma a selección.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Obxectos escollidos: Editando etiquetas

Un segundo premido no obxecto seleccionado abre o editor de etiquetas e pode editar as etiquetas asociadas ó obxecto.

Teña en conta que para sobreponer obxectos (coma un nó nunha vía) o menú de selección volve a subir por segunda vez. Ó escolle-lo mesmo obxecto aparecerá o editor de etiquetas; escoller outro obxecto sinxelamente escolle o outro obxecto.

#### Obxectos seleccionados: movendo un nó ou camiño

Despois de seleccionar un obxecto, pode moverse. Teña en conta que os obxectos poden ser arrastrados/movidos só cando están seleccionados. Sinxelamente arrastre preto (isto é, dentro da zona de tolerancia) o obxecto seleccionado para movelo. Se escolle a área de arrastre grande nos axustes, obtén unha grande área ó redor do nó escollido que facilita a posicionamento do obxecto. 

#### Adding a new Node/Point or Way 

On first start the app launches in "Simple mode", this can be changed in the main menu by un-checking the corresponding checkbox.

##### Simple mode

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Unha vez que podes ver o símbolo da mira, tes estas opcións:

* Premano mesmo lugar.
    *Se o punto da mira non está preto dun nó, premendo a mesma localización novamente crea un novo nó. Se vostede está preto dunha vía (pero non preto dun nó), o novo nó estará na vía (e conectado á vía).
    * Se o punto da mira está preto dun nó (é dicir, dentro da zona de tolerancia do nó), premer na mesma localización só selecciona o nó (e se abre o editor de etiquetas. Non se crea ningún nó novo. A acción é a mesma que a selección anterior.
* Prema noutro lugar. Premer noutro lugar (fóra da zona de tolerancia da cruz) engádese un segmento da vía dende a posición orixinal até a posición actual. Se o punto da mira ficaba preto dunha vía ou nó, o novo segmento estará conectado a ese nó ou a outra vía.

Sinxelamente prema a pantalla onde desexa engadir máis nó do camiño. Para rematar, prema no nó derradeiro dúas veces. Se o nó derradeiro está situado nunha vía ou nó, o segmento ficará conectado de xeito automático ó camiño ou ó nó. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Estase a engadir unha área

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _vías pechadas ("polígonos")_: A variante de área máis sinxela e máis común, son vías que teñen un primeiro e derradeiro nó compartido que forman un "anel" pechado (por exemplo, a maioría dos edificios son deste tipo). Son moi doados de crear no Vespucci, sinxelamente conéctate ó primeiro nó cando remate de debuxar a área. Nota: a interpretación do camiño pechado depende da súa etiquetaxe: por exemplo, se un camiño pechado está etiquetado coma un edificio considerado coma unha área, se está marcado coma unha rotonda, non vai. Nalgunhas situacións nas que ámbalas dúas interpretacións poden ser válidas, unha etiqueta de "área" pode aclarar o uso desexado.
* _multi-polígonos_: Algunhas áreas teñen múltiples partes, buracos e aneis que non se poden representar cunha soa vía. O OSM emprega un tipo específico de relación (o noso obxecto de propósito xeral que pode modelar as relacións entre elementos) para evitar isto, un multipolígono. Un multipolígono pode ter varios aneis "externos" e múltiples aneis "internos". Cada anel pode ser unha vía pechada coma se describe enriba, ou varias vías individuais que teñen nós nos extremos comúns. Mentres os grandes multipolígonos son difíciles de manexar con calquera ferramenta, os pequenos non son difíciles de crear no Vespucci.
* _beiramar_: Para obxectos moi grandes, continentes e illas, mesmo o modelo multipolígono non funciona de xeito satisfactorio. Para formas natural=coastline tomamos a semántica dependente de dirección: a terra está na beira esquerda do camiño, o auga na beira dereita. Un efecto secundario disto é que, en xeral, non debe revertir a dirección dunha vía co etiquetado da costa. Pode atopar máis información na [wiki do OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Estase a mellora-la xeometría das vías

Se achegaches o suficiente na vía seleccionada, verás un pequeno "x" no medio dos segmentos que son o suficientemente longos. Arrastrando o "x" creará un nó na vía dese lugar. Nota: para evitar a creación de nós de xeito accidental, a área de tolerancia táctil para esta operación é bastante pequena.

#### Cortar, copiar e colar

Pode copiar ou cortar nós e vía seleccionados e, deseguido, colar ou pegar unha ou varias veces nunha nova ubicación. O corte conservará o ID e versión do OSM. Para colar prema a posición na que desexa colar (verá unha liña cruzada marcando a localización). Deseguido, escolla "Colar ou pegar" no menú.

#### Estasen a engadir enderezos de xeito eficiente

Vespucci ten unha función de "engadir etiquetas de enderezos" que intenta facer que os enderezos de topografía sexan máis eficientes. Pode ser seleccionado:

* despois dunha longo premido: O Vespucci engadirá un nó na ubicación e fará unha mellor adiviñación do número da casa e engadirá as etiquetas de enderezos que estivo a empregar recentemente. Se o nó está nunha beira do edificio engadirá de xeito automático unha etiqueta "entrada = si" ó nó. O editor de etiquetas abrirase para o obxecto en cuestión e permitirá que faga as mudanzas precisadas.
* no modos nó/vía seleccionados: O Vespucci engadirá as etiquetas de enderezo coma se menciona arriba e comezará o editor das etiquetas.
* no editor das etiquetas.

A predicción de números de casa normalmente require que polo menos dous números de casa a cada lado da estrada para ser ingresados ao traballo, cantos máis números presentes nos datos mellor.

Considerar usando isto con modo [Auto-download](#download).  

#### Estasen a engadir restriccións de xiro

O Vespucci ten un xeito axiña de engadir restricións de xiro. Se é necesario, dividirase de xeito automático e pediralle que volva selecciona-los elementos. 

* Seleccione un camiño cunha etiqueta de autoestrada (as restricións de xiro só se poden engadir ás autoestradas, se hai que facer isto por outros xeitos, empregue o modo xenérico "crear relacións").
* Escolla "Engadir restricións" no menú
* Escolla o nó "vía" ou o camiño (só os elementos "via" posíbeis terán a área táctil amosada)
* Escolla o modo "a" (é posíbel volver a dobrar e configura-lo elemento "a" no elemento "dende", o Vespucci asumirá que está engadindo un xiro sen restricións no_u_turn)
* Estabeleza o tipo de restrición

### O Vespucci no modo "bloqueado"

Cando se amosa o bloqueo vermello, tódalas accións non editadas están dispoñíbeis. Adicionalmente, unha prensa longa ou próxima a un obxecto amosará a pantalla de información detallada se é un obxecto OSM.

### Estasen a garda-las súas mudanzas

*(require conectividade na rede)*

Escolla o mesmo botón ou elemento do menú que fixo para a baixada e agora escolla "Subir datos no servidor do OSM".

O Vespucci admite a autorización do OAuth e o método clásico de nome do usuario e contrasinal. O OAuth é preferíbel xa que evita o envío de contrasinais nun texto plano.

As novas instalacións do Vespucci terán o OAuth activado por defecto. No primeiro intento de subir datos modificados, carrega unha páxina do sitio web do OSM. Despois de iniciar sesión (a través dunha conexión cifrada) pediráselle que autorice ó Vespucci a editar empregando a súa conta. Se queres ou precisas autoriza-lo acceso do OAuth á súa conta antes de editar hai un elemento correspondente no menú "Ferramentas".

Se desexa garda-lo seu traballo e non ten acceso á Internet, pode gardar nun ficheiro .osm compatíbel co JOSM e subilo máis tarde co Vespucci ou co JOSM. 

#### Estasen a resolver conflitos ó subir

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Estase a empregar GPS

Podes usar Vespucci para crear unha pista GPX e visualizalo no teu dispositivo. Ademais, pode amosar a posición actual do GPS (configurar "Mostrar localización" no menú GPS) e/ou ter a pantalla en torno e seguir a posición (configure "Seguir posición GPS" no menú GPS). 

Se ten este último conxunto, mover a pantalla de xeito manual ou editar fará que o modo "Seguer GPS" se desactive e que a frecha azul do GPS troque dun esquema cara unha frecha chea. Para voltar axiña ó modo "seguer", sinxelamente prema o botón GPS ou volva a verificar a opción do menú.

## Notas e erros

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

No mapa, as notas e os erros están representados por unha pequena icona de erro! [Bug](../images/bug_open.png), os verdes están pechados/resoltos, os que foron creados ou editados son azuis e o amarelo indica que aínda está activo e non foi modificado. 

A visualización de erros do OSMOSE fornecerá unha ligazón ó obxecto afectado en azul, premendo a ligazón seleccionará o obxecto, centrará a pantalla nel e baixará a área previamente se fose necesario. 

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

## Modo-C

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Estasen a configura-las comprobacións

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a check_date field is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

#### Comprobar entradas

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtros

###  Filgros baseados en Etiquetas

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Filtro baseado nos predefinidos

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Persoalizando Vespucci

### Axustes que ti debes querer trocar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Axustes avanzados

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Informar de problemas

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


