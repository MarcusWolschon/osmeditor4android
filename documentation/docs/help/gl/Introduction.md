# Vespucci Introducción

Vespucci é un editor OpenStreetMap con tódalas funcións que admite a maioría das operacións que proporcionan os editores de escritorio. Foi probado con éxito en Googles Android 2.3 a 6.0 e varias variantes baseadas en AOSP. Unha palabra de precaución: mentres as capacidades dos dispositivos móbiles atópanse cos seus rivais de escritorio, especialmente os dispositivos máis antigos teñen memoria moi limitada dispoñible e adoitan ser bastante lentos. Ten que ter isto en conta ao usar Vespucci e manter, por exemplo, o tamaño das áreas que está editando a un tamaño razoable. 

## Primeira vez de uso

No inicio Vespucci mostra a caixa de diálogo "Descargar outra localización" / "Cargar área". Se tes as coordenadas que se amosan e queres descargar de xeito inmediato, podes seleccionar a opción adecuada e configurar o radio ao redor da localización que queres descargar. Non seleccione unha área grande en dispositivos lentos. 

Alternativamente, pode descartar o diálogo premendo o botón "Ir ao mapa" e xirar e ampliar a unha localización onde desexa editar e descargar os datos (ver a continuación: "Edición con Vespucci").

## Editando con Vespucci

Dependendo do tamaño da pantalla e da idade do seu dispositivo, as accións de edición poden ser accesibles directamente a través das iconas na barra superior, a través dun menú desplegable á dereita da barra superior, desde a barra inferior (se está presente) ou a través da tecla de menú.

### Descargando información OSM

Seleccione a icona de transferencia ![](../images/menu_transfer.png)  ou o elemento do menú "Transfer". Isto mostrará sete opcións:

* **Baixar vista actual** - baixar área visible na pantalla e substituír os datos existentes*(require conexión a internet)*
* **Engadir vista actual para descargar** - Descargue a área visible na pantalla e fúndea cos datos existentes *(require conexión a internet)*
* **Descarga outra localización** - Amosa un formulario que lle permite introducir coordenadas, buscar unha localización ou usar a posición actual e, a continuación, descargar unha área na contorna a esa localización *(require conexión a internet)*
* **Carga datos no servidor OSM** - sube edicións a OpenStreetMap *(require autentificación)* *(require conexión a internet)*
* **Auto baixada** - descargue automáticamente unha área ao redor da situación actual **(require conexión a internet)* *(require GPS)*
* **Arquivo...** - salvar e cargar información OSM a/dende os arquivos do dispositivo.
* **Nota/Bugs...** - baixar (automaticamente e manualmente) Notas OSM e "Bugs" dende QA ferramentas (actualmente OSMOSE) *(require conexión a internet)*

A forma máis sinxela de descargar datos ao dispositivo é achegar e panoramizar a localización que desexa editar e logo seleccionar "Descargar vista actual". Pode facer zoom usando xestos, os botóns de zoom ou os botóns de control de volume no teléfono. Vespucci debería entón descargar datos para a vista actual. Non se precisa autenticación para descargar datos no dispositivo.

### Editando

Para evitar edicións accidentais Vespucci comeza no modo "bloqueado", un modo que só permite achegar e mover o mapa. Toque a icona ![Locked](../images/locked.png) para desbloquear a pantalla. Unha pulsación longa na icona de bloqueo habilitará o modo "Edición de etiquetas só" que non permitirá crear novos obxectos ou editar a xeometría dos obxectos; este modo indícase cun icono de bloqueo branco un pouco diferente.

De xeito predeterminado, os nodos e os modos seleccionables teñen unha área en laranxa ao redor deles, indicando aproximadamente onde ten que tocar para seleccionar un obxecto. Se tenta seleccionar un obxecto e Vespucci determina que a selección podería significar varios obxectos, presentará un menú de selección. Os obxectos seleccionados están resaltados en amarelo.

Ista é unha boa estratexia facer zoom se ti tentas editar unha área de alta densidade.

Vespucci ten un bo sistema "desfacer/refacer" así que non teñas medo de experimentar no teu dispositivo, pero non cargues e gardes datos de proba ao chou.

#### Seleccionando / De-seleccionando

Toca un obxecto para seleccionar e resaltalo, un segundo toque no mesmo obxecto abre o editor de etiquetas no elemento. Ao tocar a pantalla nunha rexión baleira desmarcarase. Se seleccionou un obxecto e necesita seleccionar outra cousa, simplemente toque o obxecto en cuestión, non hai necesidade de desactivar primeiro. Aparecerá un toque dobre sobre un obxecto [Multiselect mode](../en/Multiselect.md).

#### Engadindo un nevo Nodo/Punto ou Vía

Manteña premido onde quere que o nodo sexa ou o xeito de comezar. Verás un símbolo de "cruzado negro". Ao tocar a mesma localización novamente créase un novo nodo, ao tocar unha localización fóra da zona de tolerancia táctil engadirá un segmento de modo desde a posición orixinal ata a posición actual. 

Simplemente toque a pantalla onde desexa engadir máis nodos do camiño. Para rematar, toque o nodo final dúas veces. Se os nodos inicial e final están localizados de algunha maneira, inserilos automaticamente.

#### Movendo un Nodo ou Vía

Os obxectos poden ser arrastrados/movidos só cando están seleccionados. Se selecciona a área de arrastre grande nas preferencias, obtén unha gran área ao redor do nodo seleccionado que facilita o posicionamento do obxecto. 

#### Mellorar a Xeometría do Camiño

Se acaba o zoom, verá un pequeno "x" no medio de segmentos que son o tempo suficiente. Arrastrando o "x" creará un nodo no camiño dese lugar. Nota: para evitar a creación de nodos accidentalmente, a tolerancia táctil para esta operación é bastante pequena.

#### Cortar, Copiar & Pegar

Pode copiar ou cortar nodos e xeitos seleccionados e, a continuación, pegar unha ou varias veces nunha nova ubicación. O corte conservará o id e versión osm. Para pegar prema a posición na que desexa pegar (verá un pelo cruzado marcando a localización). A continuación, selecciona "Pegar" no menú.

#### Eficiente Engadindo Enderezos

Vespucci ten unha función de "engadir etiquetas de enderezos" que intenta facer que os enderezos de topografía sexan máis eficientes. Pode ser seleccionado 

* despois dunha longa pulsación: Vespucci engadirá un nodo no lugar e fará unha mellor adiviñación no número da casa e engadirá as etiquetas de enderezos que estivo a usar recentemente. Se o nodo está nun contorno do edificio engadirá automaticamente un "entrance=yes" etiqueta ao nodo. O editor de etiquetas abrirase para o obxecto en cuestión e permítelle facer máis cambios.
* en modo nodos/via seleccionado: Vespucci engadirá as etiquetas de enderezo como se menciona arriba e iniciará o editor de etiquetas.
* no editor de etiquetas.

A predicción de números de casa normalmente require que polo menos dous números de casa a cada lado da estrada para ser ingresados ao traballo, cantos máis números presentes nos datos mellor.

Considere empregar esto con modo "Auto baixada"  

#### Engadindo Restriccións de Xiro

Vespucci ten un xeito rápido de engadir restricións de quenda. Nota: se precisa dividir un camiño para a restrición, cómpre facer isto antes de comezar.

* Seleccione un camiño cunha etiqueta de estrada (as restricións de volta só se poden engadir ás estradas, se ten que facelo por outras formas, use o modo xenérico de "crear relación", se non hai elementos "via" posibles, o elemento do menú non se amosan)
* seleccione "Engadir restricción" dende o menú
* seleccione a "via" nodo ou estrada (toda posible elemento "via" elements será seleccionable con alta luminosidade)
* seleccione o modo "a" (é posible dobrar e configurar o elemento "a" no elemento "dende", Vespucci suporá que está a engadir unha restrición sen restricións.)
* xogo de restricción tipo no menú etiquetas

### Vespucci  en modo "pechado"

Cando se amosa o bloqueo vermello, tódalas accións non editadas están dispoñibles. Adicionalmente, unha prensa longa ou próxima a un obxecto mostrará a pantalla de información detallada se é un obxecto OSM.

### Gardar os teus Trocos

*(require conectividade na rede)*

Seleccione o mesmo botón ou elemento de menú que fixeches para a descarga e agora seleccione "Cargar datos no servidor OSM".

Vespucci admite a autorización de OAuth e o método clásico de nome de usuario e contrasinal. OAuth é preferible xa que evita o envío de contrasinais.

As novas instalacións de Vespucci terán OAuth activado por defecto. No primeiro intento de cargar datos modificados, carga unha páxina do sitio web de OSM. Despois de iniciar sesión (a través dunha conexión cifrada) pediráselle que autorice a Vespucci a editar usando a súa conta. Se queres ou necesitas autorizar o acceso de OAuth á túa conta antes de editar hai un elemento correspondente no menú "Ferramentas".

Se desexa gardar o seu traballo e non ten acceso a Internet, pode gardar nun ficheiro .osm compatíbel con JOSM e cargar máis tarde con Vespucci ou con JOSM. 

#### Resolvendo conflitos en subidas

Vespucci ten un sinxelo resolvente de conflito. Non obstante, se sospeita que hai grandes problemas coas súas edicións, exporte os seus cambios a un ficheiro .osc (elemento do menú "Exportar" no menú "Transferir") e corrixa e cargue con JOSM. Vexa a axuda polo miudo en [conflict resolution](../en/Conflict resolution.md).  

## Usando GPS

Podes usar Vespucci para crear unha pista GPX e visualizalo no teu dispositivo. Ademais, pode mostrar a posición actual do GPS (configurar "Mostrar localización" no menú GPS) e/ou ter a pantalla en torno e seguir a posición (configure "Seguir posición GPS" no menú GPS). 

Se tes a configuración posterior, mover a pantalla manualmente ou editala fará que o modo "seguir o GPS" estea desactivado e que a frecha GPS azul cambie dun esquema a unha frecha chea. Para volver rapidamente ao modo "seguir", simplemente toque a frecha ou revise a opción do menú.

## Notas e Bugs

Vespucci permite descargar, comentar e pechar Notas OSM (anteriormente OSM Bugs) e a funcionalidade equivalente de "Bugs" producida pola [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Ambas teñen que ser descargadas de forma explícita ou pode usar a instalación de descarga automática para acceder aos elementos da súa área inmediata. Unha vez editado ou pechado, pode cargar o erro ou Nota inmediatamente ou cargar todo dunha soa vez.

No mapa, as notas e os erros están representados por un pequeno ícono de erro ![](../images/bug_open.png), Os verdes son pechados/resoltos, os que foron creados ou editados por ti en amarelo indica que aínda está activo e non se modificou. 

A visualización de erros de OSMOSE proporcionará unha ligazón ao obxecto afectado en azul, tocando a ligazón seleccionará o obxecto, centrará a pantalla nel e baixará a área previamente se fose necesario. 

## Persoalizando Vespucci

### Axustes que ti debes querer trocar

* Capa do fondo
* Capa de superposición. Engadindo unha superposición pode causar problemas con dispositivos máis antigos e con memoria limitada. Predeterminado: ningún.
* Notas/Bugs amosalos. Notas abertas e bugsMostrarase como un ícono de erro amarelo, pechados o mesmo en verde. Por omisión: on.
* Capa de fotos. Mostra as fotografías xeorreferenciadas como íconas da cámara vermella, se a información de dirección está dispoñible, a icona rotarase.Por omisión: off.
* Nodo iconas. Por omisión: on.
* Manter a pantalla accesa. Por omisión: off.
* Área grande de arrastre de nodo. Os nodos móbiles nun dispositivo con entrada táctil son problemáticos, xa que os dedos obscurecerán a posición actual na pantalla. Ao activar isto proporcionará unha gran área que pode usarse para arrastrar fóra de centro (a selección e outras operacións aínda usan a área de tolerancia táctil normal). Por omisión: off.

#### Preferencias Avanzadas

* Habilitar a barra de acción dividida. Nos últimos teléfonos, a barra de acción dividirase nunha parte superior e inferior, coa barra inferior que contén os botóns. Isto normalmente permite que se mostren máis botóns, pero usa máis da pantalla. Desactivando isto moverá os botóns á barra superior. Nota: necesitas reiniciar Vespucci para que o cambio teña efecto.
* Mostrar sempre o menú contextual. Cando estea activado, cada proceso de selección mostrará o menú contextual, apagado o menú só se amosará cando non se poida determinar ningunha selección sen ambigüidades. Predeterminado: desactivado (usado para estar activado).
* Activar tema lixeiro. En dispositivos modernos, este está activado por defecto. Mentres podes activalo para versións máis antigas de Android, o estilo probablemente non sexa coherente.
* Amosar estatísticas. Mostrará algunhas estatísticas para a depuración, non é realmente útil. Predeterminado: desactivado (usado para estar activado).  

## Reportando Problemas

Se Vespucci falla ou detecta un estado inestable, pediráselle que envíe ao vertedoiro. Faga isto se isto ocorre, pero por favor só unha vez por situación específica. Se desexa dar máis información ou abrir un problema para unha solicitude de funcións ou similar, faga isto aquí: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Se queres discutir algo relacionado con Vespucci, podes iniciar unha discusión sobre [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) ou en  [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


