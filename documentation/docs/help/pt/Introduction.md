_Antes de começar: a maioria dos ecrãs tem hiperligações no menu para o sistema de ajuda do dispositivo que lhe dá acesso direto a informações relevantes para o contexto atual, podendo também navegar facilmente para este texto. Se tiver um dispositivo maior, por exemplo um tablet, pode abrir o sistema de ajuda numa janela separada. Todos os textos de ajuda e outros (FAQs, tutoriais) também podem ser encontrados no [site de documentação Vespucci](https://vespucci.io/) too._

# Introdução ao Vespucci

O Vespucci é um editor OpenStreetMap completo que suporta a maioria das operações que os editores de secretária fornecem. Foi testado com sucesso no Android 2.3 a 10.0 da Google e em várias variantes baseadas em AOSP. Uma palavra de cautela: embora as capacidades dos dispositivos móveis se tenham aproximado dos seus rivais de secretária, os dispositivos mais antigos têm uma memória disponível muito limitada e tendem a ser bastante lentos. Deve ter isto em conta quando utilizar o Vespucci e manter, por exemplo, as áreas que está a editar com um tamanho razoável.

## Editar no Vespucci

Dependendo do tamanho do ecrã e da antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

<a id="download"></a>

### Descarregar dados do OpenStreetMap

Selecione o ícone de transferir  ![Transferir](../images/menu_transfer.png) ou o menu "Transferir". Isto irá mostrar 7 opções:

* **Descarregar a vista atual** - descarrega a área visível no ecrã e funde-a com os dados existentes *(requer conetividade de rede ou fonte de dados offline)*
* **Limpar e descarregar a vista atual** - limpar quaisquer dados na memória e depois descarregar a área visível no ecrã *(requer ligação à rede)*
* **Enviar dados para o servidor OSM…** - carregar edições para o OpenStreetMap *(requer autenticação)* *(requer ligação à rede)*
* **Atualizar dados** - voltar a descarregar dados para todas as áreas e atualizar o que está na memória *(requer ligação à rede)*
* **Descarregamento automático com base na localização** - descarrega automaticamente uma área em torno da localização geográfica atual *(requer conetividade de rede ou dados offline)* *(requer GPS)*
* **Descarregamento automático de deslocar a vista e o zoom** - descarrega automaticamente os dados da área do mapa atualmente apresentada *(requer conetividade de rede ou dados offline)* *(requer GPS)*
* **Ficheiro…** - guardar e carregar dados OSM de/para ficheiros no dispositivo.
* **Notas/erros…** - descarrega (automática e manualmente) notas e "Bugs" do OSM a partir de ferramentas QA (atualmente OSMOSE) *(requer ligação à rede)*

A forma mais fácil de descarregar dados para o dispositivo é aproximar/afastar e deslocar para a localização que quer editar e então selecionar "Descarregar a vista atual". Pode aproximar/afastar utilizando gestos dos dedos, os botões de aproximar/afastar ou os botões de volume no dispositivo. O Vespucci irá então descarregar os dados para a vista atual. Não é necessário autenticar-se para descarregar dados.

Com as configurações predefinidas, as áreas não descarregadas serão escurecidas em relação às descarregadas, para evitar a adição inadvertida de objetos duplicados em áreas que não estão a ser apresentadas. O comportamento pode ser alterado em [Preferências avançadas](Advanced%20preferences.md).

### Editar

<a id="lock"></a>

#### Bloquear, desbloquear, alternar modo

Para evitar edições acidentais, o Vespucci inicia no modo  "bloqueado". Toque no ícone ![Bloqueado](../images/locked.png) para desbloquear o ecrã. 

Se pressionar de forma longa o botão do cadeado, surgirá um menu com 4 opções:

* **Normal** - o modo de edição predefinido, podem ser adicionados novos objetos, editados os existentes, movidos e removidos. É apresentado um ícone de cadeado branco simples.
* **Apenas etiquetas** - a seleção de um objeto existente inicia o editor de propriedades, uma pressão prolongada no ecrã principal adiciona objetos, mas nenhuma outra operação de geometria funciona. Aparece o ícone de cadeado branco com um "T".
* **Endereço** - ativa o modo endereço, um modo ligeiramente simplificado com ações específicas disponíveis no botão "+" do [Modo simples](../en/Simple%20actions.md). Aparece o ícone de cadeado branco com um "A".
* **Interior** - ativa o modo Interior, ver [Modo interior](#indoor). Aparece o ícone de cadeado branco com um "I".
* **Modo C** - ativa o modo C, apenas serão apresentados os objetos que tenham um sinal de aviso definido, ver [Modo C](#c-mode). Aparece o ícone de cadeado branco com um "C".

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

Para mais informações ver [Criar novos objetos no modo de ações simples (Creating%20new%20objects%20in%20simple%20actions%20mode.md).

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

Pode copiar ou cortar os nós ou linhas selecionados, e então colar uma ou várias vezes numa nova localização. Cortar irá preservar o identificador (ID), versão e histórico do objeto. Para colar basta um toque longo no local desejado (verá um x marcando o lugar). E então selecione "Colar" no menu.

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

O Vespucci suporta a autorização OAuth assim como o método clássico de nome de utilizador e palavra-chave. É recomendável usar o OAuth porque este envia a palavra-chave para o OpenStreetMap encriptada, sendo mais seguro.

As instalações novas do Vespucci têm o OAuth ativado por defeito. Ao tentar enviar pela primeira vez dados para o OpenStreetMap, é aberta a página web do OpenStreetMap. Após entrar na sua conta do OpenStreetMap (numa ligação encriptada), será perguntado se quer autorizar o Vespucci a editar usando os dados da sua conta. Se quiser ou necessitar de autorizar o acesso OAuth para a sua conta antes de editar, existe essa opção no menu "Ferramentas".

Se quiser gravar as edições e não tiver acesso à Internet, pode gravar os dados num ficheiro .osm compatível com o JOSM e posteriormente abrir e enviar esse ficheiro através do Vespucci ou do JOSM. 

#### Resolver conflitos ao enviar

O Vespucci tem um solucionador de conflitos simples. No entanto se suspeita que existem grandes problemas com as suas edições, faça uma exportação das alterações que fez para um ficheiro .osc (o item "Exportar" no menu "Transferir") e corrija e envie as alterações para o OSM no JOSM. Veja a ajuda detalhada em [Resolução de conflitos](Conflict%20resolution.md).  

## Utilizar trilhos GPS e GPX

Com as definições padrão, o Vespucci tentará ativar o GPS (e outros sistemas de navegação por satélite) e, se tal não for possível, passará a determinar a posição através da chamada "localização de rede". Este comportamento pressupõe que, numa utilização normal, o seu dispositivo Android esteja configurado para utilizar apenas localizações geradas por GPX (para evitar o rastreamento), ou seja, que a opção eufemisticamente designada por "Melhorar a precisão da localização" esteja desativada. Se pretender ativar a opção, mas quiser evitar que o Vespucci volte a utilizar a "localização de rede", deve desativar a opção correspondente em [Preferências avançadas](Advanced%20preferences.md). 

Tocar no botão ![GPS](../images/menu_gps.png) (no lado esquerdo do ecrã do mapa) centrará o ecrã na posição atual e, à medida que se desloca, o ecrã do mapa será preenchido para manter esta posição. Mover o ecrã manualmente ou editar fará com que o modo "seguir GPS" seja desativado e a seta azul do GPS mudará de um contorno para uma seta preenchida. Para voltar rapidamente ao modo "seguir", basta tocar no botão GPS ou voltar a selecionar a opção de menu equivalente. Se o dispositivo não tiver uma localização atual, o marcador/seta de localização será apresentado a preto; se estiver disponível uma localização atual, o marcador será azul.

Para gravar um trajeto GPX e visualizá-lo no seu dispositivo, selecione o item "Iniciar trilho GPX" no menu ![GPS](../images/menu_gps.png). Pode carregar e exportar o percurso a partir da entrada no [controlo de camadas] (Main%20map%20display.md). Outras camadas podem ser adicionadas a partir de ficheiros GPX locais e de trilhos descarregados a partir da API OSM.

Nota: por predefinição, o Vespucci não regista dados de elevação com o seu trilho GPX, devido a alguns problemas específicos do Android. Para ativar o registo da elevação, instale um modelo gravitacional ou, mais simplesmente, vá às [Preferências avançadas] (Advanced%20preferences.md) e configure a entrada NMEA.

## Notas e erros reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

As Notas e Erros Reportados são mostrados no mapa com um pequeno ícone ![Erro](../images/bug_open.png), os verdes são erros fechados/resolvidos, os azuis são os criados ou editados por si e os amarelos são aqueles que ainda estão abertos/por resolver. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

### Filtros

Para além de ativar globalmente a visualização de notas e erros, pode definir um filtro de visualização de grão grosso para reduzir a desordem. A configuração do filtro pode ser acedida a partir da entrada da camada de tarefas no [controlo de camadas] (#layers):

* Notas
* Erro Osmose
* Aviso Osmose
* Problema menor Osmose
* Maproulette
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

Atualmente, existem duas verificações configuráveis (existe uma verificação para etiquetas FIXME e um teste para etiquetas de tipo em falta nas relações que não são atualmente configuráveis), ambas podem ser configuradas selecionando "Configurações do verificador" nas [preferências](Preferences.md). 

A lista de entradas está dividida em duas, a primeira metade mostra entradas de "novos levantamentos" e a segunda metade "entradas" de verificação. As entradas podem ser editadas clicando nelas. O botão do menu verde permite adicionar entradas.

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

* Camada de dados - esta é a camada em que os dados do OpenStreetMap são carregados. Numa utilização normal, não é necessário alterar nada aqui. Predefinição: ativado.
* Camada de fundo - existe uma grande variedade de imagens de fundo aéreas e de satélite disponíveis. O valor predefinido para esta camada é o mapa de "estilo padrão" do openstreetmap.org.
* Camada de sobreposição - são camadas semi-transparentes com informações adicionais, por exemplo, trilhos GPX. A adição de uma sobreposição pode causar problemas em dispositivos mais antigos e com memória limitada. Predefinição: nenhuma.
* Notas/erros - As notas e os erros abertos serão apresentados como um ícone de inseto amarelo, os fechados a verde. Predefinição: ativado.
* Camada de fotografias - Apresenta fotografias georreferenciadas como ícones de câmaras vermelhas; se estiver disponível informação sobre a direção, o ícone será rodado. Predefinição: desativado.
* Camada Mapillary - Apresenta segmentos cartográficos com marcadores onde existem imagens; ao ativar um marcador, a imagem é apresentada. Predefinição: desativado.
* Camada GeoJSON - Apresenta o conteúdo de um ficheiro GeoJSON. Predefinição: desativado.
* Grelha - Apresenta uma escala ao longo dos lados do mapa ou uma grelha. Predefinição: ativado. 

Para mais informações, consultar a secção [visualização do mapa](Main%20map%20display.md).

#### Preferências

* Manter ecrã ligado. Predefinição: desativado.
* Área grande de arrasto. Mover nós num dispositivo com entrada tátil é problemático, uma vez que os seus dedos irão obscurecer a posição atual no ecrã. Se ativar esta opção, terá uma grande área que pode ser utilizada para arrastar nós desativados (a seleção e outras operações continuam a utilizar a área normal de tolerância ao toque). Predefinição: desativado.

A descrição completa pode ser encontrada em [Preferências].(Preferences.md)

#### Preferências avançadas

* Ícones de nós. Predefinição: ativado.
* Mostrar sempre o menu de contexto. Quando ativado, cada processo de seleção mostrará o menu de contexto; desativado, o menu é mostrado apenas quando não é possível determinar uma seleção inequívoca. Predefinição: desativado (costumava ser ativado).
* Ativar o tema claro. Nos dispositivos modernos, este tema está ativado por predefinição. Embora seja possível ativá-lo em versões mais antigas do Android, é provável que o estilo seja inconsistente. 

A descrição completa pode ser encontrada em [Preferências avançadas](Advanced%20preferences.md)

## Reportar problemas

Se o Vespucci bloquear ou for abaixo. Ser-lhe-á perguntado se pretende enviar o relatório de erro com informações sobre este aos programadores do Vespucci. Os programadores agradecem que enviem esta informação, mas por favor envie um relatório de erro apenas para um mesmo erro/situação. Se quiser adicionar outras informações que possam ser úteis aos programadores ou requisitar outras funcionalidades novas no Vespucci use o [Repositório de erros do Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). A função "Fornecer opinião" no menu principal irá abrir um novo relatório e incluir a informação relevante da app e do dispositivo sem ser necessário escrever essa informação.

Se quiser discutir algo relacionado com o Vespucci, pode iniciar uma discussão no [grupo do Google Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) ou o [fórum Android do OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


