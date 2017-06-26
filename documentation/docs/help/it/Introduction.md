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

* dopo una pressione prolungata: Vespucci aggiunge un nodo in quel punto e cerca di predire il numero civico e la via sfruttando quelli che hai utilizzato per ultimi. Se il nodo appartiene ad un edificio, verrà aggiunta in automatico l'etichetta "entrance=yes". L'editor delle etichette viene aperto per l'oggetto in questione e potrà essere usato per eventuali altre modifiche.
* nella modalità nodo/percorso selezionato: Vespucci aggiunge le etichette d'indirizzo come sopra e apre la schermata dell'editor di etichette.
* nell'editor delle etichette.

La predizione dei numeri civici per funzionare richiede di solito che siano già stati inseriti due numeri civici per ogni lato della strada, più sono i civici inseriti meglio esso funzionerà.

Potrebbe essere utile usare la modalità "download automatico".  

#### Aggiungere divieti di accesso

Vespucci permette di inserire velocemente i divieti di svolta. Nota: se devi dividere un percorso per un divieto di svolta occorre farlo prima di iniziare.

* selezionare un percorso con l'etichetta highway (i divieti di svolta possono essere aggiunti solo alle strade, in alternativa è possibile usare la più generica modalità "crea relazione", il menù non verrà mostrato nemmeno se non vi sono possibili elementi di tipo "attraverso")
* selezionare "Aggiungi restrizione" dal menù
* selezionare un nodo o percorso di tipo "attraverso" (tutti i possibili elementi di tipo "via" saranno evidenziati come selezionabili)
* selezionare il percorso di tipo "verso" (è possibile ripetersi e impostare l'elemento di tipo "verso" uguale all'elemento di tipo "da", Vespucci in questo caso penserà che si sta  aggiungendo una restrizione di divieto di inversione a U)
* impostare il tipo di restrizione nel menù delle etichette

### Vespucci in modalità "blocco"

Quando è mostrato il lucchetto rosso, sono disponibili tutte le azioni che non modificano gli oggetti. Inoltre, se si preme a lungo su un oggetto OSM verranno mostrate le sue informazioni dettagliate.

### Salvare le tue modifiche

*(è richiesta una connessione di rete)*

Usa lo stesso pulsante o elemento del menù che hai usato per scaricare i dati e seleziona "Carica i dati nel server OSM"

Vespucci è compatibile sia con l'autorizzazione OAuth che con il metodo username e password. È meglio usare l'autorizzazione OAuth visto che evita di comunicare le password in chiaro.

Le nuove installazioni di Vespucci hanno l'autorizzazione OAuth abilitata. Al primo tentativo di caricare nuovi dati, verrà caricata una pagina del sito OSM. Dopo essersi autenticati (attraverso una connessione cifrata) verrà richiesto di autorizzare Vespucci modificare a nome tuo.  Se vuoi o senti il bisogno di autorizzare l'accesso OAuth al tuo account prima di iniziare a modificare c'è un apposita opzione nel menù "Strumenti".

Se vuoi salvare il tuo lavoro quando non hai accesso a Internet, puoi salvarlo su un file .osm compatibile con JOSM e in seguito caricarlo con Vespucci o con JOSM. 

#### Risolvere in conflitti durante il caricamento

Vespucci ha un risolutore semplificato dei conflitti. Se pensi di aver commesso qualche grosso errore nelle tue modifiche, puoi esportare i cambiamenti in un file .osc (opzione "Esporta" nel menù "Trasferisci") e risolverli per poi caricare i dati con JOSM. Ulteriori informazioni su [risoluzione dei confitti](../en/Conflict resolution.md).  

## Usare un GPS

Puoi usare Vespucci per creare delle tracce GPX da mostrare sul tuo dispositivo. Puoi inoltre mostrare la posizione GPS attuale (imposta "Mostra posizione" nel menù GPS) e/o impostare il centro dello schermo sulla posizione man mano che questa si aggiorna (imposta "Segui posizione GPS" nel menù GPS). 

Quando quest'ultima è abilitata, se viene mosso manualmente lo schermo o si eseguono modifiche la modalità "segui GPS" verrà disabilitata e al posto di un contorno verrà mostrata una freccia piena. Per tornare velocemente alla modalità "inseguimento", basta toccare la freccia o rispuntare l'opzione dal menù.

## Note e bug

Vespucci supporta lo scaricamento, l'aggiunta di commenti e la chiusura delle note OSM (precedentemente chiamate Bug OSM) e altrettante azioni per i "Bug" forniti dallo [strumento di controllo della qualità OSMOSE](http://osmose.openstreetmap.fr/en/map/). Per entrambi occorre che siano scaricati esplicitamente oppure può essere usata l'opzione dello scaricamento automatico per accedere gli elementi nell'area vicina. Dopo aver commentato o chiuso una nota o un bug, è possibile caricarli immediatamente o successivamente tutti insieme.

Sulla mappa le note e i bug sono rappresentati dall'icona di un piccolo insetto ![](../images/bug_open.png), quelli verdi sono risolti, quelli blu sono stati modificati o creati da te, quelli gialli sono ancora attivi e non sono stati modificati. 

La visualizzazione dei bug OSMOSE mostrerà un link di colore blu all'oggetto associato, se si preme i link verrà selezionato l'oggetto , lo schermo lo mostrerà al centro e verrà scaricata l'area prima che sia necessario. 

## Personalizzare di Vespucci

### Preferenze che potresti voler cambiare

* livello dello sfondo
* livello sovrapposto. L'aggiunta di un livello sovrapposto potrebbe causare qualche problema con i dispositivi vecchi in particolare quelli con poca memoria. Valore predefinito: nessuno.
* Visualizza Note/Bug. Mostra con l'icona di un insetto giallo le note e i bug ancora aperti, di un insetto verde quelli risolti. Valore predefinito: sì.
* Livello foto. Mostra le foto georeferenziate con un'icona di una macchina foto di colore rosso, nel caso sia presente anche l'informazione circa la direzione l'icona risulterà ruotata. Valore predefinito: no.
* Icone dei nodi. Valore predefinito: no.
* Mantieni lo schermo acceso. Valore predefinito: no.
* Area di trascinamento dei nodi estesa. Muovere i nodi su un dispositivo con il touchscreen può creare qualche problema se le dita nascondono la posizione corrente nello schermo. Con questa opzione è possibile sfruttare un'area maggiore quando si trascina dai bordi dello schermo (le operazioni di selezione e altre in genere avranno sempre la solita area di tolleranza). Valore predefinito: no.

#### Preferenze avanzate

* Abilita la split action bar. Negli smartphone recenti la barra viene suddivisa tra quella in alto e quella in basso dello schermo, i pulsanti sono presenti solo in quest'ultima. In questi casi è possibile mostrare altri pulsanti, anche se a scapito di una parte dello schermo. Se si disabilita questa opzione, i pulsanti saranno presenti solo sulla barra superiore. Nota: Vespucci deve essere riavviato affinché la modifica abbia effetto.
* Mostra sempre il menù contestuale. Se abilitata, ogni volta che si seleziona un oggetto verrà mostrato un menù contestuale, altrimenti questo verrà mostrato solo se non può essere selezionato un oggetto senza ambiguità. Valore predefinito: no (in versioni passate era "sì").
* Abilita tema chiaro. Abilitato in maniera predefinita su dispositivi recenti. Anche se può essere abilitato anche su dispositivi vecchi, lo stile potrebbe non essere in questo caso consistente.
* Mostra statistiche. Mostra statistiche utili al debug, generalmente poco utile. Valore predefinito: no (in versioni passate era "sì").  

## Segnalare un problema

Se Vespucci si interrompe improvvisamente, oppure rileva uno stato di inconsistenza, ti verrà chiesto di inviare un rapporto a riguardo. Qualora succeda, sei pregato di farlo, ma solamente una volta per ogni tipo di errore. Se vuoi aggiungere altre informazioni o chiedere delle funzionalità aggiuntive, puoi farlo qua: [Servizio di segnalazione bug di Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Se vuoi discutere di Vespucci puoi iniziare una discussione nel [gruppo di Vespucci ospitato da Google](https://groups.google.com/forum/#!forum/osmeditor4android) oppure nel [forum Android ospitato da OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


