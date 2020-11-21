# Introdução ao Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size. 

## Primeira Utilização

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Como alternativa, pode fechar a janela tocando no botão "Ir para o mapa", deslocar e aproximar numa localização que quer editar e descarregar os dados (ver abaixo"Editar no Vespucci").

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

* **Normal** - no modo de edição padrão podem ser adicionados novos elementos e os existentes podem ser editados, movidos ou removidos, O símbolo do cadeado aparece a branco.
* **Apenas etiquetas** - selecionando um elemento surgirá o Editor de Propriedades, pressionando de forma contínua no ecrã principal irá adicionar novos elementos, mas não funcionarão outras operações de geometrias. O símbolo do cadeado fica em branco com um "T".
* **Interior** - ativa o modo Interior, veja [Modo interior](#indoor). O símbolo do cadeado fica em branco com um "i".
* **Modo-C** - Ativa o modo Modo-C. Apenas serão mostrados os elementos que estejam marcados com avisos. Veja [Modo-C](#c-mode). O símbolo do cadeado fica em branco com um "C".

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

Após selecionar um objeto, este pode ser movido. Note que os objetos podem ser arrastados/movidos apenas quando estão selecionados. Simplesmente arraste perto de um objeto selecionado (dentro da zona de tolerância) para o mover. Se tiver definido nas preferências uma área grande de arrasto, obtém uma zona larga à volta do nó selecionado que torna mais fácil posicionar o objeto. 

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

* _linhas fechadas (*polígonos")_: a variante mais simples e mais comum, são linhas que têm um nó que definem o início e fim da linha formando um "anel" fechado (por exemplo, a maioria dos edifícios usam este tipo). Estas são muito fáceis de criar no Vespucci, simplesmente termine a linha clicando no nó inicial. Nota: a interpretação da linha fechada depende das etiquetas desta: por exemplo, se uma linha fechada tiver a etiqueta de edifício será considerada uma área, se tiver a etiqueta de rotunda não será considerada uma área. Em algumas situações ambas as interpretações podem ser válidas, por isso pode ser adicionada uma etiqueta "area" para clarificar.
* _multi-polígonos_: algumas áreas têm várias partes, buracos e anéis que não possam ser representados apenas com uma linha. O OpenStreetMap usa um tipo específico de relação (o objeto de uso genérico que pode modelar relações entre elementos) para contornar o problema, um multi-polígono. Um multi-polígono pode ter vários anéis "exteriores" e "interiores". Cada um dos anéis podem ser uma linha fechada como descrito acima ou várias linhas individuais que tenham nós em comum. Enquanto que multi-polígonos grandes podem ser difíceis de lidar, os pequenos não são complicados de criar no Vespucci. 
* _linhas costeiras_: para grandes objetos, continente e ilhas, mesmo o modelo de multi-polígonos não funciona de uma forma satisfatória. Para linhas com a etiqueta natural=coastline (natural=linha costeira) assume-se que a direção depende da semântica: a terra encontra-se do lado esquerdo da direção da linha, a água do lado direito. Por isso regra geral não se deve trocar a direção das linhas costeiras. Para mais informações ver a [Wiki do OpenStreetMap](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Melhorar Geometria das Linhas

Se aproximar a vista suficientemente numa linha selecionada, irá ver um pequeno "x" no meio dos segmentos da linha que sejam suficientemente compridos. Se arrastar esse "x" irá criar um nó nessa localização. Nota: para evitar que crie nós por acidente, a zona de tolerância de toque desta operação é pequena.

#### Cortar, Copiar e Colar

Pode copiar ou cortar os nós ou linhas selecionados, e então colar uma ou várias vezes numa nova localização. Cortar irá preservar o identificador (ID), versão e histórico do objeto. Para colar basta um toque longo no local desejado (verá um x marcando o lugar). E então selecione "Colar" no menu.

#### Adicionar Endereços Eficientemente

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

A previsão do número de porta normalmente necessita de 2 números de porta em cada lado da estrada para funcionar. Quantos mais números de porta presentes, melhor funciona.

Considere usar isto com o modo [Descarregar automático](#download).  

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

## Utilizar o GPS

Pode usar o Vespucci para criar trilhos GPS e mostrá-los no seu dispositivo. Pode também saber a localização GPS atual (ativar em "Ver localização" no menu GPS) e/ou ter no centro do ecrã a localização e seguir a localização (ativar em "Seguir posição GPS" no menu GPS). 

Se tiver ativado o Seguir posição GPS, se deslocar o ecrã manualmente ou editar alguma coisa, irá desativar o "Seguir posição GPS" e a seta azul do GPS irá mudar de uma seta com linha de contorno para uma seta preenchida. Para regressar rapidamente para o modo de "seguir", toque no botão GPS ou ative de novo a opção no menu.

## Notas e Erros Reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

As Notas e Erros Reportados são mostrados no mapa com um pequeno ícone ![Erro](../images/bug_open.png), os verdes são erros fechados/resolvidos, os azuis são os criados ou editados por si e os amarelos são aqueles que ainda estão abertos/por resolver. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

### Filtros

Para além de ativar globalmente a visualização de notas e erros também pode pode aplicar um filtro para reduzir a confusão. Em [Preferências avançadas](Advanced%20preferences.md) pode selecionar individualmente:

* Erros reportados
* Erro Osmose
* Aviso Osmose
* Pequeno problema Osmose
* Personalizado

<a id="indoor"></a>

## Modo interiores

Mapear interiores de edifícios pode ser desafiante devido ao elevado número de objetos que se sobrepõem. O Vespucci tem um Modo Interiores que permite filtrar todos os objetos que não estejam no mesmo andar e adiciona automaticamente o andar aos objetos criados.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

<a id="c-mode"></a>

## Modo-C

No Modo-C apenas são mostrados os objetos que têm uma bandeira de aviso definida, o que torna fácil detetar objetos que têm problemas específicos ou que correspondam a verificações configuráveis. Se for selecionado um objeto e o Editor de Propriedades iniciado no Modo-C, será aplicado automaticamente o modelo de melhor correspondência.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

### Configurar verificações

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

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

* **Chave** - Chave que deve estar presente no objeto de acordo com o modelo de etiquetas correspondente.
* **Exigir opcional** - Exigir a chave mesmo que a chave esteja nas etiquetas opcionais do modelo de etiquetas correspondente.

Esta verificação funciona determinando primeiro o modelo correspondente e então verifica se a **Chave** é uma chave "recomendada" para esse objeto de acordo com o modelo, **Exigir opcional** irá alargar a verificação a etiquetas que sejam "opcionais * no objeto. Nota: neste momento os modelos ligados não são verificados.

## Filtros

### Filtro baseado em etiquetas

O filtro pode ser ativado no menu principal, pode ser então alterado tocando no ícone de filtro. Para mais informações ver [Filtro de Etiquetas](Tag%20filter.md).

### Filtro baseado em modelos de etiquetas

Uma alternativa ao descrito anteriormente, os objetos podem ser filtrados com base em modelos de etiquetas individuais ou grupos destas. Tocando no ícone de filtro irá mostrar uma lista de seleção de modelos. Os modelos individuais podem ser selecionados apenas com um toque, e os grupos de modelos por um toque longo (toque normal abre o grupo). Para mais informações ver [Filtro de modelos de etiquetas](Preset%20filter.md).

## Personalizar o Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

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


