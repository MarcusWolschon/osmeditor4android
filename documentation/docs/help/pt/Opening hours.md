# Editor de horário de abertura do OpenStreetMap

A especificação dos horários de aberturas no OpenStreetMap é bastante complexa e não permite uma utilização simples e intuitiva.

No entanto, na maioria das vezes só se usa uma pequena parte da especificação. O editor tem isto em conta e tenta simplificar não mostrando outras opções avançadas nos menus, assim como mostrar pequenos modelos de preenchimento úteis em levantamentos no terreno.

_Esta documentação é temporária e encontra-se em desenvolvimento_

## Usar o editor de horário de abertura

Num processo normal, o objeto que está a editar irá ter uma etiqueta de horário de abertura (opening_hours, service_times e collection_times) ou então poderá tornar a aplicar o modelo de etiquetas ao objeto para obter um campo de horário de abertura limpo. Se necessitar de adicionar o campo manualmente e estiver a usar o Vespucci, pode introduzir a chave na página de detalhes e então mudar para a aba baseada em formulário para editá-la. Se acreditar que a etiqueta de horário de abertura devia fazer parte do modelo de etiquetas, por favor abra um relatório para o seu editor.

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. With the "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template. You can define separate templates and defaults for specific key, for example "opening_hours", "collection_times" and "service_times" or custom values. Further you can limit applicability of a template to a region and a specific identifier, typically an OSM top-level tag (for example amenity=restaurant). 

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

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Menu de segmento de tempo

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Mudar para intervalos de 15 minutos__: usa intervalos de 15 minutos na barra de intervalo.
* __Mudar para intervalos de 5 minutos__: usa intervalos de 5 minutos na barra de intervalo.
* __Mudar para intervalos de 1 minuto__: usa intervalos de 1 minuto na barra de intervalo, pode ser difícil de usar esta opção num ecrã pequeno.
* __Começar à meia-noite__: a barra de intervalo começa à meia-noite.
* __Mostrar intervalo__: mostra o campo de intervalo para especificar um intervalo em minutos.
* __Eliminar__: eliminar este segmento de tempo.

### Gerir modelos

The template management dialog allows you to add, edit and delete templates.

In Android 4.4 and later the following additional functionality is available from the menu button. 

* __Show all__: display all templates in the database.
* __Save to file__: write the contents of the template database to a file.
* __Load from file (replace)__: load templates from a file replacing the current contents of the database.
* __Load from file__: load templates from a file retaining the current contents.

#### Save and edit template dialogs

The dialog allows you to set

* __Name__ a descriptive name for the template.
* __Default__ if checked this will be consider as a default template (typically further constrained by the other fields).
* __Key__ the key this template is relevant for, if set to _Custom key_ you can add a non-standard value in the field below. The key values support SQL wild cards, that is _%_ matches zero or more characters, *_* matches a single character. Both wild card characters can be escaped with _\\_ for literal matches.
* __Region__ the region the template is applicable to.
* __Object__ an application specific string to use for matching.

