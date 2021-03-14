# Introduzione a Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size. 

## Primo utilizzo

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Altrimenti puoi chiudere la schermata premendo il pulsante "Vai alla mappa". Puoi quindi spostare la mappa, ingrandire sull'area che vuoi modificare e scaricarne i dati (vedi più avanti: "Modificare con Vespucci").

## Mappare con Vespucci

A seconda della dimensione dello schermo e da quanto è vecchio il tuo dispositivo, le diverse azioni saranno accessibili direttamente attraverso le icone sulla barra in alto, nel menù a tendina sulla destra della barra in alto, sulla barra in basso (se presente) oppure attraverso il pulsante menù.

<a id="download"></a>

### Download dati OSM

Seleziona l'icona di trasferimento ![Transfer](../images/menu_transfer.png) oppure l'opzione "Trasferimento" nel menu. Verranno mostrate sette opzioni:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Il modo più semplice di scaricare i dati sul dispositivo è di spostarsi e ingrandire sull'area che si vuole modificare e quindi selezionare "Scarica vista corrente". Puoi modificare l'ingrandimento usando le dita, i pulsanti di zoom oppure i pulsanti per il controllo del volume del dispositivo. Vespucci a questo punto scaricherà i dati della vista corrente. Per scaricare i dati sul dispositivo non è necessario essere l'autenticazione.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Mappare

<a id="lock"></a>

#### Blocca, sblocca, cambio modalità

Per evitare modifiche accidentali, Vespucci viene eseguito in modalità "bloccato", la quale consente di utilizzare solamente le funzioni di zoom e movimento della mappa. Premi  ![Bloccato](../images/locked.png) per passare alla modalità modifica. 

Una lunga pressione sull'icona di blocco farà apparire un menù con 4 opzioni:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Tocco singolo, doppio tocco, pressione prolungata

Di defualt, gli oggetti selezionabili e le linee sono bordate in arancione, con l'obiettivo di indicare dove premere per selezionarle. Ci sono 3 possibilità:

* Tocco singolo: seleziona oggetto. 
    * Un nodo isolato viene selezionato immediatamente. 
    * Tuttavia, se selezioni un oggetto e Vespucci ritiene che la selezione può comprendere più oggetto, presenterà un menu di selezione, permettendoti di scegliere l'oggetto che vuoi selezionare. 
    * Seleziona gli oggetti evidenziati in giallo. 
    * Per maggiori informazioni vedi [Nodo selezionato](Node%20selected.md), [Strada selezionata](Way%20selected.md) e [Relazione selezionata](Relation%20selected.md).
* Doppio tocco: Apri [modalità multiselezione](Multiselect.md)
* Tocco prlungato: crea un "bersagio" che ti permette di aggiungere note, vedere sotto e [Creare nuovi oggetti](Creating%20new%20objects.md). Questo è abilitato se "Modalità semplice" p disattivata.

Se si cerca di modificare un'area ad alta densità di oggetti, è buona norma ingrandire la mappa.

Vespucci ha un buon sistema "annulla/ripeti" quindi non temere di sperimentare con il tuo dispositivo, tuttavia sei pregato di non caricare sui server dati inventati di sana pianta.

#### Selezione / De-selezione (tocco singolo e "menu selezione")

Tocca un oggetto per selezionarlo ed evidenziarlo. Toccando lo schermo su una regione vuota deselezionerà l'oggetto. Se hai selezionato un oggetto e devi selezionare qualcos'altro, semplicemente tocca l'oggetto in questione, non è necessario deselezionarlo prima. Un doppio tocco su un oggetto aprirà la [Modalità di selezione](Multiselect.md).

Tieni presente che se cerchi di selezionare un oggetto e Vespucci rileva più oggetti in quel punto (per esempio un nodo su un percorso o altri oggetti sovrapposti) verrà mostrato un menù di selezione: premi sull’oggetto che vuoi selezionare ed esso verrà selezionato. 

Gli oggetti selezionati sono indicati da una sottile bordatura gialla. La bordatura potrebbe essere difficile da vedere, ma una volta selezionato l'oggetto, apparirà una notifica a conferma.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Oggetto selezionato: modifica etichette

Un secondo tocco sull'oggetto selezionato apre il menù di modifica delle etichette, dal quale è possibile modificare le etichette associate all'oggetto.

Tieni presente che per gli oggetti sovrapposti (come per esempio un nodo su un percorso) il menù di selezione comparirà una seconda volta. Se selezioni lo stesso oggetto, ti verrà mostrato l’editor delle etichette; se ne selezioni un altro, esso verrà semplicemente selezionato.

#### Oggetto selezionato: spostare un nodo o una linea

Una volta selezionato un oggetto, lo si può spostare. Un oggetto può essere trascinato/spostato solo dopo essere stato selezionato. Per spostarlo devi premere sull'oggetto selezionato e trascinare fino dove si desidera spostarlo. Dal menù opzioni è possibile ingrandire l'area di selezione che compare a fianco degli oggetti selezionati. 

#### Aggiungere nuovi Nodi/Punti o percorsi 

Al primo avvio l'app parte in "Modalità semplice". Questo può essere modificato ne menu principale togliendo la spunta al box corrispondente.

##### Simple mode

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Quando è visibile il simbolo a forma di croce, hai le seguenti opzioni:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Aggiungere un'area

Attualmente OpenStreetMap, a differenza di altri sistemi di dati geografici, non è provvisto di oggetti di tipo "area". L’editor online "iD" cerca di creare un modello di area utilizzando i sottostanti elementi di OSM che però non funziona sempre bene. Vespucci al momento non fa nulla di tutto ciò, quindi è necessario che tu sappia come i percorsi di tipo area sono rappresentati:

* _percorsi chiusi (*poligoni")_: il tipo di area più comune, sono i percorsi in cui il primo e l’ultimo nodo coincidono formando in tal modo un anello chiuso (ad esempio molti edifici sono rappresentati così). Sono molto facili da creare con Vespucci, basta ricollegare il percorso al primo nodo quando si è finito di disegnare l’area. Nota: l’interpretazione di un’area chiusa dipende dalle etichette usate: per esempio se un’area chiusa è mappata come edificio, essa viene considerata come area ma se invece venisse mappata come rotatoria ciò non accadrebbe. In alcune situazioni in cui entrambe le interpretazioni sono possibili, l’etichetta “area” può chiarire l’uso corretto.
* _multi-poligoni_: alcune aree sono formate da più pezzi, buchi al loro interno o anelli che non possono essere rappresentati con un solo percorso. OSM a questo scopo usa un tipo specifico di relazione (il nostro oggetto generico che può rappresentare le relazioni tra vari elementi), il multi-poligono. Un multi-poligono può avere diversi anelli “esterni” e diversi anelli “interni”. Ogni anello può sia essere un percorso chiuso sia un insieme di percorsi lineari aventi in comune gli estremi. La gestione di multi-poligoni grandi risulta essere difficile anche con altri strumenti, ma con Vespucci è relativamente semplice creare piccoli multi-poligoni. 
* _Linee costiere_: per oggetti molto grandi, tipo continenti e isole, anche il modello del multi-poligono non funziona in modo soddisfacente. Per i percorsi etichettati con ‘natural=coastline’ prendiamo in considerazione la direzione della linea: la terraferma si trova sempre a sinistra del percorso, l’acqua a destra. Come conseguenza, non si dovrebbe mai invertire la direzione di una linea costiera. Ulteriori informazioni sono disponibili nella [Wiki OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Migliorare la forma di un percorso

Se ingrandisci la mappa su di un percorso vedrai delle piccole “x” nel mezzo dei segmenti non troppo corti della linea. Se trascini una di queste “x” verrà creato un nodo in quel punto. Nota: per evitare la creazione accidentale dei nodi, la zona di tolleranza per questo tipo di operazione è piuttosto piccola.

#### Tagliare, copiare e incollare

Puoi copiare o tagliare dei nodi o delle vie selezionati per poi incollare una o più volte in un nuovo punto. Quando si taglia la versione e l'id OSM rimarranno gli stessi. Per incollare, tieni premuto il punto in cui vuoi incollare (sarà visibile il simbolo di una croce nel punto). Poi seleziona "Incolla" nel menù.

#### Aggiungere indirizzi in modo efficiente

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

La predizione dei numeri civici per funzionare richiede di solito che siano già stati inseriti due numeri civici per ogni lato della strada, più sono i civici inseriti meglio esso funzionerà.

Valuta di utilizzare la modalità di download automatico  

#### Aggiungere divieti di accesso

Vespucci consente di aggiungere velocemente le restrizioni di svolta. Se necessario, divide automaticamente le linee e chiede di selezionare nuovamente gli elementi oggetto della restrizione 

* seleziona un percorso con l’etichetta ‘highway’ (i divieti di svolta possono essere aggiunti solo alle strade, se hai la necessità di usarli su altri tipi di percorso usa la modalità generica “crea relazione”)
* seleziona “Aggiungi relazione” dal menù
* seleziona il nodo o il percorso “via”  (solo gli elementi compatibili con il ruolo “via” avranno l’area di touch visibile)
* seleziona il percorso “verso” (se si preme due volte il tasto indietro i ruoli “da” e “verso” verranno aggiunti allo stesso elemento e Vespucci assumerà che stai mappando un divieto di inversione di marcia)
* imposta il tipo di restrizione

### Vespucci in modalità "blocco"

Quando è mostrato il lucchetto rosso, sono disponibili tutte le azioni che non modificano gli oggetti. Inoltre, se si preme a lungo su un oggetto OSM verranno mostrate le sue informazioni dettagliate.

### Salvare le tue modifiche

*(è richiesta una connessione di rete)*

Usa lo stesso pulsante o elemento del menù che hai usato per scaricare i dati e seleziona "Carica i dati nel server OSM"

Vespucci è compatibile sia con l'autorizzazione OAuth che con il metodo username e password. È meglio usare l'autorizzazione OAuth visto che evita di comunicare le password in chiaro.

Le nuove installazioni di Vespucci hanno l'autorizzazione OAuth abilitata. Al primo tentativo di caricare nuovi dati, verrà caricata una pagina del sito OSM. Dopo essersi autenticati (attraverso una connessione cifrata) verrà richiesto di autorizzare Vespucci modificare a nome tuo.  Se vuoi o senti il bisogno di autorizzare l'accesso OAuth al tuo account prima di iniziare a modificare c'è un apposita opzione nel menù "Strumenti".

Se vuoi salvare il tuo lavoro quando non hai accesso a Internet, puoi salvarlo su un file .osm compatibile con JOSM e in seguito caricarlo con Vespucci o con JOSM. 

#### Risolvere in conflitti durante il caricamento

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Usare un GPS

Puoi usare Vespucci per creare delle tracce GPX da mostrare sul tuo dispositivo. Puoi inoltre mostrare la posizione GPS attuale (imposta "Mostra posizione" nel menù GPS) e/o impostare il centro dello schermo sulla posizione man mano che questa si aggiorna (imposta "Segui posizione GPS" nel menù GPS). 

Quando quest’ultima è abilitata, muovendo lo schermo manualmente o iniziando a modificare disabiliterà la modalità “segui GPS” e il simbolo della freccia blu del GPS passerà da trasparente e colorata all’interno. Per tornare velocemente alla modalità di “inseguimento”, basta toccare il simbolo GPS oppure riabilitare l’opzione nel menù.

## Note e bug

Vespucci supporta lo scaricamento, l’aggiunta di commenti e la chiusura delle note OSM (precedentemente conosciute come OSM bugs) e la funzionalità equivalente dei “Bug” prodotti dallo [strumento di controllo della qualità OSMOSE](http://osmose.openstreetmap.fr/it/map/). Entrambi vanno esplicitamente scaricati oppure è possibile attivare la funzione di scaricamento automatico per accedere agli elementi nelle immediate vicinanze. Ogni nota/bug può essere caricata sul server subito dopo essere stata modificata o chiusa oppure alla fine.

Sulla mappa le note e i bug sono rappresentati da una piccola icona di insetto ![Bug](../images/bug_open.png), quelle verdi sono chiuse/risolte, quelle blu sono quelle create o modificate da te e quelle rosse indicano le note e i bug attivi ma non ancora modificate. 

La visualizzazione dei bug OSMOSE mostrerà un link di colore blu all'oggetto associato, se si preme i link verrà selezionato l'oggetto , lo schermo lo mostrerà al centro e verrà scaricata l'area prima che sia necessario. 

### Filtraggio

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the [Advanced preferences](Advanced%20preferences.md) you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Custom

<a id="indoor"></a>

## Modalità interni

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

La modalità può essere abilitata premendo a lungo nel simbolo del lucchetto, vedi [Blocca, sblocca, cambiamento modalità](#lock), e selezionando l’elemento corrispondente del menù.

<a id="c-mode"></a>

## Modalità C

Nella modalità C vengono mostrati solo gli oggetti che hanno qualche problema vengono mostrati; questo rende molto facile identificare gli oggetti con particolari difetti o che non passano dei controlli configurabili dall’utente. Se un oggetto è selezionato e si fa partire l’editor delle proprietà in modalità C allora verrà automaticamente applicata la preimpostazione più corrispondente..

La modalità può essere abilitata premendo a lungo nel simbolo del lucchetto, vedi [Blocca, sblocca, cambiamento modalità](#lock), e selezionando l’elemento corrispondente del menù.

### Configurazione dei controlli

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Elementi da ricontrollare sul campo

Le voci da ricontrollare hanno le seguenti proprietà:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Chiave** e **Valore** sono confrontate con le etichette _esistenti_ dell’oggetto in questione.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Verifica elementi

La verifica degli elementi ha le seguenti proprietà:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtri

### Filtro dei tag

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Filtro dei preset

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Personalizzare di Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

### Layer settings

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Preferenze avanzate

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Segnalare un problema

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


