_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too._

# Introdução ao Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Editar no Vespucci

Dependendo do tamanho do ecrã e da antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

<a id="download"></a>

### Descarregar Dados do OpenStreetMap

Selecione o ícone de transferir  ![Transferir](../images/menu_transfer.png) ou o menu "Transferir". Isto irá mostrar 7 opções:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

A forma mais fácil de descarregar dados para o dispositivo é aproximar/afastar e deslocar para a localização que quer editar e então selecionar "Descarregar a vista atual". Pode aproximar/afastar utilizando gestos dos dedos, os botões de aproximar/afastar ou os botões de volume no dispositivo. O Vespucci irá então descarregar os dados para a vista atual. Não é necessário autenticar-se para descarregar dados.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Editar

<a id="lock"></a>

#### Bloquear, desbloquear, alternar modo

Para evitar edições acidentais, o Vespucci inicia no modo  "bloqueado". Toque no ícone ![Bloqueado](../images/locked.png) para desbloquear o ecrã. 

Se pressionar de forma longa o botão do cadeado, surgirá um menu com 4 opções:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Toque simples, toque duplo e toque longo

Por defeito, os nós e linhas selecionáveis têm uma área laranja à volta deles indicando onde deve tocar para selecionar um objeto. Tem 3 opções:

* Toque simples: seleciona o objeto. 
    * Um nó ou linha isolados é destacado imediatamente. 
    * No entanto se tentar selecionar um objeto e o Vespucci determinar  que a seleção pode ser vários objetos, rá mostrar um menu de seleção permitindo escolher o objeto que quer selecionar. 
    * Os objetos selecionados são destacados a amarelo. 
    * Para mais informações ver [Nó selecionado](Node%20selected.md), [Linha selecionada](Way%20selected.md) and [Relação selecionada](Relation%20selected.md).
* Toque duplo: ativar [Modo Multi-seleção](Multiselect.md)
* Toque longo: cria um "sinal mais", permitindo adicionar erros reportados. Para mais informações ver [Criar novos objetos(Creating%20new%20objects.md). Isto apenas é ativado se o  "Modo simples" estiver desativado.

É uma boa ideia aproximar a visualização caso a área tenha muitos objetos.

O Vespucci tem um bom sistema de "desfazer/refazer" por isso não tenha medo de fazer experiências de edição, mas por favor não envie essas experiências para o OpenStreetMap.

#### Selecionar / Desselecionar (toque simples e "menu de seleção")

Toque num objeto para o selecionar e destacá-lo. Se tocar numa área vazia irá desselecionar. Se selecionou um objeto e necessita de selecionar outra coisa, simplesmente toque nesse objeto, não necessita de desselecionar o primeiro. Se tocar 2 vezes num objeto irá iniciar o [modo Multi-seleção](Multiselect.md).

Note que se tentar selecionar um objeto e se o Vespucci determinar que a seleção pode referir-se a vários objetos (como um nó numa linha, ou outros objetos sobrepostos) irá ver um menu de seleção: toque no objeto que quer selecionar. 

Os objetos selecionados são indicados com uma borda amarela fina. A borda amarela pode ser difícil de ver, dependendo do fundo do mapa e do fator de ampliação. Após ser feita uma seleção, irá ver uma mensagem a confirmar a seleção.

Assim que a seleção estiver completa irá ver (como botões ou como itens 
no menu) uma lista de operações suportadas para o objeto selecionado. 
Para mais informações veja [Nó selecionado](Node%20selected.md), [Via selecionada](Way%20selected.md) e [Relação selecionada](Relation%20selected.md).

#### Objetos selecionados: Editar etiquetas

Um segundo toque no objeto selecionado abre o editor de etiquetas e poderá editar as etiquetas associadas ao objeto.

Note que nos objetos que se sobrepõem (como um nó numa linha) irá aparecer de novo o menu. Selecionando de novo o mesmo objeto irá mostrar o editor de etiquetas; selecionando outro objeto simplesmente seleciona o outro objeto.

#### Objetos selecionados: Mover um Nó ou Linha

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Adicionar um novo Nó ou Linha 

No primeiro arranque a app começa com o "Modo simples" ativado. Isto pode ser alterado no menu principal desativando essa opção.

##### Modo simples

Ao tocar no botão verde grande redondo no ecrã do mapa irá mostrar um menu. Após selecionar um dos itens do menu, será perguntando onde quer criar o objeto. O deslocamento e o aumento da visualização continuam a funcionar para ajustar a vista do mapa. 

Para mais informações ver [Criar novos objetos no modo de ações simples (Creating%20new%20objects%20in%20simple%20actions%20mode.md).

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Assim que veja um sinal mais, tem as seguintes opções:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

Também pode usar o item do menu: para mais informações veja [Criar novos objetos](Creating%20new%20objects.md).

#### Adicionar uma Área

O OpenStreetMap neste momento não tem um tipo de objeto definido como "área" ao contrário de outros sistemas de geo-dados. O editor "iD" tenta criar uma abstração de área a partir dos elementos OpenStreetMap subjacentes, o que funciona bem em algumas circunstâncias, mas não noutras. O Vespucci neste momento não faz isso, por isso necessita de saber um pouco como é que as áreas são representadas:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Melhorar Geometria das Linhas

Se aproximar a vista suficientemente numa linha selecionada, irá ver um pequeno "x" no meio dos segmentos da linha que sejam suficientemente compridos. Se arrastar esse "x" irá criar um nó nessa localização. Nota: para evitar que crie nós por acidente, a zona de tolerância de toque desta operação é pequena.

#### Cortar, Copiar e Colar

Pode copiar ou cortar os nós ou linhas selecionados, e então colar uma ou várias vezes numa nova localização. Cortar irá preservar o identificador (ID), versão e histórico do objeto. Para colar basta um toque longo no local desejado (verá um x marcando o lugar). E então selecione "Colar" no menu.

#### Adicionar Endereços Eficientemente

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

A previsão do número de porta normalmente necessita de 2 números de porta em cada lado da estrada para funcionar. Quantos mais números de porta presentes, melhor funciona.

Consider using this with one of the [Auto-download](#download) modes.  

#### Adicionar Restrições de Viragem

O Vespucci tem uma forma rápida de introduzir restrições de viragem. Se for necessário irá dividir automaticamente e perguntar-lhe para tornar a selecionar os elementos. 

* selecione uma linha com a etiqueta highway (as restrições de viragem só podem ser adicionadas em linhas, se precisar de adicionar restrições a outros elementos, use o modo "criar relação")
* escolha "Adicionar restrição de viragem" no menu
* selecione o nó ou linha "via" (apenas os elementos possíveis de "via" serão destacados automaticamente)
* selecione a linha "to" (para). É possível indicar de novo o primeiro elemento "from" (de) da restrição, o Vespucci neste caso assumirá que não se pode fazer inversão de marcha na mesma via
* indique o tipo de restrição no menu de etiquetas

### Vespucci no modo "bloqueado"

Quando é mostrado o cadeado vermelho, estão disponíveis todas as ações de não edição. Um toque longo num objeto mostra a informação sobre o objeto, se este for um elemento do mapa.

### Gravar as Alterações

*(requer ligação à Internet)*

Carregue no mesmo botão ou item do menu que fez para descarregar e então carregue em "Enviar dados para o OSM".

O Vespucci suporta a autorização OAuth assim como o método clássico de nome de utilizador e palavra-chave. É recomendável usar o OAuth porque este envia a palavra-chave para o OpenStreetMap encriptada, sendo mais seguro.

As instalações novas do Vespucci têm o OAuth ativado por defeito. Ao tentar enviar pela primeira vez dados para o OpenStreetMap, é aberta a página web do OpenStreetMap. Após entrar na sua conta do OpenStreetMap (numa ligação encriptada), será perguntado se quer autorizar o Vespucci a editar usando os dados da sua conta. Se quiser ou necessitar de autorizar o acesso OAuth para a sua conta antes de editar, existe essa opção no menu "Ferramentas".

Se quiser gravar as edições e não tiver acesso à Internet, pode gravar os dados num ficheiro .osm compatível com o JOSM e posteriormente abrir e enviar esse ficheiro através do Vespucci ou do JOSM. 

#### Resolver Conflitos ao Enviar

O Vespucci tem um solucionador de conflitos simples. No entanto se suspeita que existem grandes problemas com as suas edições, faça uma exportação das alterações que fez para um ficheiro .osc (o item "Exportar" no menu "Transferir") e corrija e envie as alterações para o OSM no JOSM. Veja a ajuda detalhada em [Resolução de conflitos](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notas e Erros Reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

As Notas e Erros Reportados são mostrados no mapa com um pequeno ícone ![Erro](../images/bug_open.png), os verdes são erros fechados/resolvidos, os azuis são os criados ou editados por si e os amarelos são aqueles que ainda estão abertos/por resolver. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

### Filtros

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Custom

<a id="indoor"></a>

## Modo interiores

Mapear interiores de edifícios pode ser desafiante devido ao elevado número de objetos que se sobrepõem. O Vespucci tem um Modo Interiores que permite filtrar todos os objetos que não estejam no mesmo andar e adiciona automaticamente o andar aos objetos criados.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

<a id="c-mode"></a>

## Modo-C

No Modo-C apenas são mostrados os objetos que têm uma bandeira de aviso definida, o que torna fácil detetar objetos que têm problemas específicos ou que correspondam a verificações configuráveis. Se for selecionado um objeto e o Editor de Propriedades iniciado no Modo-C, será aplicado automaticamente o modelo de melhor correspondência.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

### Configurar verificações

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the [preferences](Preferences.md). 

A lista de entradas está dividida em duas, a primeira metade mostra entradas de "novos levantamentos" e a segunda metade "entradas" de verificação. As entradas podem ser editadas clicando nelas. O botão do menu verde permite adicionar entradas.

#### Entradas de novos levantamentos

O levantamento possui as seguintes propriedades:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Chave** e **Valor** são verificados de acordo com as etiquetas _existentes_ do objeto em questão.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Verificar entradas

A verificação de entradas tem as seguintes duas propriedades:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

Esta verificação funciona determinando primeiro o modelo correspondente e então verifica se a **Chave** é uma chave "recomendada" para esse objeto de acordo com o modelo, **Exigir opcional** irá alargar a verificação a etiquetas que sejam "opcionais * no objeto. Nota: neste momento os modelos ligados não são verificados.

## Filtros

### Filtro baseado em etiquetas

O filtro pode ser ativado no menu principal, pode ser então alterado tocando no ícone de filtro. Para mais informações ver [Filtro de Etiquetas](Tag%20filter.md).

### Filtro baseado em modelos de etiquetas

Uma alternativa ao descrito anteriormente, os objetos podem ser filtrados com base em modelos de etiquetas individuais ou grupos destas. Tocando no ícone de filtro irá mostrar uma lista de seleção de modelos. Os modelos individuais podem ser selecionados apenas com um toque, e os grupos de modelos por um toque longo (toque normal abre o grupo). Para mais informações ver [Filtro de modelos de etiquetas](Preset%20filter.md).

## Personalizar o Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

<a id="layers"></a>

### Layer settings

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

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Preferências avançadas

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Reportar Problemas

Se o Vespucci bloquear ou for abaixo. Ser-lhe-á perguntado se pretende enviar o relatório de erro com informações sobre este aos programadores do Vespucci. Os programadores agradecem que enviem esta informação, mas por favor envie um relatório de erro apenas para um mesmo erro/situação. Se quiser adicionar outras informações que possam ser úteis aos programadores ou requisitar outras funcionalidades novas no Vespucci use o [Repositório de erros do Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). A função "Fornecer opinião" no menu principal irá abrir um novo relatório e incluir a informação relevante da app e do dispositivo sem ser necessário escrever essa informação.

Se quiser discutir algo relacionado com o Vespucci, pode iniciar uma discussão no [grupo do Google Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) ou o [fórum Android do OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


