# Introdução do Vespucci

O Vespucci é um editor OpenStreetMap com muitas funcionalidades que suporta a maioria das operações dos editores em computadores. Foi testato com sucesso em Android nas versões 2.3 a 6.0 e outros baseados em Android. A ter em atenção: apesar dos dispositivos móveis serem cada vez mais potentes e com mais capacidades, os dispositivos antigos com alguns anos podem ser lentos e utilizar o Vespucci. Deve ter isto em mente ao usar o Vespucci e manter, por exemplo, o tamanho das áreas a editar num tamanho razoavelmente pequeno. 

## Primeira Utilização

Ao iniciar o Vespucci, é mostrada a janela "Descarregar outra localização"/"Carregar Área". Se tiver as coordenadas a serem mostradas e quiser descarregar de imediato, pode selecionar a opção apropriada e selecionar o raio à volta da localização que quer descarregar. Não é recomendável descarregar áreas grandes em dispositivos lentos/antigos. 

Como alternativa, pode fechar a janela pressionando o botão "Ir para o mapa", deslocar e aproximar numa localização que quer editar e descarregar os dados (ver abaixo"Editar no Vespucci").

## Editar no Vespucci

Dependendo do tamanho do ecrã e a antiguidade do dispositivo que estiver a usar, as funcionalidades de edição podem estar acessíveis através de ícones na barra de cima, através de um menu deslizante à direita da barra de cima, através da barra de baixo (se estiver visível) ou através do botão menu do dispositivo.

### Descarregar Dados OSM

Selecione o ícone de transferir ![](../images/menu_transfer.png)  ou o menu "Transferir". Isto irá mostrar 7 opções:

* **Descarregar vista atual** - descarrega a área visível no ecrã e substitui dados existentes, se for o caso *(necessita de ligação à Internet)*
* **Descarregar e adicionar vista atual** - descarrega a área visível no ecrã e faz a fusão com os dados existentes  *(necessita de ligação à Internet)*
* **Descarregar outra localização** - mostra um formulário que permite introduzir coordenadas, procurar por uma localização ou usar a posição atual e descarregar a área à volta dessa localização *(necessita de ligação à Internet)*
* **Enviar dados para o OSM** - envia as alterações para o OpenStreetMap *(necessita de autenticação de conta de utilizador)* *(necessita de ligação à Internet)*
* **Descarregar automaticamente** - descarrega automaticamente a área à volta da localização atual *(necessita de ligação à Internet)* *(necessita de GPS)*
* **Ficheiro...** - grava e abre ficheiros de dados do OpenStreetMap no dispositivo.
* **Erros Reportados...** - descarrega (automática e manualmente) "Erros Reportados" no OpenStreetMap das ferramentas de manutenção de qualidade de dados (atualmente o OSMOSE) *(necessita de ligação à Internet)*

A forma mais fácil de descarregar dados para o dispositivo é aproximar/afastar e deslocar para a localização que quer editar e então selecionar "Descarregar a vista atual". Pode aproximar/afastar utilizando gestos dos dedos, os botões de aproximar/afastar ou os botões de volume no telemóvel. O Vespucci irá então descarregar os dados para a vista atual. Não é necessário autenticar-se para descarregar dados.

### Editar

Para evitar edições acidentais, o Vespucci abre no modo "bloqueado", um modo que apenas permite aproximar/afastar e mover o mapa.Carregue no ícone ![Locked](../images/locked.png) para desbloquear. Se carregar prolongadamente no ícone irá ativar o modo "Editar apenas etiquetas", que não permite adicionar novos objetos ou editar a geometria dos objetos. Quando este modo está ativo é visível o ícone de bloqueio na cor branca.

Por padrão, os nós e vias selecionáveis têm uma área alaranjada à volta deles que indica onde pode tocar para selecionar um objeto. Se por acaso selecionar um objeto e o vespucci determinar que a seleção pode-se referir a vários objetos, irá aparecer um menu com cada um dos objetos para selecionar. Os objetos selecionados são mostrados a amarelo.

É uma boa ideia aproximar a visualização caso a área tenha muitos objetos.

O Vespucci tem um bom sistema de "desfazer/refazer" por isso não tenha medo de fazer experiências de edição, mas por favor não envie essas experiências para o OpenStreetMap.

#### Selecionar / Desselecionar

Toque num objeto para o selecionar e destacar. Um segundo toque no mesmo objeto abre o editor de etiquetas. Tocar numa região sem objetos irá desselecionar. Se selecionar um objeto e quiser selecionar outro, basta tocar no outro, não sendo necessário desselecionar o primeiro. Ao tocar 2 vezes sobre um objeto irá ativar [Seleção Múltipla](../en/Multiselect.md).o 

#### Adicionar um novo Nó ou Via

Pressionar prolongadamente no local desejado para criar um nó ou iniciar uma via. Aparece o sinal mais. pressionando outra vez no mesmo local cria um novo nó, pressionando noutro local fora da zona de tolerãncia de toque, cria um novo nó unindo ambos. 

Basta então cpressionar noutros locais para adicionar mais nós ao segmento. Para terminar, pressiona-se 2 vezes no nó final. Se o nó inicial e final estiverem numa linha, eles são inseridos numa linha automáticamente.

#### Mover um Nó ou Via

Os objetos podem ser arrastados/movidos apenas quando são selecionados. Se tiver ativado uma área de mover larga nas preferências, obtem uma área grande à volta do nó selecionado o que torna mais fácil posicionar o objeto. 

#### Melhorar Geometria das Vias

Se aproximar bastante verá um pequeno "x" no meio dos segmentos que sejam suficientemente longos. Arrastando o "x" irá criar um nó no segmento nesse local. Nota: para evitar criar acidentalmente os nós, a tolerância de toque para esta operação é bastante pequena.

#### Cortar, Copiar e Colar

Pode copiar ou cortar os nós selecionados e vias, e então colar uma ou várias vezes numa nova localização. Cortar irá preservar o identificador (ID), versão e histórico do objeto. Para colar basta pressionar longamente no local desejado (verá um x marcando o lugar). E então selecione "Colar" no menu.

#### Adicionar Endereços Eficientemente

O Vespucci tem uma função "adicionar etiquetas de endereço" que tenta tornar o levantamento de endereços mias eficiente. Pode ser selecionado 

* após um toque longo: o Vespucci adiciona um nó no local e tentará adivinhar o endereço mais próximo e número de porta ultimamente usados. Se o nó estiver dentro de um edifício irá introduzir automaticamente "entrance=yes" (entrada=sim) ao nó. Aparece o editor de etiquetas para poder fazer outras alterações
* no modo nó/via selecionados: o Vespucci adicionará etiquetas de endereços como descrito acima e abrir o editor de etiquetas.
* no editor de etiquetas.

A adivinhação do número de porta normalmente necessita de 2 números de porta em cada lado da estrada para funcionar. Quantos mais números de porta presentes, melhor funciona.

Para isto, pode ser útil ativar o modo "Descarregar automaticamente.  

#### Adicionar Restrições de Viragem

O Vespucci tem uma forma rápida de introduzir restrições de viragem. Nota: se necessitar de cortar uma linha num nó para a usar na restrições de viragem, é necessário fazer isso primeiro antes de adicionar a restrição de viragem.

* selecione uma via com a etiqueta highway (as restrições de viragem só podem ser adicionadas em vias, se precisar de adicionar restrições a outros elementos, use o modo "criar relação". Se não houver elementos (nó ou linha) onde aplicar a função "via" (através de), o menu para criar a restrição não será mostrado.
* selecione "Adicionar restrição de viragem" no menu
* selecione o nó ou linha "via" (todos os elementos possíveis de "via" serão destacados automaticamente)
* selecione a linha "to" (para). É possível indicar de novo o primeiro elemento "from" (de) da restrição, o vespucci neste caso assumirá que não se pode fazer inversão de marcha na mesma via.
* indique o tipo de restrição no menu de etiquetas

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

Se tiver ativado o Seguir posição GPS, se deslocar o ecrã manualmente ou editar alguma coisa, irá desativar o "Seguir posição GPS" e a seta azul do GPS irá mudar de uma seta com linha de contorno para uma seta preenchida. Para regressar rapidamente para o modo de "seguir", toque na seta ou ative de novo a opção no menu.

## Erros Reportados

O Vespucci permite descarregar, comentar e fechar erros reportados no OpenStreetMap assim como os erros da [ferramenta gestão da de qualidade OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos têm de ser descarregados à parte ou utilizando a funcionalidade de descarregar automaticamente, para que sejam mostrados os erros na área que se está a editar. Após editar ou fechar esses erros, pode-se enviar as alterações imediatamente ou todos eles.

Os Erros Reportados e Erros OSMOSE são mostrados no mapa com um pequeno ícone ![](../images/bug_open.png). Os vermelhos são erros fechados/resolvidos, os azuis são os criados ou editados por si e os amarelos são aqueles que ainda estão abertos/por resolver. 

Os erros OSMOSE fornecem um link para o objeto em questão a azul, tocando no link selecionará o objeto, centra o ecrã nele e descarrega a área se for necessário. 

## Personalizar o Vespucci

### Configurações que poderá querer alterar

* Camada de fundo
* Camada de cima. Se adicionar uma camada de cime pode ter problemas caso use um dispositivo antigo ou com pouca memória RAM. Padrão: nenhuma.
* Erros Reportados. Os Erros Reportados e Erros OSMOSE por resolver são mostrados a amarelo. Os erros reolvidos são mostrados a verde. Padrão: ativado.
* Camada de fotos. Mostra fotografias geo-referenciadas num ícone de uma máquina fotográfica a vermelho. Se a fotografia tiver a informação da direção em que foi tirada será mostrado o ícone a apontar para a direção. Padrão: desativado.
* Ver ícones dos nós. Padrão: ativado.
* Manter ecrã ligado. Padrão: desativado.
* Área grande de arrasto do nó. Tentar mover nós num dispositivo com ecrã tátil pequeno pode ser problemático uma vez que os dedos tapam posição atual no ecrã. Ativando isto pode-se arrastar um nó ou outro elemento facilmente (no selecionar e outras operações é utilizada uma outra definição da área de tolerância). Padrão: desativado.

#### Preferências avançadas

* Dividir barra de ações em 2. Em telemóveis recentes a barra de ações é dividida em 2 e aparece uma parte em cima e outra em baixo do ecrã. na de baixo aparecem os botões. Normalmente isto permite mostrar mais botões, mas usa mais espaço no ecrã. Desativando isto a barra aparece apenas no topo. Nota: é necessário reiniciar o Vespucci caso se altere isto.
* Usar sempre menus de contexto. Quando ativado, sempre que se selecionar alguma coisa será mostrado o menu de contexto. Se desativado, o menu de contexto é mostrado apenas quando existe uma seleção ambígua que pode ser em mais do que 1 elemento. Padrão: desativado (antigamente estava sempre ativado).
* Ativar tema em tons claros. Em dispositivos recentes isto é ativado por padrão. Apesar de se poder ativar isto em dispositivos antigos, os grafismos podem ser inconcistentes.
* Ver estatísticas. Mostra algumas informações no canto inferior esquerdo para depuração de erros do programa. Padrão: desativado (antigamente estava sempre ativado).  

## Reportar Problemas

Se o Vespucci bloquear ou for abaixo. Ser-lhe-á perguntado se pretende enviar o relatório de erro com informações sobre este aos programadores do Vespucci. Os programadores agradecem que enviem esta informação, mas por favor envie um relatório de erro apenas para um mesmo erro/situação. Se quiser adicionar outras informações que posssam ser úteis aos programadores ou requisitar outras funcionalidades novas no vespucci use o [Repositório de erros do Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Se quiser discutir algo relacionado com o Vespucci, pode abrir uma discussão no [Grupo google do Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) ou no [fórum Android do OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


