# Editor de horarios de apertura de OpenStreetMap

A especificación de horas de apertura de OpenStreetMap é bastante complexa e non se presta fácilmente a unha interface de usuario sinxela e intuitiva.

Non obstante, a maior parte do tempo probablemente só estea usando unha pequena parte da definición. O editor ten isto en conta ao tentar ocultar as características máis escuras nos menús e a maior parte do tempo reducindo o uso "na estrada" a pequenas personalizacións de modelos predefinidos.

_Esta documentación é preliminar e un traballo en curso_

## Usando o editor de horarios de apertura

In a typical workflow the object you are editing will either already have an opening hours tag (opening_hours, service_times and collection_times) or you can re-apply the preset for the object to get an empty opening hours field. If you need to add the field manually and you are using Vespucci you can enter the key on the details page and then switch back to the form based tab to edit. If you believe that the opening hours tag should have been part of the preset, please open an issue for your editor.

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. With the "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template. You can define separate templates and defaults for the "opening_hours", "collection_times" and "service_times" tags. 

Por suposto, pode construír un valor de horas de apertura desde cero, pero recomendaríamos usar un dos modelos existentes como punto de partida.

Se se carga un valor de horas de horario existente, faise un intento automático para corrixilo para que se axuste á especificación de horas de apertura. Se isto non é posible, o lugar en bruto onde se produciu o erro quedará resaltado na visualización do valor de OH sin procesar e pode tentar corrixilo manualmente. Aproximadamente un cuarto dos valores de OH na base de datos OpenStreetMap teñen problemas, pero non se pode corrixir menos do 10%. [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) Para obter máis información sobre as tolerancias das desviacións da especificación.

### Botón menú principal

* __Engadir regra__: engadir unha nova regra
* __Add rule for holidays__: add a new rule for a holiday together with a state change.
* __Engadir regra para 24/7__: Engade unha regra para un obxecto que sempre está aberto, a especificación de horas de apertura non admite ningún outro subvalor para 24/7, pero permitimos a adición de selectores de nivel superior (por exemplo, intervalos de anos).
* __Cargar plantilla__: carga unha plantilla existente.
* __Gardar a plantilla__: Gardar o valor actual das horas de apertura como modelo para o seu uso futuro.
* __Manexar plantilla__: Editar, por exemplo, cambiar o nome e eliminar os modelos existentes.
* __Actualizar__: Repasa o valor da hora de apertura.
* __Delete all__: remove all rules.

### Regras

As regras por defecto son engadidas como regras _normais_: isto implica que terán preferencia sobre os valores das regras anteriores para mesmos días. Isto pode ser unha preocupación cando se especifiquen tempos prolongados, polo que normalmente quererá cambiar as regras a _aditivo_ mediante a entrada de menú _Amosar tipo de regra_.

#### Menú de regras

* __Engadir modificador/comentario__: Muda o efecto desta regra e engade un comentario opcional.
* __Engadir vacacións__: Engade un selector para vacacións públicas ou escolares.
* __Engade intervalo de tempo...__
    * __Hora - hora__: Unha hora de inicio ata unha hora final do mesmo día.
    * __Time - extended time__: a start time to an end time on the next day (example 26:00 is 02:00 (am) the next day).
    * __Hora var. - hora__:Desde unha hora de inicio variable (amencer, anoitecer, abrente e ocaso) ata unha hora final do mesmo día
    * __Hora var. - hora estendida__: Desde unha hora variable inicial ata unha hora de finalización ao día seguinte.
    * __Hora - hora var.__: Desde unha hora inicial ata unha hora variable final.
    * __Hora var. - hora var.__: Desde unha hora variable inicial ata unha hora variable final.
    * __Hora__: Un punto no tempo.
    * __Hora - fin indefinido__: Desde un punto de inicio en adiante.
    * __Hora variable__: No tempo variable
    * __Hora variable - fin indefinido__: Desde un inicio variable en diante
* __Engade un intervalo de días semanais__: Engade un selector baseado na semana.
* __Engadir intervalo de data...__
    * __Data - data__: Desde a data de inicio (ano, mes, día) ata unha data de finalización.
    * __Data variable - data__: Desde unha data de inicio variable (actualmente a especificación só define _pascua_) a unha data de finalización.
    * __Data - data variable__: Desde unha data de inicio ata unha data variable.
    * __Data variable - data variable__: Desde unha data variable inicial ata unha data variable final.
    * __Occurrence in month - occurrence in month__: from a start weekday occurrence in a month to the same.
    * __Occurrence in month - date__: from a start weekday occurrence in a month to a end date.
    * __Date - occurrence in month__: from a start date to an end weekday occurrence in a month.
    * __Occurrence in month - variable date__: from a start weekday occurrence in a month to an end variable date.
    * __Variable date - occurrence in month__: from a start variable date to an end weekday occurrence in a month.
    * __Date - open end__: from a start date onwards.
    * __Data variable - fin indefinido__: Dende unha data de inicio variable en diante.
    * __Occurrence in month - open end__: from a start weekday occurrence in a month onwards.
    * __Con offsets...__: As mesmas entradas que o anterior pero con offsets especificados (isto raramente se usa).
* __Engadir rango anual__: Engade un selector baseado no ano.
* __Engadir rango semanal__: Engade un selector baseado en números semanais.
* __Duplicate__: create a copy of this rule and insert it after the current position.
* __Amosar tipo de regra__: Amosar e permitir o cambio do tipo de regra _normal_, _aditivo_ and _fallback_ (Non dispoñible na primeira regra).
* __Subir__: Mova esta regra cara arriba unha posición (non está dispoñible na primeira regra).
* __Baixar__: Mova esta regra unha posición cara abaixo.
* __Borrar__: elimina esta regra.

### Períodos de tempo

Para facer a edición dos períodos de tempo o máis sinxelo posible, tratamos de escoller un intervalo de tempo e granularidade óptimos para as barras de intervalo ao cargar os valores existentes. Para novos tempos, as barras comezan ás 6:00 (am) e teñen incrementos de 15 minutos; isto pódese cambiar a través do menú.

#### Menú de períodos de tempo

* __Display time picker__: show a large number picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Cambia a tics de 15 minutos__: Use unha granularidade de 15 minutos para a barra de rango.
* __Cambia a tics de 5 minutos__: Use unha granularidade de 5 minutos para a barra de rango.
* __Cambia a tics de 1 minuto__: Use unha granularidade de 1 minuto para a barra de rango, moi difícil de usar nun teléfono.
* __Comezar a medianoite__: Inicie a barra de rango á medianoite.
* __Amosar intervalo__: Amosa o campo de intervalo para especificar un intervalo en minutos.
* __Borrar__: Borra este período de tempo.

