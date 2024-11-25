# Editor de horário de abertura do OpenStreetMap

A especificação dos horários de aberturas no OpenStreetMap é bastante complexa e não permite uma utilização simples e intuitiva.

No entanto, na maioria das vezes só se usa uma pequena parte da especificação. O editor tem isto em conta e tenta simplificar não mostrando outras opções avançadas nos menus, assim como mostrar pequenos modelos de preenchimento úteis em levantamentos no terreno.

_Esta documentação é temporária e encontra-se em desenvolvimento_

## Usar o editor de horário de abertura

Num processo normal, o objeto que está a editar irá ter uma etiqueta de horário de abertura (opening_hours, service_times e collection_times) ou então poderá tornar a aplicar o modelo de etiquetas ao objeto para obter um campo de horário de abertura limpo. Se necessitar de adicionar o campo manualmente e estiver a usar o Vespucci, pode introduzir a chave na página de detalhes e então mudar para a aba baseada em formulário para editá-la. Se acreditar que a etiqueta de horário de abertura devia fazer parte do modelo de etiquetas, por favor abra um relatório para o seu editor.

Se tiver definido um modelo predefinido (faça-o através do item de menu "Gerir modelos"), este será carregado automaticamente quando o editor for iniciado com um valor vazio. Com a função "Carregar modelo" pode carregar qualquer modelo guardado e com o menu "Guardar modelo" pode guardar o valor atual como um modelo. Pode definir modelos separados e predefinições para chaves específicas, por exemplo, "opening_hours", "collection_times" e "service_times" ou valores personalizados. Além disso, pode limitar a aplicabilidade de um modelo a uma região e a um identificador específico, normalmente uma etiqueta de nível superior do OSM (por exemplo, amenity=restaurant).

Claro que pode construir um valor de abertura a partir do nada, mas recomendamos usar um dos modelos existentes como ponto de partida.

Se for carregado um valor de horário de abertura existente, é feita uma tentativa para corrigir automaticamente para estar conforme a especificação do horário de abertura. Se isso não for possível, a localização onde o erro se encontra é destacada e poderá corrigir manualmente. Certa de um quarto de todos os valores de horários de abertura no OpenStreetMap têm problemas, mas menos de 10% não podem ser corrigidos. Veja [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) para mais informação sobre que desvios da especificação são tolerados.

### Botão do menu principal

* __Adicionar regra__: adiciona uma nova regra.
* __Adicionar regra para feriado__: adiciona uma nova regra para feriado junto com um alteração de estado.
* __Adicionar regra para 24/7__: adiciona uma regra para um elemento que está sempre aberto, a especificação de horário de abertura não suporta mais nenhum sub valor para 24/7, no entanto permite-se adicionar seletores de alto nível (por exemplo, intervalos de tempo).
* __Carregar modelo__: carrega um modelo existente.
* __Gravar no modelo__: grava os valores do horário de abertura atual num modelo para futura reutilização.
* __Gerir modelos__: editar, por exemplo alterar o nome e eliminar os modelos existentes.
* __Atualizar__: torna a processar os valores das horas.
* __Eliminar tudo__: elimina todas as regras.

### Regras

As regras padrão são adicionadas como regras _normais_, isto implica que elas irão gravar por cima que quaisquer valores existentes para os mesmo dias. Isto pode ser um problema ao especificar horas expandidas, normalmente irá querer mudar as regras através do menu _Mostrar tipo de regra_ para _aditivo_.

#### Menu de regras

* __Adicionar estado/comentário__: altera o efeito desta regra e permite adicionar um comentário opcional.
* __Adicionar feriado__: adiciona um seletor para feriados civis ou escolares.
* __Adicionar segmento de tempo...__
    * __Hora - hora__: uma hora inicial e final no mesmo dia.
    * __Hora - tempo alargado__: uma hora inicial e uma final no dia seguinte (por exemplo 26:00 corresponde às 02:00 do dia seguinte)
    * __Hora variável - hora__: de uma hora inicial variável (amanhecer, anoitecer, nascer do sol e pôr do sol) até uma hora de fecho no mesmo dia.
    * __Hora variável - tempo alargado__: de uma hora inicial variável até uma hora de fecho no dia seguinte.
    * __Hora - hora variável__: de uma hora de início até uma hora de fim.
    * __Hora variável - hora variável__: de uma hora de início variável até uma hora de fim variável.
    * __Hora__: um ponto no tempo.
    * __Hora-fecho após últ.cliente__: começa numa hora inicial.
    * __Hora variável__: na hora variável
    * __Hora variável-fecho após últ.cliente__: começa numa hora inicial variável.
* __Adicionar intervalo de dia da semana__: adiciona um seletor baseado no dia da semana.
* __Adicionar intervalo de datas...__
    * __Data - data__: de uma data inicial (ano, mês, dia) até uma data de fim.
    * __Data variável - data__: de uma data inicial variável (atualmente a especificação apenas define a _páscoa_) até uma data de fim.
    * __Data - data variável__: de uma data inicial até uma data variável.
    * __Data variável - data variável__: de uma data inicial variável até uma data de fim variável.
    * __Ocorrência no mês - ocorrência no mês__: de uma ocorrência no mês do dia da semana até ao mesmo.
    * __Ocorrência num mês - data__: de uma ocorrência no mês do dia da semana até a uma data de fim.
    * __Data - ocorrência num mês__: de uma data de início até uma ocorrência no mês do dia da semana.
    * __Ocorrência no mês - data variável__: de uma ocorrência no dia da semana num mês até uma data de fim variável.
    * __Data variável - ocorrência no mês__: de uma data inicial variável até uma ocorrência no dia da semana num mês.
    * __Data - fecho após últ.cliente__: começa numa data inicial.
    * __Data variável - fecho após últ.cliente__: começa numa data inicial variável.
    * __Ocorrência no mês - fecho após últ.cliente__: começa num dia da semana num mês inicial.
    * __Com deslocamentos...__: como as mesmas entradas de cima mas com deslocamentos especificados (isto raramente é usado)
* __Adicionar intervalo de anos...__    
    * __Adicionar intervalo de anos__: adiciona um seletor baseado em anos.
    * __Adicionar ano de início__: adicionar um intervalo de anos com fim aberto.
* __Adicionar intervalo de semanas__: adiciona um seletor baseado no número da semana
* __Duplicar__: cria uma cópia da regra atual e insere-a após a posição atual.
* __Mostrar tipo de regra__: mostra e permite mudar o tipo de regra _normal_, _aditivo_ e _alternativo (ou)_ (não disponível na primeira regra).
* __Mover para cima__: move uma posição para cima a regra atual (não disponível na primeira regra).
* __Mover para baixo__: move uma posição para baixo a regra atual
* __Eliminar__: elimina a regra atual.

### Segmentos de tempo

Para tornar a edição de segmentos de tempo o mais fácil possível, tentamos escolher o melhor intervalo de tempo e granularidade para as barras de intervalo ao carregar valores existentes. Para novos segmentos de tempo novos, as barras começam às 6:00 e têm incrementos de  minutos. Isto pode ser alterado no menu.

Clicar (não nos pinos) na barra de tempo abrirá o grande seletor de tempo, quando a utilização direta das barras for demasiado difícil. Os seletores de tempo prolongam-se até ao dia seguinte, pelo que são uma forma simples de prolongar um intervalo de tempo sem ter de apagar e voltar a adicionar o intervalo.

#### Menu de segmento de tempo

* __Mostrar seletor de hora__: mostra um seletor de hora grande para selecionar a hora de início e de fim, em ecrãs muito pequenos esta é a forma preferida de mudar as horas.
* __Mudar para intervalos de 15 minutos__: usa intervalos de 15 minutos na barra de intervalo.
* __Mudar para intervalos de 5 minutos__: usa intervalos de 5 minutos na barra de intervalo.
* __Mudar para intervalos de 1 minuto__: usa intervalos de 1 minuto na barra de intervalo, pode ser difícil de usar esta opção num ecrã pequeno.
* __Começar à meia-noite__: a barra de intervalo começa à meia-noite.
* __Mostrar intervalo__: mostra o campo de intervalo para especificar um intervalo em minutos.
* __Eliminar__: eliminar este segmento de tempo.

### Gerir modelos

A caixa de diálogo de gestão de modelos permite-lhe adicionar, editar e eliminar modelos.

No Android 4.4 e posterior, a seguinte funcionalidade adicional está disponível a partir do botão de menu.

* __Mostrar tudo__: mostra todos os modelos na base de dados.
* __Guardar no ficheiro__: guarda o conteúdo da base de dados de modelos num ficheiro.
* __Carregar de ficheiro (substituir)__: carrega modelos de um ficheiro substituindo o conteúdo atual da base de dados.
* __Carregar do ficheiro__: carrega modelos de um ficheiro mantendo o conteúdo atual.

#### Diálogos para guardar e editar modelos

O diálogo permite-lhe definir

* __Nome__ um nome descritivo para o modelo.
* __Predefinição__ se estiver selecionado, será considerado como um modelo predefinido (normalmente mais limitado pelos outros campos).
* __Chave__ a chave para a qual este modelo é relevante; se definido como _Chave personalizada_, pode adicionar um valor não normalizado no campo abaixo. Os valores de chave suportam os wild cards SQL, ou seja, _%_ corresponde a zero ou mais caracteres, *_* corresponde a um único carácter. Ambos os caracteres de wild card podem ser escapados com _\\_ para correspondências literais.
* __Região__ a região à qual o modelo é aplicável.
* __Objeto__ uma cadeia de caracteres específica da aplicação a utilizar para a correspondência.

