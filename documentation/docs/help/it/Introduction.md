# Introduzione a Vespucci

Vespucci è un completo editor per OpenStreetMap, che supporta la maggior parte delle funzionalità disponibili con gli editor desktop.  È stato testato su Android nelle versioni dalla 2.3 alla 7.0, oltre che su alcune varianti di AOSP.
Nota: anche se i dispositivi mobile hanno raggiunto la capacità dei rivali desktop, alcuni dispositivi particolarmente vecchi tendono ad avere poca memoria disponibile e ad essere piuttosto lenti. Dovresti tenerne conto quando usi Vespucci e agire di conseguenza, ad esempio riducendo l'area che stai modificando. 

## Primo utilizzo

All'avvio Vespucci mostra la schermata "Scarica altra posizione"/"Carica area". Se sono visibili le coordinate e vuoi scaricare i dati immediatamente, puoi selezionare l'opzione corrispondente e impostare il raggio dell'area che vuoi ottenere. Non scegliere un'area troppo grande se hai un dispositivo lento. 

Altrimenti puoi chiudere la schermata premendo il pulsante "Vai alla mappa". Puoi quindi spostare la mappa, ingrandire sull'area che vuoi modificare e scaricarne i dati (vedi più avanti: "Modificare con Vespucci").

## Mappare con Vespucci

A seconda della dimensione dello schermo e da quanto è vecchio il tuo dispositivo, le diverse azioni saranno accessibili direttamente attraverso le icone sulla barra in alto, nel menù a tendina sulla destra della barra in alto, sulla barra in basso (se presente) oppure attraverso il pulsante menù.

<a id="download"></a>

### Download dati OSM

Seleziona l'icona di trasferimento ![Transfer](../images/menu_transfer.png) oppure l'opzione "Trasferimento" nel menu. Verranno mostrate sette opzioni:

* **Scarica vista corrente** - scarica l'area visibile sullo schermo e sostituisce i dati già scaricati *(richiede connessione a Internet)*
* **Aggiungi vista corrente ai dati** - scarica l'area visibile sullo schermo e la unisce con i dati già scaricati *(richiede connessione a Internet)*
* **Scarica altra posizione** - permette di scaricare l'area attorno alla posizione specificata, che può essere inserita con le coordinate geografiche, cercando il nome del luogo o usando la posizione corrente *(richiede connessione a Internet)*
* **Carica i dati su OSM** - carica le modifiche su OpenStreetMap *(richiede autenticazione)* *(richiede connessione a Internet)*
* **Scarica area in automatico** - scarica in automatico l'area attorno alla posizione geografica attuale *(richiede connessione a Internet)* *(richiede GPS)*
* **File...** - carica o salva dati OSM tramite file salvati sul dispositivo
* **Note/problemi mappa...** - scarica manualmente o automaticamente le note di OSM e i problemi segnalati dagli strumenti QA (Quality Assurance; attualmente OSMOSE) *(richiede connessione a Internet)*

Il modo più semplice di scaricare i dati sul dispositivo è di spostarsi e ingrandire sull'area che si vuole modificare e quindi selezionare "Scarica vista corrente". Puoi modificare l'ingrandimento usando le dita, i pulsanti di zoom oppure i pulsanti per il controllo del volume del dispositivo. Vespucci a questo punto scaricherà i dati della vista corrente. Per scaricare i dati sul dispositivo non è necessario essere l'autenticazione.

### Mappare

<a id="lock"></a>

#### Blocca, sblocca, cambio modalità

Per evitare modifiche accidentali, Vespucci viene eseguito in modalità "bloccato", la quale consente di utilizzare solamente le funzioni di zoom e movimento della mappa. Premi  ![Bloccato](../images/locked.png) per passare alla modalità modifica. 

Una lunga pressione sull'icona di blocco farà apparire un menù con 4 opzioni:

* **Normale** - è la modalità predefinita di modifica, consente di aggiungere oggetti, modificare quelli esistenti, spostarli e rimuoverli. L'icona corrispondente è una lucchetto bianco.
* **Solo Etichette** - selezionando un elemento si avvierà l'editor delle proprietà degli oggetti, una pressione prolungata sullo schermo aggiungerà un nodo. L'icona mostrata sarà un lucchetto bianco con una "T".
* **Interni** - attiva la modalità interni, vedi [Indoor mode](#indoor). L'icona corrispondente è un lucchetto bianco con una "I".
* **C-Mode** - attiva la modalità C, mostra solo gli oggetti contrassegnati con una bandierina di attenzione, vedi  [C-Mode](#c-mode). L'icona corrispondente è un lucchetto bianco con una "C".

#### Tocco singolo, doppio tocco, pressione prolungata

Di defualt, gli oggetti selezionabili e le linee sono bordate in arancione, con l'obiettivo di indicare dove premere per selezionarle. Ci sono 3 possibilità:

* Tocco singolo: seleziona un oggetto
     * Un nodo o un percorso vengono immediatamente evidenziati
     * Tuttavia, se si cerca di selezionare un oggetto e Vespucci individua più oggetti in quel punto, verrà mostrato un menù di selezione che permetterà di scegliere l’oggetto da selezionare. 
    * Gli oggetti selezionati sono evidenziati in giallo. 
    * Per ulteriori informazioni vedere [Nodo selezionato](../en/Node%20selected.md), [Percorso selezionato](../en/Way%20selected.md) e [Relazione selezionata](../en/Relation%20selected.md). 
    * Tocco doppio: fa partire la [modalità multiselezione](../en/Multiselect.md). 
    * Pressione a lungo: crea una "croce" che permette di aggiungere nodi, vedi qui sotto oppure in [Creare nuovi oggetti](../en/Creating%20new%20objects.md)

Se si cerca di modificare un'area ad alta densità di oggetti, è buona norma ingrandire la mappa.

Vespucci ha un buon sistema "annulla/ripeti" quindi non temere di sperimentare con il tuo dispositivo, tuttavia sei pregato di non caricare sui server dati inventati di sana pianta.

#### Selezione / De-selezione (tocco singolo e "menu selezione")

Premi su un oggetto per selezionarlo ed evidenziarlo. Toccare lo schermo in un punto senza oggetti consente di de-selezionare l'oggetto. Se hai selezionato un oggetto ma hai bisogno di selezionarle un altro, per farlo ti basta premere su questo secondo oggetto, non c'è bisogno di de-selezionare il precedente. Un doppio tocco su un oggetto avvia la funzione selezione multipla [Modalità selezione multipla](../en/Multiselect.md).

Tieni presente che se cerchi di selezionare un oggetto e Vespucci rileva più oggetti in quel punto (per esempio un nodo su un percorso o altri oggetti sovrapposti) verrà mostrato un menù di selezione: premi sull’oggetto che vuoi selezionare ed esso verrà selezionato. 

Gli oggetti selezionati sono indicati da una sottile bordatura gialla. La bordatura potrebbe essere difficile da vedere, ma una volta selezionato l'oggetto, apparirà una notifica a conferma.

Completata la selezione vedrai una lista delle operazioni che è possibile effettuare: per maggiori informazioni vedi [Nodo selezionato](../en/Node%20selected.md), [Linea selezionata](../en/Way%20selected.md) and [Relazione selezionata](../en/Relation%20selected.md).

#### Oggetto selezionato: modifica etichette

Un secondo tocco sull'oggetto selezionato apre il menù di modifica delle etichette, dal quale è possibile modificare le etichette associate all'oggetto.

Tieni presente che per gli oggetti sovrapposti (come per esempio un nodo su un percorso) il menù di selezione comparirà una seconda volta. Se selezioni lo stesso oggetto, ti verrà mostrato l’editor delle etichette; se ne selezioni un altro, esso verrà semplicemente selezionato.

#### Oggetto selezionato: spostare un nodo o una linea

Una volta selezionato un oggetto, lo si può spostare. Un oggetto può essere trascinato/spostato solo dopo essere stato selezionato. Per spostarlo devi premere sull'oggetto selezionato e trascinare fino dove si desidera spostarlo. Dal menù opzioni è possibile ingrandire l'area di selezione che compare a fianco degli oggetti selezionati. 

#### Aggiungere un nuovo nodo o una linea (pressione prolungata)

Premi a lungo dove vuoi posizionare il nodo o che cominci il percorso. Vedrai un simbolo a forma di croce.
* se vuoi creare un nuovo nodo (non connesso ad un altro oggetto), clicca lontano da oggetti esistenti.
* se vuoi estendere un percorso, clicca all’interno della "zona di tolleranza" del percorso (o su un nodo del percorso). La zona di tolleranza è indicata dalle aree attorno ad un nodo o percorso.

Quando è visibile il simbolo a forma di croce, hai le seguenti opzioni:

* Premere nello stesso punto.
    * Se la croce non è vicina ad un nodo, premendo nello stesso punto di nuovo si crea un nuovo nodo. Se è vicino ad un percorso (ma non ad un nodo), il nuovo nodo sarà aggiunto al percorso.
    * Se la croce è vicina ad un nodo (cioè all'interno della zona di tolleranza), premendo nello stesso punto semplicemente seleziona quel nodo (e si apre l’editor delle etichette). Nessun nodo viene creato. Si tratta della stessa azione vista in precedenza che si ha per la selezione.
* Premere in un’altra zona. Premendo in un altro punto (al di fuori della zona di tolleranza della croce) si crea un segmento che parte dalla posizione della croce fino alla posizione corrente. Se la croce era vicina ad un nodo o un percorso, il nuovo segmento sarà connesso a quel nodo od a quel segmento.

Premi semplicemente sullo schermo dove vuoi aggiungere altri nodi al percorso. Per completare, premi il nodo finale due volte. Se il nodo finale è su un nodo o percorso già esistenti, il segmento verrà automaticamente collegato a quel nodo o a quel percorso. 

Puoi anche usare un elemento del menù: vedi [Creare nuovi oggetti](../en/Creating%20new%20objects.md) per maggiori approfondimenti.

#### Aggiungere un'area

Attualmente OpenStreetMap, a differenza di altri sistemi di dati geografici, non è provvisto di oggetti di tipo "area". L’editor online "iD" cerca di creare un modello di area utilizzando i sottostanti elementi di OSM che però non funziona sempre bene. Vespucci al momento non fa nulla di tutto ciò, quindi è necessario che tu sappia come i percorsi di tipo area sono rappresentati:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Migliorare la forma di un percorso

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Tagliare, copiare e incollare

Puoi copiare o tagliare dei nodi o delle vie selezionati per poi incollare una o più volte in un nuovo punto. Quando si taglia la versione e l'id OSM rimarranno gli stessi. Per incollare, tieni premuto il punto in cui vuoi incollare (sarà visibile il simbolo di una croce nel punto). Poi seleziona "Incolla" nel menù.

#### Aggiungere indirizzi in modo efficiente

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

La predizione dei numeri civici per funzionare richiede di solito che siano già stati inseriti due numeri civici per ogni lato della strada, più sono i civici inseriti meglio esso funzionerà.

Valuta di utilizzare la modalità di download automatico  

#### Aggiungere divieti di accesso

Vespucci consente di aggiungere velocemente le restrizioni di svolta. Se necessario, divide automaticamente le linee e chiede di selezionare nuovamente gli elementi oggetto della restrizione 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci in modalità "blocco"

Quando è mostrato il lucchetto rosso, sono disponibili tutte le azioni che non modificano gli oggetti. Inoltre, se si preme a lungo su un oggetto OSM verranno mostrate le sue informazioni dettagliate.

### Salvare le tue modifiche

*(è richiesta una connessione di rete)*

Usa lo stesso pulsante o elemento del menù che hai usato per scaricare i dati e seleziona "Carica i dati nel server OSM"

Vespucci è compatibile sia con l'autorizzazione OAuth che con il metodo username e password. È meglio usare l'autorizzazione OAuth visto che evita di comunicare le password in chiaro.

Le nuove installazioni di Vespucci hanno l'autorizzazione OAuth abilitata. Al primo tentativo di caricare nuovi dati, verrà caricata una pagina del sito OSM. Dopo essersi autenticati (attraverso una connessione cifrata) verrà richiesto di autorizzare Vespucci modificare a nome tuo.  Se vuoi o senti il bisogno di autorizzare l'accesso OAuth al tuo account prima di iniziare a modificare c'è un apposita opzione nel menù "Strumenti".

Se vuoi salvare il tuo lavoro quando non hai accesso a Internet, puoi salvarlo su un file .osm compatibile con JOSM e in seguito caricarlo con Vespucci o con JOSM. 

#### Risolvere in conflitti durante il caricamento

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict%20resolution.md).  

## Usare un GPS

Puoi usare Vespucci per creare delle tracce GPX da mostrare sul tuo dispositivo. Puoi inoltre mostrare la posizione GPS attuale (imposta "Mostra posizione" nel menù GPS) e/o impostare il centro dello schermo sulla posizione man mano che questa si aggiorna (imposta "Segui posizione GPS" nel menù GPS). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Note e bug

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

La visualizzazione dei bug OSMOSE mostrerà un link di colore blu all'oggetto associato, se si preme i link verrà selezionato l'oggetto , lo schermo lo mostrerà al centro e verrà scaricata l'area prima che sia necessario. 

### Filtraggio

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue

<a id="indoor"></a>

## Modalità interni

La mappatura dei luoghi chiusi è complessa a causa del numero elevato di oggetti, i quali spesso si troverebbero sovrapposti sulla mappa. Vespucci integra una apposita modalità che consente di filtrare gli oggetti a seconda del livello su cui si trovano, e di aggiungere automaticamente il valore livello agli oggetti creati.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## Modalità C

Nella modalità C vengono mostrati solo gli oggetti che hanno qualche problema vengono mostrati; questo rende molto facile identificare gli oggetti con particolari difetti o che non passano dei controlli configurabili dall’utente. Se un oggetto è selezionato e si fa partire l’editor delle proprietà in modalità C allora verrà automaticamente applicata la preimpostazione più corrispondente..

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half check "entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a check_date field is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Check optional** - Check the optional tags of the matching preset.

This check works be first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Check optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtri

### Filtro dei tag

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](../en/Tag%20filter.md).

### Filtro dei preset

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](../en/Preset%20filter.md).

## Personalizzare di Vespucci

### Preferenze che potresti voler cambiare

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Preferenze avanzate

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Segnalare un problema

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


