# Vespucci Introducción

Vespucci é un editor OpenStreetMap con tódalas funcións que admite a maioría das operacións que proporcionan os editores de escritorio. Foi probado con éxito no Android de Google 2.3 a 7.0 e varias variantes baseadas na AOSP. Unha palabra de precaución: mentres as capacidades do dispositivo móbil atópanse cos seus rivais de escritorio, especialmente os dispositivos máis antigos teñen memoria moi limitada dispoñible e adoitan ser bastante lentos. Debes ter en conta a miña conta ao usar Vespucci e manter, por exemplo, as áreas que estás a editar nun tamaño razoable. 

## Primeira vez de uso

No inicio Vespucci mostra a caixa de diálogo "Descargar outra localización" / "Cargar área". Se tes as coordenadas que se amosan e queres descargar de xeito inmediato, podes seleccionar a opción adecuada e configurar o radio ao redor da localización que queres descargar. Non seleccione unha área grande en dispositivos lentos. 

Alternativamente, pode descartar o diálogo premendo o botón "Ir ao mapa" e xirar e ampliar a unha localización onde desexa editar e descargar os datos (ver a continuación: "Edición con Vespucci").

## Editando con Vespucci

Dependendo do tamaño da pantalla e da idade do seu dispositivo, as accións de edición poden ser accesibles directamente a través das iconas na barra superior, a través dun menú desplegable á dereita da barra superior, desde a barra inferior (se está presente) ou a través da tecla de menú.

<a id="download"></a>

### Descargando información OSM

Selecciona a icona de transferencia! [Transfer](../images/menu_transfer.png) ou o "Transfer" artigo do menu. Isto mostrará sete opcións:

* **Baixar vista actual** - descargue a área visible na pantalla e substitúa os datos existentes *(require conexión a internet)*
* **Engadir vista actual para descargar** - descargue a área visible na pantalla e fúndea cos datos existentes *(require conexión a internet)*
* **Descargar noutro lugar** - mostra un formulario que lle permite introducir coordenadas, buscar unha localización ou usar a posición actual e, a continuación, descargar unha área en torno a esa localización *(require conexión a internet)*
* **Subir datos ao server de OSM** - subir e editar OpenStreetMap *(require autenticación)* *(require conexión a internet)*
* **Auto baixada** - descargue unha área ao redor da situación xeográfica actual automaticamente *(require conexión a internet)* *(require GPS)*
* **Arquivo...** - salvar e cargar información en OSM a/dende arquivos do dispositivo.
* **Nota/Bugs...** - baixar (automaticamente e manualmente) OSM Notas e "Bugs" dende QA ferramentas (actualmente OSMOSE) *(require conexión a internet)*

A forma máis sinxela de descargar datos ao dispositivo é achegar e panoramizar a localización que desexa editar e logo seleccionar "Descargar vista actual". Podes achegar os xestos, os botóns de zoom ou os botóns de control de volume do dispositivo. Vespucci debería entón descargar datos para a vista actual. Non se precisa autenticación para descargar datos no dispositivo.

### Editando

<a id="lock"></a>

#### Bloquear, desbloquear, "só editor de etiquetas", modo interior 

Para evitar edicións accidentais Vespucci comeza no modo "bloqueado", un modo que só permite achegar e mover o mapa. Tócao! [Locked](../images/locked.png) icona para desbloquear a pantalla. 

Unha pulsación longa na icona de bloqueo habilitará o modo "Edición de etiquetas só" que non permitirá editar a xeometría dos obxectos ou moverlos, este modo indícase cun icono de bloqueo branco un pouco diferente. Non obstante, pode crear novos nodos e formas cunha prensa longa como normal.

Outra pulsación longa activará  [Indoor mode](#indoor), e outra máis volverá ao modo de edición normal.

Toque único, dobre toque e prema longa

De xeito predeterminado, os nodos e os modos seleccionables teñen unha área en laranxa ao redor dela, indicando aproximadamente onde ten que tocar para seleccionar un obxecto. Tes tres opcións:

* Toque único: obxecto seleccionado. 
    * Un nodo/vía aillado é destacado inmediatamente. 
    * Con todo, se ao tentar seleccionar un obxecto ea selección Vespucci que determina varios obxectos podería significar que presentará un menú de selección, permítelle escoller o obxecto que quere seleccionar. 
    * Os obxectos seleccionados son destacadas en amarelo. 
    * Para obter información vexa [Node selected](../en/Node%20selected.md), [Way selected](../en/Way%20selected.md) and [Relation selected](../en/Relation%20selected.md).
* Doble selección: Iniciar [Multiselect mode](../en/Multiselect.md)
* Presión longa: Crea unha "mira", permítelle engadir nós, vexa abaixo e [Creating new objects](../en/Creating%20new%20objects.md)

Ista é unha boa estratexia facer zoom se ti tentas editar unha área de alta densidade.

Vespucci ten un bo sistema "desfacer/refacer" así que non teñas medo de experimentar no teu dispositivo, pero non cargues e gardes datos de proba ao chou.

#### Selección / Deselección (toque único e "menú de selección")

Toca un obxecto para seleccionar e resaltalo. Ao tocar a pantalla nunha rexión baleira desmarcarase. Se seleccionou un obxecto e precisa seleccionar outra cousa, simplemente toque o obxecto en cuestión, non hai necesidade de desactivar primeiro. Aparecerá un toque dobre sobre un obxecto [Multiselect mode](../en/Multiselect.md).

Teña en conta que se intentas seleccionar un obxecto e Vespucci determina que a selección podería significar varios obxectos (como un nodo dun xeito ou outros obxectos solapados) presentará un menú de selección: toque o obxecto que desexe seleccionar e o obxecto é seleccionado. 

Os obxectos seleccionados son indicados a través dun delgado borde amarelo. O bordo amarelo pode ser difícil de detectar, dependendo do fondo do mapa e do factor de zoom. Unha vez feita unha selección, verás unha notificación que confirma a selección.

Unha vez completada a selección verá (xa sexa como botóns ou como elementos de menú) unha lista de operacións admitidas para o obxecto seleccionado: Para máis información consulte [Node selected](../en/Node%20selected.md), [Way selected](../en/Way%20selected.md) and [Relation selected](../en/Relation%20selected.md).

#### Seleccione obxectos: Editando etiquetas

Un segundo toque no obxecto seleccionado abre o editor de etiquetas e pode editar as etiquetas asociadas ao obxecto.

Teña en conta que para superponer obxectos (como un nodo de certa forma) o menú de selección volve a subir por segunda vez. Ao seleccionar o mesmo obxecto aparecerá o editor de etiquetas; Seleccionar outro obxecto simplemente selecciona o outro obxecto.

#### Obxectos seleccionados: mover un nodo ou camiño

Despois de seleccionar un obxecto, pode moverse. Teña en conta que os obxectos poden ser arrastrados/movidos só cando están seleccionados. Simplemente arrastre preto (isto é, dentro da zona de tolerancia de) o obxecto seleccionado para movelo. Se selecciona a área de arrastre grande nas preferencias, obtén unha gran área ao redor do nodo seleccionado que facilita a posicionamento do obxecto. 

#### Engadir un novo Nodo/Punto ou Camiño (prema prolongadamente)

Manteña premido onde quere que o nodo sexa ou o xeito de comezar. Verás un símbolo de "cruzamento" negro. 
*Se desexa crear un novo nodo (non conectado a un obxecto), faga clic fóra de obxectos existentes.
* Se queres estender un camiño, faga clic dentro da "zona de tolerancia" do camiño (ou un nodo no camiño). A zona de tolerancia está indicada polas áreas ao redor dun nó ou camiño.

Unha vez que podes ver o símbolo da mira, tes estas opcións:

* Toque no mesmo lugar.
    *Se a mira non está preto dun nodo, tocando a mesma localización nuevamente crea un novo nodo. Se estás preto dun xeito (pero non preto dun nodo), o novo nodo estará en camiño (e conectado ao modo).
    * Se a mira está preto dun nodo (é dicir, dentro da zona de tolerancia do nodo), tocar na mesma localización só selecciona o nodo (e se abre o editor de etiquetas. Non se crea ningún nodo novo. A acción é a mesma que a selección anterior.
* Toca outro lugar. Tocar noutro lugar (fóra da zona de tolerancia da cruz) engade un segmento de forma desde a posición orixinal ata a posición actual. Se a mira estaba preto dun xeito ou nodo, o novo segmento estará conectado a ese nodo ou a outro xeito.

Simplemente toque a pantalla onde desexa engadir máis nodos do camiño. Para rematar, toque o nodo final dúas veces. Se o nodo final está situado nun xeito ou nodo, o segmento estará conectado automaticamente ao camiño ou ao nodo. 

Tamén pode usar un elemento de menú: Ver [Creating new objects](../en/Creating%20new%20objects.md) for more information.

#### Engadindo un Área

OpenStreetMap actualmente non ten un tipo de obxecto de "área" contrario a outros sistemas de datos xeométricos. O editor en liña "iD" tenta crear unha área de abstracción dos elementos OSM subxacentes que funciona ben nalgunhas circunstancias, noutros non é así. Vespucci actualmente non trata de facer nada similar, polo que precisa saber un pouco sobre a forma en que as áreas están representadas:

* _closed ways (*polygons")_: A variante de área máis sinxela e máis común, son formas que teñen un primeiro e último nodo compartido que forman un "anel" pechado (por exemplo, a maioría dos edificios son deste tipo). Son moi fáciles de crear en Vespucci, simplemente conéctate ao primeiro nodo cando termine de debuxar a área. Nota: a interpretación do camiño pechado depende da súa etiquetaxe: por exemplo, se un camiño pechado está etiquetado como un edificio considerado como unha área, se está marcado como unha rotonda, non vai. Nalgunhas situacións nas que ambas interpretacións poden ser válidas, unha etiqueta de "área" pode aclarar o uso desexado.
* _multi-ploygons_:Algunhas áreas teñen múltiples partes, buratos e aneis que non se poden representar con só un xeito. OSM usa un tipo específico de relación (o noso obxecto de propósito xeral que pode modelar as relacións entre elementos) para evitar isto, un multipolígono. Un multipolígono pode ter varios aneis "externos" e múltiples aneis "internos". Cada anel pode ser un xeito pechado como se describe arriba, ou varias formas individuais que teñen nós extremos comúns. Mentres os grandes multipolígonos son difíciles de manexar con calquera ferramenta, os pequenos non son difíciles de crear en Vespucci.
* _coastlines_: Para obxectos moi grandes, continentes e illas, mesmo o modelo multipolígono non funciona de forma satisfactoria. Por formas natural=coastline tomamos a semántica dependente de dirección: a terra está no lado esquerdo do camiño, o auga no lado dereito. Un efecto secundario disto é que, en xeral, non debe revertir a dirección dun xeito co etiquetado da costa. Pode atopar máis información na páxina [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mellorar a Xeometría do Camiño

Se achegaches o suficiente de forma seleccionada, verás un pequeno "x" no medio dos segmentos que son o suficientemente longo. Arrastrando o "x" creará un nodo no camiño dese lugar. Nota: para evitar a creación de nodos accidentalmente, a área de tolerancia táctil para esta operación é bastante pequena.

#### Cortar, Copiar & Pegar

Pode copiar ou cortar nodos e xeitos seleccionados e, a continuación, pegar unha ou varias veces nunha nova ubicación. O corte conservará o id e versión osm. Para pegar prema a posición na que desexa pegar (verá un pelo cruzado marcando a localización). A continuación, selecciona "Pegar" no menú.

#### Eficiente Engadindo Enderezos

Vespucci ten unha función de "engadir etiquetas de enderezos" que intenta facer que os enderezos de topografía sexan máis eficientes. Pode ser seleccionado:

* despois dunha longa prensión: Vespucci engadirá un nodo no lugar e fará unha mellor adiviñación no número da casa e engadirá as etiquetas de enderezos que estivo a usar recentemente. Se o nodo está nun contorno do edificio engadirá automaticamente unha etiqueta "entrada = si" ao nodo. O editor de etiquetas abrirase para o obxecto en cuestión e permitirá que realice os cambios necesarios.
* no modos nodo / modo seleccionado: Vespucci engadirá as etiquetas de enderezo como se menciona arriba e inicie o editor de etiquetas.
* no editor de etiquetas.

A predicción de números de casa normalmente require que polo menos dous números de casa a cada lado da estrada para ser ingresados ao traballo, cantos máis números presentes nos datos mellor.

Considerar usando isto con modo [Auto-download](#download).  

#### Engadindo Restriccións de Xiro

Vespucci ten un xeito rápido de engadir restricións de xiro. Se é necesario, dividirase automaticamente e pediralle que volva seleccionar elementos. 

* Seleccione un camiño cunha etiqueta de estrada (as restricións de turno só se poden engadir ás estradas, se hai que facer isto por outras formas, use o modo xenérico "crear relación").
* Selecciona "Engadir restrición" no menú
* Seleccione o nodo "vía" ou o camiño (só os elementos "via" posibles terán a área táctil mostrada)
* Seleccione o modo "a" (é posible dobrar e configurar o elemento "a" no elemento "de", Vespucci asumirá que está engadindo un xiro sen restricións no_u_turn restriction)
* Establecer o tipo de restrición

### Vespucci  en modo "pechado"

Cando se amosa o bloqueo vermello, tódalas accións non editadas están dispoñibles. Adicionalmente, unha prensa longa ou próxima a un obxecto mostrará a pantalla de información detallada se é un obxecto OSM.

### Gardar os teus Trocos

*(require conectividade na rede)*

Seleccione o mesmo botón ou elemento de menú que fixeches para a descarga e agora seleccione "Cargar datos no servidor OSM".

Vespucci admite a autorización de OAuth e o método clásico de nome de usuario e contrasinal. OAuth é preferible xa que evita o envío de contrasinais.

As novas instalacións de Vespucci terán OAuth activado por defecto. No primeiro intento de cargar datos modificados, carga unha páxina do sitio web de OSM. Despois de iniciar sesión (a través dunha conexión cifrada) pediráselle que autorice a Vespucci a editar usando a súa conta. Se queres ou necesitas autorizar o acceso de OAuth á túa conta antes de editar hai un elemento correspondente no menú "Ferramentas".

Se desexa gardar o seu traballo e non ten acceso a Internet, pode gardar nun ficheiro .osm compatíbel con JOSM e cargar máis tarde con Vespucci ou con JOSM. 

#### Resolvendo conflitos en subidas

Vespucci ten xeitos sinxelos de resolver conflitos. Con todo, se sospeita que hai grandes problemas coas súas edicións, exporte os seus cambios nun arquivo .osc ( "Export" elemento de menú no menú "transferencia") e resolvelos e envios con JOSM. Consulte a axuda detallada sobre [conflict resolution](../en/Conflict%20resolution.md).  

## Usando GPS

Podes usar Vespucci para crear unha pista GPX e visualizalo no teu dispositivo. Ademais, pode mostrar a posición actual do GPS (configurar "Mostrar localización" no menú GPS) e/ou ter a pantalla en torno e seguir a posición (configure "Seguir posición GPS" no menú GPS). 

Se ten este último axustado, mover a pantalla manualmente ou editar fará que o modo "seguir o GPS" se desactive e que a frecha GPS azul cambie dun esquema a unha frecha chea. Para volver rapidamente ao modo "seguir", simplemente toque o botón GPS ou volva a verificar a opción do menú.

## Notas e Bugs

Vespucci permite descargar, comentar e pechar Notas OSM (anteriormente OSM Bugs) e a funcionalidade equivalente de "Bugs" producida pola [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Ambas teñen que ser descargadas de forma explícita ou pode usar a instalación de descarga automática para acceder aos elementos da súa área inmediata. Unha vez editado ou pechado, pode cargar o erro ou Nota inmediatamente ou cargar todo dunha soa vez.

No mapa, as notas e os erros están representados por unha pequena icona de erro! [Bug](../images/bug_open.png), os verdes están pechados/resoltos, os que foron creados ou editados son azuis e o amarelo indica que aínda está activo e non foi modificado. 

A visualización de erros de OSMOSE proporcionará unha ligazón ao obxecto afectado en azul, tocando a ligazón seleccionará o obxecto, centrará a pantalla nel e baixará a área previamente se fose necesario. 

### Filtrado

Ademais de habilitar globalmente as notas e os erros, podes establecer un filtro de visualización de grans grosos para reducir o desorden. Nas "Preferencias avanzadas" pódese seleccionar individualmente:

* Notas
* Osmose error
* Osmose coidado
* Osmose menor edición

<a id="indoor"></a>

## Modo Interior

O mapeo en interiores é un reto debido ao gran número de obxectos que moitas veces se superponerán. Vespucci ten un modo interior dedicado que permite filtrar todos os obxectos que non están no mesmo nivel e que engadirán automaticamente o nivel actual aos novos obxectos creados.

O modo pode ser activado premendo longamente no elemento de bloqueo, consulte [Lock, unlock, "tag editing only", indoor mode](#lock).

## Filtros

###  Filgros baseados en Etiquetas

O filtro pódese habilitar desde o menú principal, entón pode modificarse tocando a icona do filtro. Aquí pódese atopar máis documentación [Tag filter](../en/Tag%20filter.md).

### Base do filtro Presente

Unha alternativa ao anterior, os obxectos son filtrados en presentes individuais ou en grupos predefinidos. Ao tocar na icona do filtro aparecerá un diálogo de selección predefinido similar ao usado noutro lugar en Vespucci. Os predefinidos individuais poden ser seleccionados por un clic normal, os grupos predefinidos mediante un longo clic (o clic normal entre o grupo). Aquí pódese atopar máis documentación[Preset filter](../en/Preset%20filter.md).

## Persoalizando Vespucci

### Axustes que ti debes querer trocar

* Capa de fondo
* Capa de superposición. Engadindo unha superposición pode causar problemas con dispositivos máis antigos e con memoria limitada. Predeterminado: ningún.
* Notas/Bugs amosar. As notificacións e os erros abertos mostraranse como unha icona de erro amarelo, os mesmos quedan en verde. Predeterminado: acceso.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Predeterminado: off.
* Nodo iconas. Defecto: on.
* Mantén a pantalla activada. Predeterminado: desactivado
* Área grande de arrastre de nodo. Os nodos móbiles nun dispositivo con entrada táctil son problemáticos, xa que os dedos obscurecerán a posición actual na pantalla. Ao activar isto proporcionará unha gran área que se pode empregar para arrastrar fóra de centro (a selección e outras operacións aínda usan a área de tolerancia táctil normal). Predeterminado: desactivado.

#### Preferencias Avanzadas

* Mostrar sempre o menú contextual. Cando estea activado, cada proceso de selección mostrará o menú contextual, apagado o menú só se amosará cando non se poida determinar ningunha selección sen ambigüidades. Predeterminado: desactivado (usado para estar activado).
* Activar tema lixeiro. En dispositivos modernos, este está activado por defecto. Mentres podes activalo para versións máis antigas de Android, o estilo probablemente non sexa coherente.
* Amosar estatísticas. Mostrará algunhas estatísticas para a depuración, non é realmente útil. Predeterminado: desactivado (usado para estar activado).  

## Reportando Problemas

Se Vespucci falla ou detecta un estado inconsistente, pediráselle que envíe ao garete. Faga isto se isto ocorre, pero por favor só unha vez por situación específica. Se desexa dar máis información ou abrir un problema para unha solicitude de funcións ou similar, faga isto aquí: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Se queres discutir algo relacionado con Vespucci, podes iniciar unha discusión sobre a [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) ou en [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


