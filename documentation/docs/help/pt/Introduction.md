_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Introdução ao Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Editar no Vespucci

Dependendo do tamanho do ecrã e da antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

<a id="download"></a>

### Descarregar dados do OpenStreetMap

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

A forma mais fácil de descarregar dados para o dispositivo é aproximar/afastar e deslocar para a localização que quer editar e então selecionar "Descarregar a vista atual". Pode aproximar/afastar utilizando gestos dos dedos, os botões de aproximar/afastar ou os botões de volume no dispositivo. O Vespucci irá então descarregar os dados para a vista atual. Não é necessário autenticar-se para descarregar dados.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Editar

<a id="lock"></a>

#### Bloquear, desbloquear, alternar modo

Para evitar edições acidentais, o Vespucci inicia no modo  "bloqueado". Toque no ícone ![Bloqueado](../images/locked.png) para desbloquear o ecrã. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

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

Depois de selecionar um objeto, este pode ser movido. Note que os objetos só podem ser arrastados/movidos quando estão selecionados. Basta arrastar perto (ou seja, dentro da zona de tolerância) do objeto selecionado para movê-lo. Se selecionar a área de arrastamento grande nas [preferências] (Preferences.md), obtém uma área grande à volta do nó selecionado que facilita o posicionamento do objeto. 

#### Adicionar um novo Nó ou Linha 

No primeiro arranque a app começa com o "Modo simples" ativado. Isto pode ser alterado no menu principal desativando essa opção.

##### Modo simples

Ao tocar no botão verde grande redondo no ecrã do mapa irá mostrar um menu. Após selecionar um dos itens do menu, será perguntando onde quer criar o objeto. O deslocamento e o aumento da visualização continuam a funcionar para ajustar a vista do mapa. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Modo avançado (pressão longa)
 
Prima longamente o local onde pretende que o nó fique ou a linha para começar. Aparecerá um símbolo de "mira" preta. 
* Se pretender criar um novo nó (não ligado a um objeto), toque num local longe dos objetos existentes.
* Se quiser continuar uma linha, toque dentro da "zona de tolerância" da linha (ou de um nó da linha). A zona de tolerância é indicada pelas áreas à volta de um nó ou linha.

Assim que veja um sinal mais, tem as seguintes opções:

* _Toque normal no mesmo local._
    * Se a mira não estiver perto de um nó, tocar novamente no mesmo local cria um novo nó. Se estiver perto de uma linha (mas não perto de um nó), o novo nó ficará na linha (e ligado à linha).
    * Se a mira estiver perto de um nó (ou seja, dentro da zona de tolerância do nó), tocar na mesma localização apenas seleciona o nó (e o editor de etiquetas abre-se). Não é criado um novo nó. A ação é a mesma que a da seleção acima.
* _Toque normal noutro local._ Tocar noutro local (fora da zona de tolerância da mira) adiciona um segmento de linha desde a posição original até à posição atual. Se a mira estava perto de uma linha ou nó, o novo segmento será ligado a esse nó ou linha.

Basta tocar no ecrã onde pretende adicionar mais nós da linha. Para terminar, toque duas vezes no nó final. Se o nó final estiver localizado numa linha ou num nó, o segmento será ligado automaticamente à linha ou ao nó. 

Também pode usar o item do menu: para mais informações veja [Criar novos objetos](Creating%20new%20objects.md).

#### Adicionar uma Área

O OpenStreetMap neste momento não tem um tipo de objeto definido como "área" ao contrário de outros sistemas de geo-dados. O editor "iD" tenta criar uma abstração de área a partir dos elementos OpenStreetMap subjacentes, o que funciona bem em algumas circunstâncias, mas não noutras. O Vespucci neste momento não faz isso, por isso necessita de saber um pouco como é que as áreas são representadas:

* _linhas fechadas (*polígonos")_: a variante de área mais simples e mais comum são as linhas que têm um primeiro e um último nó partilhados formando um "anel" fechado (por exemplo, a maioria dos edifícios são deste tipo). Estas são muito fáceis de criar no Vespucci, basta ligar ao primeiro nó quando terminar de desenhar a área. Nota: a interpretação da linha fechada depende das suas etiquetas: por exemplo, se uma linha fechada for etiquetada como um edifício, será considerada uma área, se for etiquetada como uma rotunda, não. Em algumas situações em que ambas as interpretações podem ser válidas, uma etiqueta de "área" pode clarificar a utilização pretendida.
* _multi-polígono_: algumas áreas têm várias partes, buracos e anéis que não podem ser representados numa só linha. O OSM utiliza um tipo específico de relação (o nosso objeto de uso geral que pode modelar relações entre elementos) para contornar esta situação, um multi-polígono. Um multi-polígono pode ter vários anéis "exteriores" e vários anéis "interiores". Cada anel pode ser uma linha fechada, como descrito acima, ou várias linhas individuais que têm nós finais comuns. Embora os multi-polígonos de grandes dimensões sejam difíceis de manusear com qualquer ferramenta, os pequenos não são difíceis de criar no Vespucci. 
* _linhas costeiras_: para objetos muito grandes, continentes e ilhas, mesmo o modelo multi-polígono não funciona de forma satisfatória. Para linhas natural=coastline assumimos uma semântica dependente da direção: a terra está do lado esquerdo da linha, a água do lado direito. Um efeito secundário disto é que, em geral, não se deve inverter a direção de uma linha com marcação de costa. Pode encontrar mais informações na [wiki do OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Melhorar geometria das linhas

Se aproximar a vista suficientemente numa linha selecionada, irá ver um pequeno "x" no meio dos segmentos da linha que sejam suficientemente compridos. Se arrastar esse "x" irá criar um nó nessa localização. Nota: para evitar que crie nós por acidente, a zona de tolerância de toque desta operação é pequena.

#### Cortar, copiar e colar

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Adicionar endereços eficientemente

O Vespucci suporta uma funcionalidade que torna o levantamento de endereços mais eficiente, prevendo números de casas (lados esquerdo e direito das ruas separadamente) e adicionando automaticamente etiquetas _addr:street_ ou _addr:place_ com base no último valor utilizado e nas proximidades. Na melhor das hipóteses, isto permite adicionar um endereço sem qualquer digitação.   

A adição das etiquetas pode ser ativada premindo ![Endereço](../images/address.png): 

* após uma pressão longa (apenas no modo não simples): o Vespucci adiciona um nó no local e faz uma estimativa do número da casa e adiciona etiquetas de endereço que utilizou recentemente. Se o nó estiver num contorno de um edifício, adicionará automaticamente uma etiqueta "entrance=yes" ao nó. O editor de etiquetas abre-se para o objeto em questão e permite-lhe fazer quaisquer outras alterações necessárias.
* nos modos selecionados de nó/via: o Vespucci adicionará etiquetas de endereço como acima e iniciará o editor de etiquetas.
* no editor de propriedades.

Para adicionar nós de endereço individuais diretamente no "Modo simples" por defeito, mude para o modo de edição "Endereço" (prima longamente o botão de bloqueio), "Adicionar nó de endereço" adicionará então um nó de endereço no local e, se estiver numa linha fechada de edifício, adicionará uma etiqueta de entrada como descrito acima.

A previsão do número de porta normalmente necessita de 2 números de porta em cada lado da estrada para funcionar. Quantos mais números de porta presentes, melhor funciona.

Considere a possibilidade de o utilizar com um dos modos de [descarregamento automático](#download).  

#### Adicionar restrições de viragem

O Vespucci tem uma forma rápida de introduzir restrições de viragem. Se for necessário irá dividir automaticamente e perguntar-lhe para tornar a selecionar os elementos. 

* selecione uma linha com a etiqueta highway (as restrições de viragem só podem ser adicionadas em linhas, se precisar de adicionar restrições a outros elementos, use o modo "criar relação")
* escolha "Adicionar restrição de viragem" no menu
* selecione o nó ou linha "via" (apenas os elementos possíveis de "via" serão destacados automaticamente)
* selecione a linha "to" (para). É possível indicar de novo o primeiro elemento "from" (de) da restrição, o Vespucci neste caso assumirá que não se pode fazer inversão de marcha na mesma via
* indique o tipo de restrição no menu de etiquetas

### Vespucci no modo "bloqueado"

Quando é mostrado o cadeado vermelho, estão disponíveis todas as ações de não edição. Um toque longo num objeto mostra a informação sobre o objeto, se este for um elemento do mapa.

### Guardar as alterações

*(requer ligação à Internet)*

Carregue no mesmo botão ou item do menu que fez para descarregar e então carregue em "Enviar dados para o OSM".

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Se quiser gravar as edições e não tiver acesso à Internet, pode gravar os dados num ficheiro .osm compatível com o JOSM e posteriormente abrir e enviar esse ficheiro através do Vespucci ou do JOSM. 

#### Resolver conflitos ao enviar

O Vespucci tem um solucionador de conflitos simples. No entanto se suspeita que existem grandes problemas com as suas edições, faça uma exportação das alterações que fez para um ficheiro .osc (o item "Exportar" no menu "Transferir") e corrija e envie as alterações para o OSM no JOSM. Veja a ajuda detalhada em [Resolução de conflitos](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Utilizar trilhos GPS e GPX

Com as definições padrão, o Vespucci tentará ativar o GPS (e outros sistemas de navegação por satélite) e, se tal não for possível, passará a determinar a posição através da chamada "localização de rede". Este comportamento pressupõe que, numa utilização normal, o seu dispositivo Android esteja configurado para utilizar apenas localizações geradas por GPX (para evitar o rastreamento), ou seja, que a opção eufemisticamente designada por "Melhorar a precisão da localização" esteja desativada. Se pretender ativar a opção, mas quiser evitar que o Vespucci volte a utilizar a "localização de rede", deve desativar a opção correspondente em [Preferências avançadas](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Para gravar um trajeto GPX e visualizá-lo no seu dispositivo, selecione o item "Iniciar trilho GPX" no menu ![GPS](../images/menu_gps.png). Pode carregar e exportar o percurso a partir da entrada no [controlo de camadas] (Main%20map%20display.md). Outras camadas podem ser adicionadas a partir de ficheiros GPX locais e de trilhos descarregados a partir da API OSM.

Nota: por predefinição, o Vespucci não regista dados de elevação com o seu trilho GPX, devido a alguns problemas específicos do Android. Para ativar o registo da elevação, instale um modelo gravitacional ou, mais simplesmente, vá às [Preferências avançadas] (Advanced%20preferences.md) e configure a entrada NMEA.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtros

Para além de ativar globalmente a visualização de notas e erros, pode definir um filtro de visualização de grão grosso para reduzir a desordem. A configuração do filtro pode ser acedida a partir da entrada da camada de tarefas no [controlo de camadas] (#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Modo interiores

Mapear interiores de edifícios pode ser desafiante devido ao elevado número de objetos que se sobrepõem. O Vespucci tem um Modo Interiores que permite filtrar todos os objetos que não estejam no mesmo andar e adiciona automaticamente o andar aos objetos criados.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

<a id="c-mode"></a>

## Modo-C

No Modo-C apenas são mostrados os objetos que têm uma bandeira de aviso definida, o que torna fácil detetar objetos que têm problemas específicos ou que correspondam a verificações configuráveis. Se for selecionado um objeto e o Editor de Propriedades iniciado no Modo-C, será aplicado automaticamente o modelo de melhor correspondência.

O modo pode ser ativado ao pressionar de forma longa o botão do cadeado, ver [Bloquear, desbloquear, alternar modo](#lock) e selecionar o respetivo menu.

### Configurar verificações

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Entradas de novos levantamentos

O levantamento possui as seguintes propriedades:

* **Chave** - Chave da etiqueta de interesse.
* **Valor** - Valor que a etiqueta de interesse deve ter; se estiver vazia, o valor da etiqueta será ignorado.
* **Idade** - o número de dias após a última alteração do elemento que este deve voltar a ser controlado; se estiver presente uma etiqueta _check_date_, será essa a utilizada; caso contrário, será a data em que a versão atual foi criada. Se o valor for definido como zero, a verificação será efetuada simplesmente em função da chave e do valor.
* **Expressão regular** - se ativado, presume-se que **valor** é uma expressão regular JAVA.

**Chave** e **Valor** são verificados de acordo com as etiquetas _existentes_ do objeto em questão.

O grupo _Anotações_ nos modelos de etiquetas padrão contém um item que adiciona automaticamente uma etiqueta _check_date_ com a data atual.

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

Muitos aspetos da aplicação podem ser personalizados. Se estiver à procura de algo específico e não o conseguir encontrar, [o site do Vespucci] (https://vespucci.io/) pode ser pesquisado e contém informações adicionais sobre o que está disponível no dispositivo.

<a id="layers"></a>

### Definições de camadas

As definições das camadas podem ser alteradas através do controlo de camadas (menu "hambúrguer" no canto superior direito), todas as outras definições são acessíveis através do botão de preferências do menu principal. As camadas podem ser ativadas, desativadas e temporariamente ocultas.

Tipos de camadas disponíveis:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Para mais informações, consultar a secção [visualização do mapa](Main%20map%20display.md).

#### Preferências

* Manter ecrã ligado. Predefinição: desativado.
* Área grande de arrasto. Mover nós num dispositivo com entrada tátil é problemático, uma vez que os seus dedos irão obscurecer a posição atual no ecrã. Se ativar esta opção, terá uma grande área que pode ser utilizada para arrastar nós desativados (a seleção e outras operações continuam a utilizar a área normal de tolerância ao toque). Predefinição: desativado.

A descrição completa pode ser encontrada em [Preferências].(Preferences.md)

#### Preferências avançadas

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

A descrição completa pode ser encontrada em [Preferências avançadas](Advanced%20preferences.md)

## Reporting and Resolving Issues

Se o Vespucci bloquear ou for abaixo. Ser-lhe-á perguntado se pretende enviar o relatório de erro com informações sobre este aos programadores do Vespucci. Os programadores agradecem que enviem esta informação, mas por favor envie um relatório de erro apenas para um mesmo erro/situação. Se quiser adicionar outras informações que possam ser úteis aos programadores ou requisitar outras funcionalidades novas no Vespucci use o [Repositório de erros do Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). A função "Fornecer opinião" no menu principal irá abrir um novo relatório e incluir a informação relevante da app e do dispositivo sem ser necessário escrever essa informação.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


