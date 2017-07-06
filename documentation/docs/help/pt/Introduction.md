# Introdução do Vespucci

Vespucci é um editor dedicado ao OpenStreetMap que suporta a maioria das operações que os editores desktop proporcionam. Foi testado com sucesso nas versões Android 2.3 até à 7.0 e várias outras variantes AOSP. À sua consideração: Apesar das capacidades dos dispositivos móveis terem aproximado aos rivais desktops, os dispositivos mais antigos possuem uma capacidade de memória limitada e tendem a ser lentos. Deve ter isto em conta quando está a utilizar o Vespucci e deve ter em atenção à dimensão da área que está a editar, para que seja uma área com uma dimensão razoável. 

## Primeira Utilização

Ao iniciar o Vespucci, é mostrada a janela "Descarregar outra localização"/"Carregar Área". Se tiver as coordenadas a serem mostradas e quiser descarregar de imediato, pode seleccionar a opção apropriada e seleccionar o raio à volta da localização que quer descarregar. Não é recomendável descarregar áreas grandes em dispositivos lentos/antigos. 

Como alternativa, pode fechar a janela pressionando o botão "Ir para o mapa", deslocar e aproximar numa localização que quer editar e descarregar os dados (ver abaixo"Editar no Vespucci").

## Editar no Vespucci

Dependendo do tamanho do ecrã e da antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

<a id="download"></a>

### Descarregar Dados OSM

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display seven options:

* **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
* **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Download at other location** - shows a form that allows you to enter coordinates, search for a location or use the current position, and then download an area around that location *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Auto download** - download an area around the current geographic location automatically *(requires network connectivity)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

The easiest way to download data to the device is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the device.  Vespucci should then download data for the current view. No authentication is required for downloading data to your device.

### Editar

<a id="lock"></a>

#### Lock, unlock, "tag editing only", indoor mode 

To avoid accidental edits Vespucci starts in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen. 

A long press on the lock icon will enable "Tag editing only" mode which will not allow you to edit the geometry of objects or move them, this mode is indicated with a slightly different white lock icon. You can however create new nodes and ways with a long press as normal.

Another long press will enable [Indoor mode](#indoor), and one more will cycle back to the normal editing mode.

#### Single tap, double tap, and long press

By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. You have three options:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](../en/Node%20selected.md), [Way selected](../en/Way%20selected.md) and [Relation selected](../en/Relation%20selected.md).
* Double tap: Start [Multiselect mode](../en/Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](../en/Creating new objects.md)

É uma boa ideia aproximar a visualização caso a área tenha muitos objetos.

O Vespucci tem um bom sistema de "desfazer/refazer" por isso não tenha medo de fazer experiências de edição, mas por favor não envie essas experiências para o OpenStreetMap.

#### Selecting / De-selecting (single tap and "selection menu")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](../en/Multiselect.md).

Note that if you try to select an object and Vespucci determines that the selection could mean multiple objects (such as a node on a way or other overlapping objects) it will present a selection menu: Tap the object you wish to select and the object is selected. 

Selected objects are indicated through a thin yellow border. The yellow border may be hard to spot, depending on map background and zoom factor. Once a selection has been made, you will see a notification confirming the selection.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](../en/Node%20selected.md), [Way selected](../en/Way%20selected.md) and [Relation selected](../en/Relation%20selected.md).

#### Selected objects: Editing tags

A second touch on the selected object opens the tag editor and you can edit the tags associated with the object.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Selected objects: Moving a Node or Way

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Adding a new Node/Point or Way (long press)

Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Once you can see the crosshair symbol, you have these options:

* Touch in the same place.
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* Touch another place. Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is  located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](../en/Creating new objects.md) for more information.

#### Adding an Area

OpenStreetMap currently doesn't have an "area" object type contrary to other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Melhorar Geometria das Vias

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Cortar, Copiar e Colar

Pode copiar ou cortar os nós selecionados e vias, e então colar uma ou várias vezes numa nova localização. Cortar irá preservar o identificador (ID), versão e histórico do objeto. Para colar basta pressionar longamente no local desejado (verá um x marcando o lugar). E então selecione "Colar" no menu.

#### Adicionar Endereços Eficientemente

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

A adivinhação do número de porta normalmente necessita de 2 números de porta em cada lado da estrada para funcionar. Quantos mais números de porta presentes, melhor funciona.

Consider using this with the [Auto-download](#download) mode.  

#### Adicionar Restrições de Viragem

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci no modo "bloqueado"

Quando é mostrado o cadeado vermelho, estão disponíveis todas as ações de não edição. Um toque longo num objeto mostra a informação sobre o objeto, se este for um elemento do mapa.

### Gravar as Alterações

*(requer ligação à Internet)*

Carregue no mesmo botão ou item do menu que fez para descarregar e então carregue em "Enviar dados para o OSM".

O Vespucci suporta a autorização OAuth assim como o método clássico de nome de utilizador e palavra-chave. É recomendável usar o OAuth porque este envia a palavra-chave para o OSM encriptada, sendo mais seguro.

As instalações novas do Vespucci têm o OAuth ativado por defeito. Ao tentar enviar pela primeira vez dados para o OpenStreetMap, é aberta a página web do OSM. Após entrar na sua conta do OSM (numa ligação encriptada), será perguntado se quer autorizar o Vespicci a editar usando os dados da dus conta. Se quiser ou necessitar de autorizar o acesso OAuth para a sua conta antes de editar, existe o item no menu "Ferramentas".

Se quiser gravar as edições e não tiver acesso à Internet, pode gravar os dados num ficheiro .osm compatível com o JOSM e posteriormente abrir e enviar esse ficheiro através do Vespucci ou do JOSM. 

#### Resolver Conflitos ao Enviar

O Vespucci tem um solucionador de conflitos simples. Se suspeitar que há muitos problemas com a sua edição, exporte as alterações para um ficheiro .osc (no menu "Transferir" e "Exportar") podendo depois corrigir e enviar no JOSM. Veja as instruções detalhadas em [resolução de conflitos](../en/Conflict resolution.md).  

## Utilizar o GPS

Pode usar o Vespucci para criar trilhos GPS e mostrá-los no seu dispositivo. Pode também saber a localização GPS atual (ativar em "Ver localização" no menu GPS) e/ou ter no centro do ecrã a localização e seguir a localização (ativar em "Seguir posição GPS" no menu GPS). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Erros Reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue

<a id="indoor"></a>

## Indoor mode

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created their.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, "tag editing only", indoor mode](#lock).

## Filters

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](../en/Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](../en/Preset%20filter.md).

## Personalizar o Vespucci

### Configurações que poderá querer alterar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Preferências avançadas

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reportar Problemas

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


