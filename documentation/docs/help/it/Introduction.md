_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too._

# Introduzione a Vespucci

Vespucci è un editor completo per OpenStreetMap, che supporta la maggior parte delle funzionalità disponibili con gli editor desktop. È stato testato su Android nelle versioni dalla 2.3 alla 10.0, oltre che su alcune varianti di AOSP. Nota: anche se i dispositivi mobile hanno raggiunto la capacità dei rivali desktop, alcuni dispositivi particolarmente vecchi tendono ad avere poca memoria disponibile e ad essere piuttosto lenti. Dovresti tenerne conto quando usi Vespucci e agire di conseguenza, ad esempio mantenendo l'area che stai modificando ad una dimensione ragionevole.

## Mappare con Vespucci

A seconda della dimensione dello schermo e da quanto è vecchio il tuo dispositivo, le diverse azioni saranno accessibili direttamente attraverso le icone sulla barra in alto, nel menù a tendina sulla destra della barra in alto, sulla barra in basso (se presente) oppure attraverso il pulsante menù.

<a id="download"></a>

### Download dati OSM

Seleziona l'icona di trasferimento ![Transfer](../images/menu_transfer.png) oppure l'opzione "Trasferimento" nel menu. Verranno mostrate sette opzioni:

* **Scarica vista corrente** - scarica l’area visibile sullo schermo e la unisce ai dati esistenti *(richiede la connessione dati oppure una fonte dati offline)*
* **Cancella e scarica vista corrente** - cancella tutti i dati in memoria e poi scarica l’area visibile sullo schermo *(richiede la connessione dati)*
* **Carica dati nel server OSM** - carica le modifiche su OpenStreetMap *(richiede l’autenticazione)* *(richiede la connessione dati)*
* **Aggiorna dati** - riscarica i dati per tutte le aree e aggiorna quello che si trova in memoria *(richiede la connessione dati)*
* **Scaricamento automatico basato sulla posizione** - scarica automaticamente l’area attorno alla corrente posizione geografica *(richiede la connessione dati oppure una fonte dati offline)* *(richiede il GPS)*
* **Scarica automaticamente dopo zoom o spostamento schermata** - scarica automaticamente i dati per la mappa correntemente visualizzata *(richiede la connessione dati o una fonte dati offline)* *(richiede il GPS)*
* **File…** - salvataggio e caricamento di dati OSM da/verso file del dispositivo.
* **Compiti** - scarica (automaticamente e manualmente) le note OSM e i compiti dagli strumenti di controllo della qualità (attualmente OSMOSE) *(richiede la connessione dati)*

Il modo più semplice di scaricare i dati sul dispositivo è di spostarsi e ingrandire sull'area che si vuole modificare e quindi selezionare "Scarica vista corrente". Puoi modificare l'ingrandimento usando le dita, i pulsanti di zoom oppure i pulsanti per il controllo del volume del dispositivo. Vespucci a questo punto scaricherà i dati della vista corrente. Per scaricare i dati sul dispositivo non è necessario essere l'autenticazione.

Con le impostazioni predefinite tutte le aree non scaricate vengono scurite rispetto a quelle scaricate, al fine di evitare l’aggiunta di oggetti duplicati in aree che non sono visualizzate. Tale comportamento può essere modificato nelle [Preferenze avanzate](Advanced%20preferences.md).

### Mappare

<a id="lock"></a>

#### Blocca, sblocca, cambio modalità

Per evitare modifiche accidentali, Vespucci viene eseguito in modalità "bloccato", la quale consente di utilizzare solamente le funzioni di zoom e movimento della mappa. Premi  ![Bloccato](../images/locked.png) per passare alla modalità modifica. 

Una lunga pressione sull'icona di blocco farà apparire un menù con 4 opzioni:

* **Normale** - è la modalità predefinita di modifica, consente di aggiungere oggetti, modificare quelli esistenti, spostarli e rimuoverli. L'icona corrispondente è una lucchetto bianco.
* **Solo Etichette** - selezionando un elemento si avvierà l'editor delle proprietà degli oggetti, una pressione prolungata sullo schermo aggiungerà un nodo. L'icona mostrata è un lucchetto bianco con una "T".
* **Indirizzi** - abilita la modalità Indirizzi, una modalità leggermente semplificata avente anche specifiche azioni del pulsante “+” in [modalità semplificata](../it/Simple%20actions.md). L’icona mostrata è un lucchetto con la "A".
* **Interni** - attiva la modalità interni, vedi [Indoor mode](#indoor). L'icona corrispondente è un lucchetto bianco con una "I".
* **C-Mode** - attiva la modalità C, mostra solo gli oggetti contrassegnati con una bandierina di attenzione, vedi  [C-Mode](#c-mode). L'icona corrispondente è un lucchetto bianco con una "C".

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

Una volta che la selezione è completata vedrai (o come pulsante o come elementi del menu) una lista di operazioni supportate per gli oggetti selezionati: per maggiori informazioni vedi [Nodo selezionato](Node%20selected.md), [Linea selezionata](Way%20selected.md) e [Relazione selezionata](Relation%20selected.md).

#### Oggetto selezionato: modifica etichette

Un secondo tocco sull'oggetto selezionato apre il menù di modifica delle etichette, dal quale è possibile modificare le etichette associate all'oggetto.

Tieni presente che per gli oggetti sovrapposti (come per esempio un nodo su un percorso) il menù di selezione comparirà una seconda volta. Se selezioni lo stesso oggetto, ti verrà mostrato l’editor delle etichette; se ne selezioni un altro, esso verrà semplicemente selezionato.

#### Oggetto selezionato: spostare un nodo o una linea

Una volta selezionato un oggetto, lo si può spostare. Un oggetto può essere trascinato/spostato solo dopo essere stato selezionato. Per spostarlo devi premere sull'oggetto selezionato (entro un certo raggio di tolleranza) e trascinare fino dove si desidera spostarlo. Dal menù [Preferenze](Preferences.md) è possibile ingrandire l'area di selezione che compare attorno agli oggetti selezionati e che permette di semplificare il loro posizionamento. 

#### Aggiungere nuovi Nodi/Punti o percorsi 

Al primo avvio l'app parte in "Modalità semplice". Questo può essere modificato ne menu principale togliendo la spunta al box corrispondente.

##### Modalità semplice

Premendo sul grande pulsante verde al di sopra della mappa verrà mostrato un menù. Dopo che avrai selezionato una voce, ti verrà chiesto di premere sullo schermo nel punto in cui vuoi creare l’oggetto, lo scorrimento e lo zoom funzionano sempre se hai bisogno di modificare la vista sulla mappa. 

Consulta [Creare nuovi oggetti in modalità semplice](Creating%20new%20objects%20in%20simple%20actions%20mode.md) per altre informazioni.

##### Modalità avanzata (tenendo premuto)

Premi a lungo dove vuoi posizionare il nodo o dove comincia il percorso. Vedrai un simbolo nero a forma di “mirino”.
* se vuoi creare un nuovo nodo (non connesso ad un altro oggetto), clicca lontano da oggetti esistenti.
* se vuoi estendere un percorso, clicca all’interno della "zona di tolleranza" del percorso (o su un nodo del percorso). La zona di tolleranza è indicata dalle aree attorno ad un nodo o percorso.

Quando è visibile il simbolo a forma di croce, hai le seguenti opzioni:

* _Premi sullo stesso punto._
    * Se la croce non è vicina ad un nodo, premendo nello stesso punto di nuovo si crea un nuovo nodo. Se è vicino ad un percorso (ma non ad un nodo), il nuovo nodo sarà aggiunto al percorso.
    * Se la croce è vicina ad un nodo (cioè all'interno della zona di tolleranza), premendo nello stesso punto semplicemente seleziona quel nodo (e si apre l’editor delle etichette). Nessun nodo viene creato. Si tratta della stessa azione vista in precedenza che si ha per la selezione.
* _Premi in un’altra zona._ Premendo in un altro punto (al di fuori della zona di tolleranza della croce) si crea un segmento che parte dalla posizione della croce fino alla posizione corrente. Se la croce era vicina ad un nodo o un percorso, il nuovo segmento sarà connesso a quel nodo od a quel segmento.

Premi semplicemente sullo schermo dove vuoi aggiungere altri nodi al percorso. Per completare, premi il nodo finale due volte. Se il nodo finale è su un nodo o percorso già esistenti, il segmento verrà automaticamente collegato a quel nodo o a quel percorso. 

Puoi anche usare una voce del menù: vedi [Creare nuovi oggetti](Creating%20new%20objects.md) per maggiori approfondimenti.

#### Aggiungere un'area

Attualmente OpenStreetMap, a differenza di altri sistemi di dati geografici, non è provvisto di oggetti di tipo "area". L’editor online "iD" cerca di creare un modello di area utilizzando i sottostanti elementi di OSM che però non funziona sempre bene. Vespucci al momento non fa nulla di tutto ciò, quindi è necessario che tu sappia come i percorsi di tipo area sono rappresentati:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Migliorare la forma di un percorso

Se ingrandisci la mappa su di un percorso vedrai delle piccole “x” nel mezzo dei segmenti non troppo corti della linea. Se trascini una di queste “x” verrà creato un nodo in quel punto. Nota: per evitare la creazione accidentale dei nodi, la zona di tolleranza per questo tipo di operazione è piuttosto piccola.

#### Tagliare, copiare e incollare

Puoi copiare o tagliare dei nodi o delle vie selezionati per poi incollare una o più volte in un nuovo punto. Quando si taglia la versione e l'id OSM rimarranno gli stessi. Per incollare, tieni premuto il punto in cui vuoi incollare (sarà visibile il simbolo di una croce nel punto). Poi seleziona "Incolla" nel menù.

#### Aggiungere indirizzi in modo efficiente

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

La predizione dei numeri civici per funzionare richiede di solito che siano già stati inseriti due numeri civici per ogni lato della strada, più sono i civici inseriti meglio esso funzionerà.

Considerare l'uso di questa modalità con una delle modalità [Auto-download](#download).  

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

Vespucci dispone di un semplice risolutore di conflitti. Tuttavia se pensi che nelle tue modifiche vi siano dei gravi errori, esporta le tue modifiche su un file .osc (la voce “Esporta” nel menù “Trasferisci”), risolvi i problemi e poi carica i dati sul server con JOSM. Consulta la guida dettagliata sulla [risoluzione dei conflitti](Conflict%20resolution.md).  

## Uso di tracce GPS e GPX

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Per registrare una traccia GPX e visualizzarla sul dispositivo, selezionare la voce "Avvia traccia GPX" nel menu ![GPS](../images/menu_gps.png). Questo aggiungerà uno strato alla visualizzazione con la traccia correntemente registrata; è possibile caricare ed esportare la traccia dalla voce nel [controllo livelli] (Main%20map%20display.md). Altri strati possono essere aggiunti da un documento GPX locale e da tracce scaricate dall'API OSM.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Note e bug

Vespucci supporta lo scaricamento, l’aggiunta di commenti e la chiusura delle note OSM (precedentemente conosciute come OSM bugs) e la funzionalità equivalente dei “Bug” prodotti dallo [strumento di controllo della qualità OSMOSE](http://osmose.openstreetmap.fr/it/map/). Entrambi vanno esplicitamente scaricati oppure è possibile attivare la funzione di scaricamento automatico per accedere agli elementi nelle immediate vicinanze. Ogni nota/bug può essere caricata sul server subito dopo essere stata modificata o chiusa oppure alla fine.

Sulla mappa le note e i bug sono rappresentati da una piccola icona di insetto ![Bug](../images/bug_open.png), quelle verdi sono chiuse/risolte, quelle blu sono quelle create o modificate da te e quelle rosse indicano le note e i bug attivi ma non ancora modificate. 

La visualizzazione dei bug OSMOSE mostrerà un link di colore blu all'oggetto associato, se si preme i link verrà selezionato l'oggetto , lo schermo lo mostrerà al centro e verrà scaricata l'area prima che sia necessario. 

### Filtraggio

Oltre ad abilitare globalmente la visualizzazione di note e bug puoi anche impostare un semplice filtro per ridurne il numero. La configurazione di tale filtro può essere effettuata accedendo alla voce dello strato compiti nel [controllo dei livelli](#layers):

* Note
* Errore Osmose
* Avviso Osmose
* Errore minore Osmose
* Maproulette
* Personalizzato

<a id="indoor"></a>

## Modalità interni

La mappatura dei luoghi chiusi è complessa a causa del numero elevato di oggetti, i quali spesso si troverebbero sovrapposti sulla mappa. Vespucci integra una apposita modalità che consente di filtrare gli oggetti a seconda del livello su cui si trovano e di aggiungere automaticamente il valore livello agli oggetti ivi creati.

La modalità può essere abilitata premendo a lungo nel simbolo del lucchetto, vedi [Blocca, sblocca, cambiamento modalità](#lock), e selezionando l’elemento corrispondente del menù.

<a id="c-mode"></a>

## Modalità C

Nella modalità C vengono mostrati solo gli oggetti che hanno qualche problema vengono mostrati; questo rende molto facile identificare gli oggetti con particolari difetti o che non passano dei controlli configurabili dall’utente. Se un oggetto è selezionato e si fa partire l’editor delle proprietà in modalità C allora verrà automaticamente applicata la preimpostazione più corrispondente..

La modalità può essere abilitata premendo a lungo nel simbolo del lucchetto, vedi [Blocca, sblocca, cambiamento modalità](#lock), e selezionando l’elemento corrispondente del menù.

### Configurazione dei controlli

Allo stato attuale è possibile configurare due controlli (i controlli per le etichette FIXME e un test per le etichette ‘type’ mancanti nelle relazioni che preferenze che non sono attualmente configurabili) entrambi possono essere configurati selezionando “Impostazioni validatore” nelle [preferenze](Preferences.md).. 

La lista delle voci è divisa in due, la metà più in alto indica le voci da “controllare di nuovo”, quella più in basso le “voci da verificare”. Gli elementi della lista possono essere modificate cliccandoci sopra, il pulsante verde del menù permette di aggiungere nuovi controlli.

#### Elementi da ricontrollare sul campo

Le voci da ricontrollare hanno le seguenti proprietà:

* **Chiave** - Chiave dell’etichetta scelta.
* **Valore** - Valore dell’etichetta scelta dovrebbe avere, se vuoto il valore dell’etichetta verrà ignorato.
* **Età** - quanti giorni devono passare dall’ultima modifica dell’oggetto per richieder un altro controllo sul campo, se presente l’etichetta _check_date_ allora verrà usato quella per la verifica, altrimenti verrà presa in considerazione la data in cui la versione attuale è stata creata. Se impostato a zero, il controllo verrà effettuato solo su chiavi e valori corrispondenti.
* **Espressione regolare** - se spuntato, **Valore** sarà considerato come una espressione regolare di tipo JAVA.

**Chiave** e **Valore** sono confrontate con le etichette _esistenti_ dell’oggetto in questione.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Verifica elementi

La verifica degli elementi ha le seguenti proprietà:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtri

### Filtro dei tag

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Filtro dei preset

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Personalizzare di Vespucci

Molti aspetti dell'app possono essere personalizzati; se si cerca qualcosa di specifico e non lo si trova, [il sito web di Vespucci] (https://vespucci.io/) è ricercabile e contiene informazioni aggiuntive rispetto a quelle disponibili sul dispositivo.

<a id="layers"></a>

### Impostazioni degli strati

Le impostazioni degli strati possono essere modificate tramite il controllo degli strati (menu "hamburger" in alto a destra), mentre tutte le altre impostazioni sono raggiungibili tramite il pulsante preferenze del menu principale. Gli strati possono essere attivati, disattivati e temporaneamente nascosti.

Tipi di strati disponibili:

* Strato dati: è lo strato in cui vengono caricati i dati di OpenStreetMap. Nell'uso normale non è necessario modificare nulla. Predefinito: attivo.

* Strato di sfondo - è disponibile un'ampia gamma di immagini aeree e satellitari di sfondo. Il valore predefinito è la mappa "stile standard" di openstreetmap.org.
* Strato di sovrapposizione: si tratta di strati semitrasparenti con informazioni aggiuntive, ad esempio tracce GPX. L'aggiunta di uno strato sovrapposto può causare problemi con i dispositivi più vecchi e con memoria limitata. Predefinito: spento.
* Visualizzazione di note e bug - Le note e i bug aperti vengono visualizzati con un'icona gialla, quelli chiusi con un'icona verde. Predefinito: attivo.
* Strato foto - Visualizza le foto geo-referenziate come icone rosse della fotocamera; se sono disponibili informazioni sulla direzione, l'icona sarà ruotata. Predefinito: spento.
* Strato Mapillary - Visualizza i segmenti Mapillary con i marcatori dove sono presenti le immagini; premendo su un marcatore si visualizza l'immagine. Predefinito: spento.
* Strato GeoJSON - Visualizza il contenuto di un file GeoJSON. Predefinito: spento.
* Griglia - Visualizza una scala lungo i lati della mappa o una griglia. Predefinito: attivo. 

Ulteriori informazioni sono disponibili nella sezione sulla [map display] (Main%20map%20display.md).

#### Preferenze

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

La descrizione completa può essere trovata qua [Preferenze](Preferences.md)

#### Preferenze avanzate

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

La descrizione completa può essere trovata qua [Preferenze avanzate](Advanced%20preferences.md)

## Segnalare un problema

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

Se volete discutere riguardo a Vespucci, potete avviare una discussione sul [gruppo Google Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) o sul [forum OpenStreetMap Android](http://forum.openstreetmap.org/viewforum.php?id=56).


