# Introduzione a Vespucci

Vespucci è un editor completo che supporta molte delle funzionalità di un normale editor desktop.  È stato testato con successo su Google Android nelle versioni dalla 2.3 alla 6.0 oltre che in alcune varianti basate su AOSP. Un avvertimento: anche se i dispositivi mobile hanno raggiunto la capacità dei rivali desktop, quelli particolarmente vecchi tendono ad avere poca memoria disponibile e ad essere piuttosto lenti. Dovresti tenerne conto quando usi Vespucci e per esempio ridurre l'area che stai modificando ad una dimensione accettabile. 

## Il primo utilizzo

All'avvio Vespucci mostra la schermata "Scarica altra posizione"/"Carica area". Se sono visibili le coordinate e vuoi scaricare i dati immediatamente, puoi selezionare l'opzione corrispondente e impostare il raggio dell'area che vuoi ottenere. Non scegliere un'area troppo grande su dispositivi lenti. 

Altrimenti puoi chiudere la schermata premendo il pulsante "Vai alla mappa", spostandoti e ingrandendo poi sull'area che vuoi modificare per poi scaricarne i dati (vedi più avanti: "Modificare con Vespucci")

## Mappare con Vespucci

A seconda della dimensione dello schermo e dell'età del tuo dispositivo, le diverse azioni potrebbero essere accessibili direttamente attraverso delle icone sulla barra in alto, nel menù a tendina sulla destra della barra in alto, sulla barra in basso (se presente) oppure attraverso il pulsante menù.

### Scaricare dati OSM

Seleziona l'icona di trasferimento ![](../images/menu_transfer.png)  oppure "Trasferisci" dal menù. Saranno mostrate sette opzioni:

* **Scarica vista corrente** - scarica la vista presente sullo schermo e rimpiazza qualsiasi dato già esistente *(è richiesta una connessione di rete)*
* **Aggiungi la vista corrente ai dati scaricati** - scarica l'area visibile sullo schermo e la aggiunge ai dati già presenti *(è richiesta una connessione di rete)*
* **Scarica altra posizione** - mostra una schermata per inserire coordinate, cercare un luogo o utilizzare la posizione attuale per poi scaricare i dati in un raggio attorno ad essa *(è richiesta una connessione di rete)*
* **Carica dati nel server OSM ** - carica le modifiche su OpenStreetMap *(è richiesta l'autenticazione)* *(è richiesta una connessione di rete)*
* **Scarica automaticamente** - scarica un'area attorno alla posizione attuale in maniera automatica (è richiesta una connessione di rete)* *(è richiesto il GPS)*
* **File...** - per salvare o caricare dati OSM verso e da file del dispositivo.
* **Note/Bug...** - scarica (automaticamente e manualmente) note OSM e "bug" dagli strumenti per il controllo della qualità (per ora solo OSMOSE) *(è richiesta una connessione di rete)*

Il modo più semplice di scaricare i dati sul dispositivo è di spostarsi e ingrandire sull'area che si vuole modificare e quindi selezionare "Scarica vista corrente". Puoi modificare l'ingrandimento usando i gesti, i pulsanti di zoom oppure i pulsanti per il controllo del volume del telefono. Vespucci a questo punto scaricherà i dati della vista corrente. Per scaricare i dati sul dispositivo non è necessario essere autenticati.

### Mappare

Per evitare di modificare accidentalmente, Vespucci all'avvio è in modalità "bloccato", una modalità che permette solo di ingrandire e spostarsi sulla mappa. Premi sull'icona [Bloccato](../images/locked.png) per sbloccare lo schermo. Una pressione prolungata sul lucchetto imposta la modalità "solo modifica di etichette" che non permette di aggiungere nuovi oggetti o di modificare la geometria di quelli esistenti; questa modalità è indicata da un'icona di lucchetto bianco leggermente diversa.

Se non diversamente specificato dall'utente, i nodi e i percorsi selezionabili hanno attorno ad essi un'area arancione che ne indica grossolanamente dove premere per selezionarli. Se si cerca di selezionare un oggetto e Vespucci individua più di un oggetto, verrà mostrato un menù di selezione. Gli oggetti selezionati sono evidenziati in giallo.

Se si cerca di modificare un'area ad alta densità di oggetti, è buona norma ingrandire la mappa.

Vespucci ha un buon sistema "annulla/ripeti" quindi non temere di sperimentare con il tuo dispositivo, tuttavia sei pregato di non caricare sui server dati inventati di sana pianta.

#### Selezionare / Deselezionare

Tocca un oggetto per selezionarlo ed evidenziarlo, un'ulteriore pressione sullo stesso oggetto apre l'editor delle etichette di quell'elemento. La pressione sullo schermo in una qualsiasi regione vuota lo deselezionerà. Se hai selezionato un oggetto e vuoi selezionarne un altro, basta premere l'oggetto in questione, senza bisogno di deselezionare prima l'altro. Una pressione doppia su un oggetto farà partire la [modalità multi-selezione](../en/Multiselect.md).

#### Aggiungere nuovi Nodi/Punti o percorsi

Premi a lungo dove vuoi piazzare il nodo o iniziare il percorso. Vedrai il simbolo di una croce nera. Se premi di nuovo nello stesso punto verrà creato un nuovo nodo, se tocchi al di fuori dalla zona di tolleranza verrà invece disegnato un segmento di percorso dal punto originale a quello corrente. 

Basta premere lo schermo dove vuoi aggiungere altri nodi al percorso. Per terminare, premi l'ultimo nodo per una seconda volta. Se il nodo iniziale e quello finale sono appartenenti ad un percorso, verranno inseriti automaticamente nel percorso.

#### Spostare un nodo o un percorso

Gli oggetti possono essere trascinati/spostati solo quando sono selezionati. Se hai abilitato l'area di trascinamento estesa nelle preferenze, sarà presente un'area molto estesa attorno al nodo selezionato così da rendere più semplice il riposizionamento dello stesso. 

#### Migliorare la forma di un percorso

Se ingrandisci abbastanza la mappa sarà visibile una piccola"x" nel centro di quei segmenti di percorso che non sono troppo corti. Trascinando la "x" verrà creato un nodo del percorso in quel punto. Nota: per evitare di creare accidentalmente dei nuovi nodi, la tolleranza al tocco per questa operazione è piuttosto bassa.

#### Tagliare, copiare e incollare

Puoi copiare o tagliare dei nodi o delle vie selezionati per poi incollare una o più volte in un nuovo punto. Quando si taglia la versione e l'id OSM rimarranno gli stessi. Per incollare, tieni premuto il punto in cui vuoi incollare (sarà visibile il simbolo di una croce nel punto). Poi seleziona "Incolla" nel menù.

#### Aggiungere indirizzi in modo efficiente

Vespucci ha una funzione "aggiungi le etichette dell'indirizzo civico" che tenta di rendere la mappatura degli indirizzi civici più efficiente. Può essere selezionata. 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Aggiungere divieti di accesso

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci in modalità "blocco"

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Salvare le tue modifiche

*(è richiesta una connessione di rete)*

Usa lo stesso pulsante o elemento del menù che hai usato per scaricare i dati e seleziona "Carica i dati nel server OSM"

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

Le nuove installazioni di Vespucci hanno l'autorizzazione OAuth abilitata. Al primo tentativo di caricare nuovi dati, verrà caricata una pagina del sito OSM. Dopo essersi autenticati (attraverso una connessione cifrata) verrà richiesto di autorizzare Vespucci modificare a nome tuo.  Se vuoi o senti il bisogno di autorizzare l'accesso OAuth al tuo account prima di iniziare a modificare c'è un apposita opzione nel menù "Strumenti".

Se vuoi salvare il tuo lavoro quando non hai accesso a Internet, puoi salvarlo su un file .osm compatibile con JOSM e in seguito caricarlo con Vespucci o con JOSM. 

#### Risolvere in conflitti durante il caricamento

Vespucci ha un risolutore semplificato dei conflitti. Se pensi di aver commesso qualche grosso errore nelle tue modifiche, puoi esportare i cambiamenti in un file .osc (opzione "Esporta" nel menù "Trasferisci") e risolverli per poi caricare i dati con JOSM. Ulteriori informazioni su [risoluzione dei confitti](../en/Conflict resolution.md).  

## Usare un GPS

Puoi usare Vespucci per creare delle tracce GPX da mostrare sul tuo dispositivo. Puoi inoltre mostrare la posizione GPS attuale (imposta "Mostra posizione" nel menù GPS) e/o impostare il centro dello schermo sulla posizione man mano che questa si aggiorna (imposta "Segui posizione GPS" nel menù GPS). 

Quando quest'ultima è abilitata, se viene mosso manualmente lo schermo o si eseguono modifiche la modalità "segui GPS" verrà disabilitata e al posto di un contorno verrà mostrata una freccia piena. Per tornare velocemente alla modalità "inseguimento", basta toccare la freccia o rispuntare l'opzione dal menù.

## Note e bug

Vespucci supporta lo scaricamento, l'aggiunta di commenti e la chiusura delle note OSM (precedentemente chiamate Bug OSM) e altrettante azioni per i "Bug" forniti dallo [strumento di controllo della qualità OSMOSE](http://osmose.openstreetmap.fr/en/map/). Per entrambi occorre che siano scaricati esplicitamente oppure può essere usata l'opzione dello scaricamento automatico per accedere gli elementi nell'area vicina. Dopo aver commentato o chiuso una nota o un bug, è possibile caricarli immediatamente o successivamente tutti insieme.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## Personalizzare di Vespucci

### Preferenze che potresti voler cambiare

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Preferenze avanzate

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Segnalare un problema

Se Vespucci si interrompe improvvisamente, oppure rileva uno stato di inconsistenza, ti verrà chiesto di inviare un rapporto a riguardo. Qualora succeda, sei pregato di farlo, ma solamente una volta per ogni tipo di errore. Se vuoi aggiungere altre informazioni o chiedere delle funzionalità aggiuntive, puoi farlo qua: [Servizio di segnalazione bug di Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Se vuoi discutere di Vespucci puoi iniziare una discussione nel [gruppo di Vespucci ospitato da Google](https://groups.google.com/forum/#!forum/osmeditor4android) oppure nel [forum Android ospitato da OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


