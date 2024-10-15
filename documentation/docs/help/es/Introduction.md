_Antes de empezar: la mayoría de las pantallas tienen enlaces en el menú al sistema de ayuda del dispositivo que te dan acceso directo a la información relevante para el contexto actual; también puedes volver fácilmente a este texto. Si tienes un dispositivo más grande, como una tablet, puedes abrir el sistema de ayuda en una ventana dividida separada. Todos los textos de ayuda y más (preguntas frecuentes, tutoriales) también se pueden encontrar en el [sitio de documentación de Vespucci](https://vespucci.io/)._

# Introducción a Vespucci

Vespucci es un editor de OpenStreetMap con todas las funciones que admite la mayoría de las operaciones que ofrecen los editores de escritorio. Se ha probado con éxito en Android 2.3 a 10.0 de Google y en varias variantes basadas en AOSP. Una advertencia: si bien las capacidades de los dispositivos móviles se han puesto al día con sus rivales de escritorio, los dispositivos más antiguos, en particular, tienen una memoria muy limitada disponible y tienden a ser bastante lentos. Debes tener esto en cuenta al usar Vespucci y mantener, por ejemplo, las áreas que estás editando a un tamaño razonable.

## Editar con Vespucci

Dependiendo del tamaño de la pantalla y la antigüedad de tu dispositivo, puedes acceder a las acciones de edición directamente mediante los iconos de la barra superior, a través de un menú desplegable a la derecha de la barra superior, desde la barra inferior (si está presente) o mediante la tecla de menú.

<a id="download"></a>

### Descargar datos de OSM

Selecciona el ícono de transferencia ![Transferir](../images/menu_transfer.png) o el elemento del menú "Transferir". Esto mostrará siete opciones:

* **Descargar vista actual**: descarga el área visible en la pantalla y combínala con los datos existentes *(requiere conectividad de red o fuente de datos sin conexión)*.
* **Borrar y descargar vista actual**: borra cualquier dato en la memoria y luego descarga el área visible en la pantalla *(requiere conectividad de red)*.
* **Subir datos al servidor OSM**: sube las ediciones a OpenStreetMap *(requiere autenticación)* *(requiere conectividad de red)*.
* **Actualizar datos**: vuelve a descargar los datos de todas las áreas y actualiza lo que está en la memoria *(requiere conectividad de red)*.
* **Descarga automática basada en la ubicación**: descarga automáticamente un área alrededor de la ubicación geográfica actual *(requiere conectividad de red o datos sin conexión)* *(requiere GPS)*.
* **Descarga automática al desplazar y hacer zoom**: descarga automáticamente los datos del área del mapa que se muestra actualmente *(requiere conectividad de red o datos sin conexión)* *(requiere GPS)*.
* **Archivo...**: guardar y cargar datos OSM en/desde archivos del dispositivo.
* **Notas/Errores...**: descarga (automática y manualmente) notas de OSM y "errores" de las herramientas de control de calidad (actualmente OSMOSE) *(requiere conectividad de red)*.

La manera más fácil de descargar datos al dispositivo es hacer zoom y desplazarte hasta la ubicación que quieres editar y luego seleccionar "Descargar vista actual". Puedes hacer zoom utilizando gestos, los botones de zoom o los botones de control de volumen del dispositivo. Vespucci debería descargar entonces los datos de la vista actual. No se requiere autenticación para descargar datos a tu dispositivo.

Con la configuración predeterminada, cualquier área no descargada se atenuará en relación con las descargadas; esto es para evitar agregar objetos duplicados por error en áreas que no se están mostrando. El comportamiento se puede cambiar en las [Preferencias avanzadas](Advanced%20preferences.md).

### Editar

<a id="lock"></a>

#### Bloquear, desbloquear, cambio de modo

Para evitar ediciones accidentales, Vespucci inicia en modo "bloqueado", un modo que solo permite hacer zoom y mover el mapa. Toca el ícono ![Bloqueado](../images/locked.png) para desbloquear la pantalla. 

Una pulsación larga en el ícono de bloqueo mostrará un menú que actualmente ofrece 5 opciones:

* **Normal**: el modo de edición predeterminado; se pueden agregar objetos nuevos, editar, mover y eliminar los existentes. Se muestra un ícono de candado blanco simple.
* **Solo etiquetas**: al seleccionar un objeto existente se iniciará el Editor de propiedades; una pulsación larga en la pantalla principal agregará objetos, pero ninguna otra operación de geometría funcionará. Se muestra un ícono de candado blanco con una "T".
* **Dirección**: activa el modo Dirección, un modo ligeramente simplificado con acciones específicas disponibles desde el botón "+" del [modo simple](../en/Simple%20actions.md). Se muestra un ícono de candado blanco con una "A".
* **Interior**: activa el modo Interior; consulta [Modo Interior](#indoor). Se muestra un ícono de candado blanco con una "I".
* **Modo C**: activa el Modo C, solo se mostrarán los objetos que tengan un indicador de advertencia establecido; consulta [Modo C](#c-mode). Se muestra un ícono de candado blanco con una "C".

#### Toque simple, toque doble y pulsación larga

De forma predeterminada, los nodos y vías seleccionables tienen un área naranja a su alrededor que indica aproximadamente dónde debes tocar para seleccionar un objeto. Tienes tres opciones:

* Toque simple: selecciona el objeto.
* Un nodo/vía aislado se resalta inmediatamente.
* Sin embargo, si intentas seleccionar un objeto y Vespucci determina que la selección podría referirse a varios objetos, presentará un menú de selección que te permitirá elegir el objeto que deseas seleccionar.
* Los objetos seleccionados se resaltan en amarillo.
* Para obtener más información, consulta [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada](Relation%20selected.md).
* Toque doble: inicia el [modo de selección múltiple](Multiselect.md).
* Pulsación larga: crea una "cruz", que te permite agregar nodos; consulta a continuación y [Crear nuevos objetos](Creating%20new%20objects.md). Esto solo está habilitado si el "Modo simple" está desactivado.

Es una buena estrategia acercar el zoom si intentas editar un área de alta densidad.

Vespucci tiene un buen sistema de "deshacer/rehacer", así que no tengas miedo de experimentar en tu dispositivo. Sin embargo, no subas ni guardes datos de prueba puros.

#### Seleccionar / Deseleccionar (toque simple y "menú de selección")

Toca un objeto para seleccionarlo y resaltarlo. Tocar la pantalla en una región vacía anulará la selección. Si has seleccionado un objeto y necesitas seleccionar otro, simplemente toca el objeto en cuestión; no es necesario deseleccionar primero. Un doble toque en un objeto iniciará el [modo de selección múltiple](Multiselect.md).

Ten en cuenta que si intentas seleccionar un objeto y Vespucci determina que la selección podría referirse a varios objetos (como un nodo en una vía u otros objetos que se superponen), presentará un menú de selección: toca el objeto que deseas seleccionar y el objeto se seleccionará. 

Los objetos seleccionados se indican mediante un borde amarillo delgado. El borde amarillo puede ser difícil de detectar, dependiendo del fondo del mapa y el factor de zoom. Una vez realizada una selección, verás una notificación que confirma la selección.

Una vez que se haya completado la selección, verás (ya sea como botones o como elementos del menú) una lista de operaciones compatibles para el objeto seleccionado. Para obtener más información, consulta [Nodo seleccionado](Node%20selected.md), [Vía seleccionada](Way%20selected.md) y [Relación seleccionada](Relation%20selected.md).

#### Objetos seleccionados: Editar etiquetas

Un segundo toque en el objeto seleccionado abre el editor de etiquetas y puedes editar las etiquetas asociadas al objeto.

Ten en cuenta que para los objetos que se superponen (como un nodo en una vía), el menú de selección vuelve a aparecer por segunda vez. Seleccionar el mismo objeto abre el editor de etiquetas; seleccionar otro objeto simplemente selecciona el otro objeto.

#### Objetos seleccionados: Mover un nodo o vía

Una vez que hayas seleccionado un objeto, puedes moverlo. Ten en cuenta que los objetos solo se pueden arrastrar/mover cuando están seleccionados. Simplemente arrastra cerca (es decir, dentro de la zona de tolerancia) del objeto seleccionado para moverlo. Si seleccionas el área de arrastre grande en las [preferencias](Preferences.md), obtendrás un área grande alrededor del nodo seleccionado que facilita la colocación del objeto. 

#### Agregar un nuevo nodo/punto o vía 

Al iniciar la aplicación por primera vez, se inicia en "Modo simple". Esto se puede cambiar en el menú principal desmarcando la casilla correspondiente.

##### Modo simple

Al tocar el botón flotante verde grande en la pantalla del mapa, se mostrará un menú. Después de que hayas seleccionado uno de los elementos, se te pedirá que toques la pantalla en la ubicación donde deseas crear el objeto. Desplazar y hacer zoom continúa funcionando si necesitas ajustar la vista del mapa. 

Consulta [Creación de nuevos objetos en modo de acciones simples](Creating%20new%20objects%20in%20simple%20actions%20mode.md) para obtener más información.

##### Modo avanzado (pulsación larga)

Mantén pulsado donde quieras que esté el nodo o donde quieras que comience la vía. Verás un símbolo de "cruz" negra.
* Si quieres crear un nuevo nodo (no conectado a un objeto), toca lejos de los objetos existentes.
* Si quieres extender una vía, toca dentro de la "zona de tolerancia" de la vía (o un nodo en la vía). La zona de tolerancia se indica mediante las áreas alrededor de un nodo o vía.

Una vez que veas el símbolo de la cruz, tienes estas opciones:

* *Pulsación normal en el mismo lugar*.
* Si la cruz no está cerca de un nodo, tocar la misma ubicación nuevamente crea un nuevo nodo. Si estás cerca de una vía (pero no cerca de un nodo), el nuevo nodo estará en la vía (y conectado a la vía).
* Si la cruz está cerca de un nodo (es decir, dentro de la zona de tolerancia del nodo), tocar la misma ubicación simplemente selecciona el nodo (y se abre el editor de etiquetas). No se crea ningún nodo nuevo. La acción es la misma que la selección anterior.
* *Toque normal en otro lugar*. Tocar otra ubicación (fuera de la zona de tolerancia de la cruz) agrega un segmento de vía desde la posición original a la posición actual. Si la cruz estaba cerca de una vía o nodo, el nuevo segmento se conectará a ese nodo o vía.

Simplemente toca la pantalla donde quieras agregar más nodos de la vía. Para terminar, toca el nodo final dos veces. Si el nodo final se encuentra en una vía o nodo, el segmento se conectará automáticamente a la vía o nodo. 

También puedes usar un elemento de menú: consulta [Crear nuevos objetos](Creating%20new%20objects.md) para obtener más información.

#### Agregar un área

OpenStreetMap actualmente no tiene un tipo de objeto "área" a diferencia de otros sistemas de geodatos. El editor en línea "iD" intenta crear una abstracción de área a partir de los elementos OSM subyacentes, lo que funciona bien en algunas circunstancias y no tan bien en otras. Actualmente, Vespucci no intenta hacer nada similar, por lo que necesitas saber un poco sobre cómo se representan las áreas:

* _vías cerradas (polígonos)_: la variante de área más simple y común son las vías que comparten un primer y último nodo, formando un "anillo" cerrado (por ejemplo, la mayoría de los edificios son de este tipo). Son muy fáciles de crear en Vespucci, simplemente conéctate de nuevo al primer nodo cuando hayas terminado de dibujar el área. Nota: la interpretación de la vía cerrada depende de su etiquetado: por ejemplo, si una vía cerrada está etiquetada como un edificio, se considerará un área; si está etiquetada como una rotonda, no. En algunas situaciones en las que ambas interpretaciones pueden ser válidas, una etiqueta de "área" puede aclarar el uso previsto.
* _multipolígonos_: algunas áreas tienen varias partes, agujeros y anillos que no se pueden representar con una sola vía. OSM utiliza un tipo específico de relación (nuestro objeto de propósito general que puede modelar relaciones entre elementos) para solucionar esto: un multipolígono. Un multipolígono puede tener varios anillos "externos" y varios anillos "internos". Cada anillo puede ser una vía cerrada como se describe anteriormente o varias vías individuales que tienen nodos finales comunes. Si bien los multipolígonos grandes son difíciles de manejar con cualquier herramienta, los pequeños no son difíciles de crear en Vespucci.
* _líneas costeras_: para objetos muy grandes, continentes e islas, incluso el modelo de multipolígono no funciona de manera satisfactoria. Para las vías natural=línea costera, asumimos una semántica dependiente de la dirección: la tierra está en el lado izquierdo de la vía, el agua en el lado derecho. Un efecto secundario de esto es que, en general, no debes invertir la dirección de una vía con etiquetado de línea costera. Puedes encontrar más información en la [wiki de OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Mejorar la geometría de la vía

Si acercas el zoom lo suficiente en una vía seleccionada, verás una pequeña "x" en el medio de los segmentos de la vía que son lo suficientemente largos. Arrastrar la "x" creará un nodo en la vía en esa ubicación. Nota: para evitar la creación accidental de nodos, el área de tolerancia táctil para esta operación es bastante pequeña.

#### Cortar, copiar y pegar

Puedes copiar o cortar los nodos y vías seleccionados y luego pegarlos una o varias veces en una nueva ubicación. Cortar conservará el ID y la versión de OSM. Para pegar, mantén pulsada la ubicación donde quieres pegar (verás una cruz que marca la ubicación). Luego selecciona "Pegar" en el menú.

#### Agregar direcciones de forma eficiente

Vespucci admite funciones que hacen que el levantamiento de direcciones sea más eficiente al predecir los números de casas (lados izquierdo y derecho de las calles por separado) y agregar automáticamente etiquetas _addr:street_ o _addr:place_ según el último valor utilizado y la proximidad. En el mejor de los casos, esto permite agregar una dirección sin necesidad de escribir nada.   

Puedes activar la adición de etiquetas pulsando ![Dirección](../images/address.png): 

* después de una pulsación larga (solo en modo no simple): Vespucci agregará un nodo en la ubicación, hará una mejor estimación del número de casa y agregará etiquetas de dirección que hayas estado usando últimamente. Si el nodo está en el contorno de un edificio, agregará automáticamente una etiqueta "entrance=yes" al nodo. El editor de etiquetas se abrirá para el objeto en cuestión y te permitirá realizar los cambios adicionales necesarios.
* en los modos de nodo/vía seleccionados: Vespucci agregará etiquetas de dirección como se indicó anteriormente e iniciará el editor de etiquetas.
* en el editor de propiedades.

Para agregar nodos de dirección individuales directamente mientras estás en el "Modo simple" predeterminado, cambia al modo de edición "Dirección" (pulsación larga en el botón de bloqueo). "Agregar nodo de dirección" agregará un nodo de dirección en la ubicación y, si está en el contorno de un edificio, le agregará una etiqueta de entrada como se describe arriba.

La predicción del número de casa generalmente requiere que se ingresen al menos dos números de casa a cada lado de la carretera para que funcione; cuantos más números haya en los datos, mejor.

Considera usar esto con uno de los modos de [Descarga automática](#download).  

#### Agregar restricciones de giro

Vespucci tiene una forma rápida de agregar restricciones de giro. Si es necesario, dividirá las vías automáticamente y te pedirá que vuelvas a seleccionar los elementos. 

* Selecciona una vía con una etiqueta highway (las restricciones de giro solo se pueden agregar a carreteras; si necesitas hacer esto para otras vías, utiliza el modo genérico "crear relación").
* Selecciona "Agregar restricción" en el menú.
* Selecciona el nodo o vía "vía" (solo los elementos "vía" posibles mostrarán el área táctil).
* Selecciona la vía "a" (es posible retroceder y configurar el elemento "a" en el elemento "desde"; Vespucci asumirá que estás agregando una restricción no_u_turn).
* Establece el tipo de restricción.

### Vespucci en modo "bloqueado"

Cuando se muestra el candado rojo, todas las acciones que no son de edición están disponibles. Además, una pulsación larga en o cerca de un objeto mostrará la pantalla de información detallada si se trata de un objeto OSM.

### Guardar los cambios

*(requiere conectividad de red)*

Selecciona el mismo botón o elemento de menú que usaste para la descarga y ahora selecciona "Subir datos al servidor OSM".

Vespucci admite la autorización OAuth y el método clásico de nombre de usuario y contraseña. OAuth es preferible, ya que evita el envío de contraseñas en texto sin formato.

Las nuevas instalaciones de Vespucci tendrán OAuth habilitado de forma predeterminada. En tu primer intento de subir datos modificados, se carga una página del sitio web de OSM. Después de que hayas iniciado sesión (a través de una conexión cifrada), se te pedirá que autorices a Vespucci a editar con tu cuenta. Si quieres o necesitas autorizar el acceso OAuth a tu cuenta antes de editar, hay un elemento correspondiente en el menú "Herramientas".

Si quieres guardar tu trabajo y no tienes acceso a Internet, puedes guardarlo en un archivo .osm compatible con JOSM y subirlo más tarde con Vespucci o con JOSM. 

#### Resolver conflictos en las subidas

Vespucci tiene un solucionador de conflictos simple. Sin embargo, si sospechas que hay problemas importantes con tus ediciones, exporta los cambios a un archivo .osc (elemento del menú "Exportar" en el menú "Transferir"), corrígelos y súbelos con JOSM. Consulta la ayuda detallada sobre [resolución de conflictos](Conflict%20resolution.md).  

## Usar GPS y pistas GPX

Con la configuración estándar, Vespucci intentará habilitar el GPS (y otros sistemas de navegación basados en satélites) y recurrirá a determinar la posición mediante la llamada "ubicación de red" si esto no es posible. Este comportamiento asume que, en el uso normal, tienes tu dispositivo Android configurado para usar solo ubicaciones generadas por GPX (para evitar el seguimiento), es decir, que tienes desactivada la opción eufemísticamente llamada "Mejorar la precisión de la ubicación". Si quieres habilitar la opción pero quieres evitar que Vespucci recurra a la "ubicación de red", debes desactivar la opción correspondiente en las [Preferencias avanzadas](Advanced%20preferences.md). 

Al tocar el botón ![GPS](../images/menu_gps.png) (en el lado izquierdo de la pantalla del mapa), la pantalla se centrará en la posición actual y, a medida que te muevas, la pantalla del mapa se ajustará para mantener esto. Mover la pantalla manualmente o editar hará que el modo "seguir GPS" se desactive y la flecha azul del GPS cambiará de un contorno a una flecha rellena. Para volver rápidamente al modo "seguir", simplemente toca el botón GPS o vuelve a marcar la opción de menú equivalente. Si el dispositivo no tiene una ubicación actual, el marcador/flecha de ubicación se mostrará en negro; si hay una ubicación actual disponible, el marcador será azul.

Para grabar una pista GPX y mostrarla en tu dispositivo, selecciona la opción "Iniciar pista GPX" en el menú ![GPS](../images/menu_gps.png). Esto agregará una capa a la pantalla con la pista grabada actual; puedes subir y exportar la pista desde la entrada en el [control de capas](Main%20map%20display.md). Se pueden agregar más capas desde archivos GPX locales y pistas descargadas de la API de OSM.

Nota: De forma predeterminada, Vespucci no grabará datos de elevación con tu pista GPX debido a algunos problemas específicos de Android. Para habilitar la grabación de elevación, instala un modelo gravitacional o, más sencillo, ve a las [Preferencias avanzadas](Advanced%20preferences.md) y configura la entrada NMEA.

## Notas y errores

Vespucci admite la descarga, los comentarios y el cierre de notas de OSM (anteriormente errores de OSM) y la funcionalidad equivalente para los "errores" producidos por la [herramienta de control de calidad OSMOSE](http://osmose.openstreetmap.fr/en/map/). Ambos tienen que descargarse explícitamente o puedes usar la función de descarga automática para acceder a los elementos en tu área inmediata. Una vez editados o cerrados, puedes subir el error o la nota inmediatamente o subir todo a la vez.

En el mapa, las notas y los errores se representan con un pequeño icono de error ![Error](../images/bug_open.png): los verdes están cerrados/resueltos, los azules los has creado o editado tú y el amarillo indica que todavía está activo y no se ha modificado. 

La visualización de errores de OSMOSE proporcionará un enlace al objeto afectado en azul; al tocar el enlace, se seleccionará el objeto, se centrará la pantalla en él y se descargará el área de antemano si es necesario. 

### Filtrado

Además de habilitar globalmente la visualización de notas y errores, puedes configurar un filtro de visualización de grano grueso para reducir el desorden. Se puede acceder a la configuración del filtro desde la entrada de la capa de tareas en el [control de capas](#layers):

* Notas
* Error de Osmose
* Advertencia de Osmose
* Problema menor de Osmose
* Maproulette
* Personalizado

<a id="indoor"></a>

## Modo de interiores

Mapear en interiores es un desafío debido a la gran cantidad de objetos que, con mucha frecuencia, se superpondrán entre sí. Vespucci tiene un modo interior dedicado que te permite filtrar todos los objetos que no están en el mismo nivel y que agregará automáticamente el nivel actual a los nuevos objetos que se creen allí.

Puedes habilitar el modo manteniendo pulsado el elemento de bloqueo; consulta [Bloquear, desbloquear, cambio de modo](#lock) y seleccionando la entrada de menú correspondiente.

<a id="c-mode"></a>

## Modo-C

En el Modo C, solo se muestran los objetos que tienen un indicador de advertencia establecido; esto facilita la detección de objetos que tienen problemas específicos o coinciden con comprobaciones configurables. Si se selecciona un objeto y se inicia el Editor de propiedades en Modo C, se aplicará automáticamente el preajuste que mejor coincida.

Puedes habilitar el modo manteniendo pulsado el elemento de bloqueo; consulta [Bloquear, desbloquear, cambio de modo](#lock) y seleccionando la entrada de menú correspondiente.

### Configurar comprobaciones

Actualmente hay dos comprobaciones configurables (hay una comprobación para las etiquetas FIXME y una prueba para las etiquetas de tipo faltantes en las relaciones que actualmente no son configurables); ambas se pueden configurar seleccionando "Configuración del validador" en las [preferencias](Preferences.md). 

La lista de entradas se divide en dos: la mitad superior muestra las entradas de "revisión", la mitad inferior las entradas de "comprobación". Las entradas se pueden editar haciendo clic en ellas; el botón verde del menú permite agregar entradas.

#### Entradas de revisión

Las entradas de revisión tienen las siguientes propiedades:

* **Clave**: clave de la etiqueta de interés.
* **Valor**: valor que debe tener la etiqueta de interés; si está vacío, se ignorará el valor de la etiqueta.
* **Antigüedad**: cuántos días después de la última modificación del elemento se debe revisar el elemento. Si hay una etiqueta _check_date_ presente, se utilizará; de lo contrario, se utilizará la fecha en que se creó la versión actual. Si se establece el valor en cero, la comprobación simplemente coincidirá con la clave y el valor.
* **Expresión regular**: si está marcada, se asume que **Valor** es una expresión regular de JAVA.

**Clave** y **Valor** se comparan con las etiquetas _existentes_ del objeto en cuestión.

El grupo _Anotaciones_ en los preajustes estándar contiene un elemento que agregará automáticamente una etiqueta _check_date_ con la fecha actual.

#### Entradas de comprobación

Las entradas de comprobación tienen las siguientes dos propiedades:

* **Clave**: clave que debe estar presente en el objeto según el preajuste coincidente.
* **Requerir opcional**: requiere la clave incluso si la clave está en las etiquetas opcionales del preajuste coincidente.

Esta comprobación funciona determinando primero el preajuste coincidente y luego comprobando si **Clave** es una clave "recomendada" para este objeto según el preajuste. **Requerir opcional** ampliará la comprobación a las etiquetas que son "opcionales" en el objeto. Nota: actualmente no se comprueban los preajustes enlazados.

## Filtros

### Filtro basado en etiquetas

El filtro se puede habilitar desde el menú principal; luego se puede cambiar tocando el icono del filtro. Puedes encontrar más documentación aquí: [Filtro de etiquetas](Tag%20filter.md).

### Filtro basado en preajustes

Como alternativa a lo anterior, los objetos se filtran por preajustes individuales o por grupos de preajustes. Al tocar el icono del filtro, se mostrará un cuadro de diálogo de selección de preajustes similar al utilizado en otras partes de Vespucci. Se pueden seleccionar preajustes individuales con un clic normal, grupos de preajustes con un clic largo (un clic normal entra en el grupo). Puedes encontrar más documentación aquí: [Filtro de preajustes](Preset%20filter.md).

## Personalizar Vespucci

Muchos aspectos de la app se pueden personalizar; si buscas algo específico y no lo encuentras, el [sitio web de Vespucci](https://vespucci.io/) se puede buscar y contiene información adicional sobre lo que está disponible en el dispositivo.

<a id="layers"></a>

### Ajustes de capa

Puedes cambiar los ajustes de capa a través del control de capas (menú de "hamburguesa" en la esquina superior derecha); todos los demás ajustes se pueden acceder a través del botón de preferencias del menú principal. Se pueden habilitar, deshabilitar y ocultar temporalmente las capas.

Tipos de capas disponibles:

* Capa de datos: esta es la capa en la que se cargan los datos de OpenStreetMap. En el uso normal, no necesitas cambiar nada aquí. Predeterminado: activado.
* Capa de fondo: hay una amplia gama de imágenes aéreas y satelitales de fondo disponibles. El valor predeterminado para esto es el mapa de "estilo estándar" de openstreetmap.org.
* Capa de superposición: estas son capas semitransparentes con información adicional, por ejemplo, pistas GPX. Agregar una superposición puede causar problemas con dispositivos más antiguos y con memoria limitada. Predeterminado: ninguno.
* Visualización de notas/errores: las notas y los errores abiertos se mostrarán como un icono de error amarillo, los cerrados igual pero en verde. Predeterminado: activado.
* Capa de fotos: muestra fotografías georreferenciadas como iconos de cámara rojos; si hay información de dirección disponible, el icono girará. Predeterminado: desactivado.
* Capa de Mapillary: muestra segmentos de Mapillary con marcadores donde existen imágenes; al hacer clic en un marcador, se mostrará la imagen. Predeterminado: desactivado.
* Capa GeoJSON: muestra el contenido de un archivo GeoJSON. Predeterminado: desactivado.
* Cuadrícula: muestra una escala a los lados del mapa o una cuadrícula. Predeterminado: activado. 

Puedes encontrar más información en la sección sobre la [visualización del mapa](Main%20map%20display.md).

#### Preferencias

* Mantener la pantalla encendida. Predeterminado: desactivado.
* Área de arrastre de nodo grande. Mover nodos en un dispositivo con entrada táctil es problemático, ya que tus dedos oscurecerán la posición actual en la pantalla. Activar esto proporcionará un área grande que se puede usar para arrastrar fuera del centro (la selección y otras operaciones aún utilizan el área de tolerancia táctil normal). Predeterminado: desactivado.

Puedes encontrar la descripción completa aquí: [Preferencias](Preferences.md).

#### Preferencias avanzadas

* Iconos de nodo. Predeterminado: activado.
* Mostrar siempre el menú contextual. Cuando está activada, cada proceso de selección mostrará el menú contextual; cuando está desactivada, el menú solo se muestra cuando no se puede determinar una selección inequívoca. Predeterminado: desactivado (solía estar activado).
* Habilitar tema claro. En los dispositivos modernos, esto está activado de forma predeterminada. Si bien puedes habilitarlo para versiones anteriores de Android, es probable que el estilo sea inconsistente. 

Puedes encontrar la descripción completa aquí: [Preferencias avanzadas](Advanced%20preferences.md).

## Informar de problemas

Si Vespucci falla o detecta un estado inconsistente, se te pedirá que envíes el volcado de memoria. Hazlo si eso sucede, pero solo una vez por situación específica. Si quieres proporcionar más información o abrir un problema para una solicitud de función o similar, hazlo aquí: [Rastreador de problemas de Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). La función "Enviar comentarios" del menú principal abrirá un nuevo problema e incluirá la información relevante de la aplicación y el dispositivo sin necesidad de escribir más.

Si quieres debatir algo relacionado con Vespucci, puedes iniciar una discusión en el [grupo de Google de Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) o en el [foro de OpenStreetMap Android](http://forum.openstreetmap.org/viewforum.php?id=56).


