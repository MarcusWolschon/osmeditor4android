# Introdução ao Vespucci

O Vespucci é um editor dedicado ao OpenStreetMap que suporta a maioria das operações que os editores de computadores proporcionam. Foi testado com sucesso nas versões Android 2.3 até à 7.0 e várias outras variantes do Android Open Source Project. A ter em conta: apesar das capacidades dos dispositivos móveis se terem aproximado aos computadores, os dispositivos mais antigos possuem uma capacidade de memória limitada e tendem a ser lentos. Deve ter isto em conta quando está a utilizar o Vespucci e deve ter em atenção a dimensão da área que está a editar, para que seja uma área com uma dimensão razoável e não demasiado grande. 

## Primeira Utilização

Ao iniciar o Vespucci, é mostrada a janela "Descarregar outra localização"/"Carregar Área". Se tiver as coordenadas a serem mostradas e quiser descarregar de imediato, pode selecionar a opção apropriada e selecionar o raio à volta da localização que quer descarregar. Não é recomendável descarregar áreas grandes em dispositivos lentos/antigos. 

Como alternativa, pode fechar a janela tocando no botão "Ir para o mapa", deslocar e aproximar numa localização que quer editar e descarregar os dados (ver abaixo"Editar no Vespucci").

## Editar no Vespucci

Dependendo do tamanho do ecrã e da antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

<a id="download"></a>

### Descarregar Dados do OpenStreetMap

Selecione o ícone de transferir  ![Transferir](../images/menu_transfer.png) ou o menu "Transferir". Isto irá mostrar 7 opções:

* **Descarregar vista atual** - descarrega a área visível no ecrã e substitui dados existentes, se for o caso *(necessita de ligação à Internet)*
* **Descarregar e adicionar vista atual** - descarrega a área visível no ecrã e faz a fusão com os dados existentes *(necessita de ligação à Internet)*
* **Descarregar outra localização** - mostra um formulário que permite introduzir coordenadas, procurar por uma localização ou usar a posição atual e descarregar a área à volta dessa localização *(necessita de ligação à Internet)*
* **Enviar dados para o OSM** - envia as alterações para o OpenStreetMap *(necessita de autenticação de conta de utilizador)* *(necessita de ligação à Internet)*
* **Descarregar automático** - descarrega automaticamente a área à volta da localização atual *(necessita de ligação à Internet)* *(necessita de GPS)*
* **Ficheiro...** - grava e abre ficheiros de dados do OpenStreetMap no dispositivo.
* **Erros Reportados...** - descarrega (automática e manualmente) "Erros Reportados" no OpenStreetMap das ferramentas de manutenção de qualidade de dados (atualmente o OSMOSE) *(necessita de ligação à Internet)*

A forma mais fácil de descarregar dados para o dispositivo é aproximar/afastar e deslocar para a localização que quer editar e então selecionar "Descarregar a vista atual". Pode aproximar/afastar utilizando gestos dos dedos, os botões de aproximar/afastar ou os botões de volume no dispositivo. O Vespucci irá então descarregar os dados para a vista atual. Não é necessário autenticar-se para descarregar dados.

### Editar

<a id="lock"></a>

#### Trancar, destrancar, alternar modo

Para evitar edições acidentais, o Vespucci inicia no modo  "bloqueado". Toque no ícone ![Bloqueado](../images/locked.png) para desbloquear o ecrã. 

Se pressionar de forma longa o botão do cadeado, surgirá um menu com 4 opções:

* **Normal** - No modo de edição padrão novos elementos podem ser adicionados, os existentes editados, movidos ou removidos, O símbolo do cadeado fica em branco.  
* **Apenas Etiquetas** - Selecionando um elemento surgirá a o Editor de Propriedades, pressionando de forma contínua no ecrã principal irá adicionar novos elementos, mas não novas geometrias. O símbolo do cadeado fica em branco com um "T".
* **Interior** - Ativa o modo Interior, veja [Indoor mode](#indoor). O símbolo do cadeado fica em branco com um "i".
* **C-Mode** - Ativa o modo C-Mode, apenas elementos qur estejam marcados com avisos serão mostrados. Veja [C-Mode](#c-mode). O símbolo do cadeado fica em branco com um "C".

#### Toque simples, toque duplo e toque longo

Por defeito, os nós e linhas selecionáveis têm uma área laranja à volta deles indicando onde deve tocar para selecionar um objeto. Tem 3 opções:

* Toque simples: seleciona o objeto. 
    * Um nó ou linha isolados é destacado imediatamente. 
    * No entanto se tentar selecionar um objeto e o Vespucci determinar  que a seleção pode ser vários objetos, rá mostrar um menu de seleção permitindo escolher o objeto que quer selecionar. 
    * Os objetos selecionados são destacados a amarelo. 
    * Para mais informações ver [Nó selecionado](../en/Node%20selected.md), [Linha selecionada](../en/Way%20selected.md) e [Relação selecionada](../en/Relation%20selected.md).
* Toque duplo: ativar [Modo Multi-seleção](../en/Multiselect.md)
* Toque longo: cria um "sinal mais", permitindo adicionar erros reportados. Para mais informações ver [Criar novos objetos](../en/Creating%20new%20objects.md)

É uma boa ideia aproximar a visualização caso a área tenha muitos objetos.

O Vespucci tem um bom sistema de "desfazer/refazer" por isso não tenha medo de fazer experiências de edição, mas por favor não envie essas experiências para o OpenStreetMap.

#### Selecionar / Desselecionar (toque simples e "menu de seleção")

Toque num objeto para o selecionar e destacá-lo. Se tocar numa área vazia irá desselecionar. Se selecionou um objeto e necessita de selecionar outra coisa, simplesmente toque nesse objeto, não necessita de desselecionar o primeiro. Se tocar 2 vezes num objeto irá iniciar o [modo Multi-seleção](../en/Multiselect.md).

Note que se tentar selecionar um objeto e se o Vespucci determinar que a seleção pode referir-se a vários objetos (como um nó numa linha, ou outros objetos sobrepostos) irá ver um menu de seleção: toque no objeto que quer selecionar. 

Os objetos selecionados são indicados com uma borda amarela fina. A borda amarela pode ser difícil de ver, dependendo do fundo do mapa e do fator de ampliação. Após ser feita uma seleção, irá ver uma mensagem a confirmar a seleção.

Após a seleção estar feita irá ver (quer com botões, quer com menu) uma lista de operações permitidas para o objeto selecionado: para mais informações ver [Nó selecionado](../en/Node%20selected.md), [Linha selecionada](../en/Way%20selected.md) e [Relação selecionada](../en/Relation%20selected.md).

#### Objetos selecionados: Editar etiquetas

Um segundo toque no objeto selecionado abre o editor de etiquetas e poderá editar as etiquetas associadas ao objeto.

Note que nos objetos que se sobrepõem (como um nó numa linha) irá aparecer de novo o menu. Selecionando de novo o mesmo objeto irá mostrar o editor de etiquetas; selecionando outro objeto simplesmente seleciona o outro objeto.

#### Objetos selecionados: Mover um Nó ou Linha

Após selecionar um objeto, este pode ser movido. Note que os objetos podem ser arrastados/movidos apenas quando estão selecionados. Simplesmente arraste perto de um objeto selecionado (dentro da zona de tolerância) para o mover. Se tiver definido nas preferências uma área grande de arrasto, obtém uma zona larga à volta do nó selecionado que torna mais fácil posicionar o objeto. 

#### Adicionar um novo Nó/Ponto ou Linha (toque longo)

Faça um toque longo no local onde quer o nó ou o início da linha. Irá ver o sinal mais. 
* Se quiser criar um novo nó (não ligado a nenhum objeto), toque longe dos objetos existentes.
* Se quiser prolongar uma linha, toque dentro da "zona de tolerância" da linha (ou no nó da linha). A zona de tolerância é indicada pelas áreas à volta de um nó ou linha.

Assim que veja um sinal mais, tem as seguintes opções:

* Toque no mesmo local.
    * Se o sinal mais não estiver perto de um nó, tocar de novo no mesmo local cria um novo nó. Se for perto de uma linha (mas não perto de um nó), o novo nó fará parte da linha (e estará ligado à linha).
    * Se o sinal mais estiver perto de um nó (dentro da zona de tolerância), tocar no mesmo local apenas seleciona o nó e abre o editor de etiquetas. Não é criado um novo nó. A ação é a mesma que a seleção em cima.
* Toque noutro local (fora da zona de tolerância do sinal mais) adiciona um novo segmento à linha da posição original para a posição atual. Se o sinal mais estiver perto de uma linha ou nó, o novo segmento será ligado a esse nó ou linha.

Simplesmente toque no ecrã onde quer adicionar mais nós na linha. Para terminar, toque no nó final 2 vezes. Se o nó final se encontrar numa linha ou nó existente, o segmento ficará ligado automaticamente à linha ou nó. 

Também pode usar o menu: ver [Criar novos objetos](../en/Creating%20new%20objects.md) para mais informações.

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

O Vespucci tem uma função "adicionar etiquetas de endereço" que torna mais fácil esta operação. Pode ser selecionado:

* após um toque longo: o Vespucci adiciona um nó no local e tentará adivinhar o endereço mais próximo e número de porta ultimamente usados. Se o nó estiver dentro de um edifício irá introduzir automaticamente "entrance=yes" (entrada=sim) ao nó. Aparece o editor de etiquetas para poder fazer outras alterações.
* no modo nó/linha selecionados: o Vespucci adicionará etiquetas de endereços como descrito acima e abrir o editor de etiquetas.
* no editor de etiquetas.

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

O Vespucci tem um solucionador de conflitos simples. No entanto se suspeitar que existem erros graves a resolver com as suas edições, exporte as suas alterações num ficheiro .osc  (item "Exportar" no menu "Transferir") e corrija os conflitos no JOSM. Para mais informações veja [resolução de conflitos](../en/Conflict%20resolution.md).  

## Utilizar o GPS

Pode usar o Vespucci para criar trilhos GPS e mostrá-los no seu dispositivo. Pode também saber a localização GPS atual (ativar em "Ver localização" no menu GPS) e/ou ter no centro do ecrã a localização e seguir a localização (ativar em "Seguir posição GPS" no menu GPS). 

Se tiver ativado o Seguir posição GPS, se deslocar o ecrã manualmente ou editar alguma coisa, irá desativar o "Seguir posição GPS" e a seta azul do GPS irá mudar de uma seta com linha de contorno para uma seta preenchida. Para regressar rapidamente para o modo de "seguir", toque no botão GPS ou ative de novo a opção no menu.

## Notas e Erros Reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

As Notas e Erros Reportados são mostrados no mapa com um pequeno ícone ![Erro](../images/bug_open.png), os verdes são erros fechados/resolvidos, os azuis são os criados ou editados por si e os amarelos são aqueles que ainda estão abertos/por resolver. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

### Filtros

Para além disso, se ativar os erros reportados também pode ativar o filtro espaçado de visualização para reduzir a confusão. Nas "Preferências avançadas" pode selecionar individualmente:

* Notas reportadas
* Erro Osmose
* Aviso Osmose
* Pequeno problema Osmose

<a id="indoor"></a>

## Modo interiores

Mapear interiores de edifícios pode ser desafiante devido ao elevado número de objetos que se sobrepõem. O Vespucci tem um Modo Interiores que permite filtrar todos os objetos que não estejam no mesmo andar e adiciona automaticamente o andar aos objetos criados.

O modo pode ser ativadoo ao pressionar de forma longa o botão do cadeado, ver [Lock, unlock, mode switching](#lock) e selecionar o respetivo menu.

<a id="c-mode"></a>

## Modo-C

No Modo-C apenas são mostrados os objetos que têm uma bandeira de aviso definida, o que torna fácil detetar objetos que têm problemas específicos ou que correspondam a verificações configuráveis. Se for selecionado um objeto e o Editor de Propriedades iniciado no Modo-C, será aplicado automaticamente o modelo de melhor correspondência.

O modo pode ser ativadoo ao pressionar de forma longa o botão do cadeado, ver [Lock, unlock, mode switching](#lock) e selecionar o respetivo menu.

### Configurar verificações

Neste momento existem duas verificações configuráveis (existe uma verificação para etiquetas FIXME e um teste para tipos de etiquetas que faltam em relações que não são neste momento configuráveis). Ambas podem ser configuráveis selecionando "Preferências do Verificador" em "Preferências". 

A lista de entradas está dividida em duas, a primeira metade mostra entradas de "novos levantamentos" e a segunda metade "entradas" de verificação. As entradas podem ser editadas clicando nelas. O botão do menu verde permite adicionar entradas.

#### Entradas de novos levantamentos

O levantamento possui as seguintes propriedades:

* **Chave** - Chave da etiqueta de interesse.
* **Valor** - Valor que a etiqueta de interesse deve ter, se estiver vazia o valor da chave será ignorado.
* **Idade** - quantos dias, após a última alteração ao elemento, devem passar para que o elemento seja novamente verificado no terreno. Se estiver presente o campo check_date este será utilizado, caso contrário será a data da versão atual. Definir o valor a zero irá levar a que a verificação simplesmente corresponda com a chave e o valor.
* **Expressão regular** - se **Valor** estiver ativo, é asumido que seja uma expressão regular em JAVA.

**Chave** e **Valor** são verificados de acordo com as etiquetas _existentes_ do objeto em questão.

#### Verificar entradas

A verificação de entradas tem as seguintes duas propriedades:

* **Chave** - Chave que deve estar presente no objeto de acordo com o modelo correspondente.
* **Verificação opcional** - Verifica as etiquetas opcionais do modelo correspondente.

Esta verificação funciona determinando primeiro o modelo correspondente e então verifica se a **Chave** é uma chave "recomendada" kpara esse objeto de acordo com o modelo, **Verificação opcional** irá alargar a verificação a etiquetas que sejam "opcionais * no objeto. Nota: neste momento os modelos ligados não são verificados.

## Filtros

### Filtro baseado em etiquetas

O filtro pode ser ativado no menu principal, pode ser então alterado tocando no ícone de filtro. Para mais informações ver [Filtro de Etiquetas](../en/Tag%20filter.md).

### Filtro baseado em modelos de etiquetas

Uma alternativa ao descrito anteriormente, os objetos podem ser filtrados com base em modelos de etiquetas individuais ou grupos destas. Tocando no ícone de filtro irá mostrar uma lista de seleção de modelos. Os modelos individuais podem ser selecionados apenas com um toque, e os grupos de modelos por um toque longo (toque normal abre o grupo). Para mais informações ver [Filtro de modelos de etiquetas](../en/Preset%20filter.md).

## Personalizar o Vespucci

### Configurações que poderá querer alterar

* Camada de fundo
* Camada superior. Adicionar uma camada superior pode causar problemas com dispositivos antigos assim como aqueles com memória RAM limitada. Padrão: nenhuma.
* Visualização de erros reportados. Os erros reportados que estejam abertos serão mostrados com um ícone de um inseto amarelo. Os erros reportados fechados serão mostrados a verde. Padrão: ativo.
* Camada de fotos. Mostra fotografias geo-referenciadas com um ícone de uma máquina fotográfica vermelha. Se estiver disponível a direção em que foi tirada, o ícone será rodado. Padrão: desativado.
* Ícones dos nós. Padrão: ativo.
* Manter ecrã ligado. Padrão: desativado.
* Área grande de arrasto. Mover nós num dispositivo com ecrã sensível ao toque é problemático uma vez que os dedos irão tapar a posição atual no ecrã. Ao ativar esta opção irá poder usar uma área grande de toque descentrada (a seleção e outras operações continuam a usar a área normal de tolerância). Padrão: desativado.

#### Preferências avançadas

* Mostrar sempre menu de contexto. Quando ativado, qualquer selecionar irá mostrar o menu de contexto. Se desativado, o menu de contexto apenas é mostrado quando for possível determinar apenas um elemento selecionado. Padrão: desativado (antes era ativo).
* Ativar tema em tons claros. Em dispositivos recentes isto é ativado por padrão. Apesar de se poder ativar isto em dispositivos antigos, os grafismos podem ser inconsistentes.
* Ver estatísticas. Mostra algumas informações no canto inferior esquerdo para depuração de erros do programa. Padrão: desativado (antigamente estava sempre ativado).  

## Reportar Problemas

Se o Vespucci bloquear ou for abaixo. Ser-lhe-á perguntado se pretende enviar o relatório de erro com informações sobre este aos programadores do Vespucci. Os programadores agradecem que enviem esta informação, mas por favor envie um relatório de erro apenas para um mesmo erro/situação. Se quiser adicionar outras informações que possam ser úteis aos programadores ou requisitar outras funcionalidades novas no Vespucci use o [Repositório de erros do Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Se quiser discutir algo relacionado com o Vespucci, pode abrir uma discussão no [Grupo Google do Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) ou no [fórum Android do OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


