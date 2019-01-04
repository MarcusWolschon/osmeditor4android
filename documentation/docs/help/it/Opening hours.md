# Editor degli orari di apertura

Le specifiche sugli orari di apertura di OpenStreetMap sono piuttosto articolate e non sono facilmente rappresentabili tramite un’interfaccia utente intuitiva.

Comunque sia il più delle volte solo una piccola parte della definizione viene realmente usata. L’editor considera questo aspetto omettendo gli elementi più oscuri dai menù e riducendo spesso e volentieri la libertà di azione dell’utente a piccole personalizzazioni di modelli predefiniti.

_Questa documentazione è in continuo sviluppo_

## Utilizzare l'editor degli orari di apertura

Nello scenario tipico, l’oggetto che stai modificando può sia avere già un’etichetta per gli orari di apertura (opening_hours, service_times e collection_times) oppure puoi riapplicare il valore predefinito all’oggetto per avere il campo degli orari di apertura vuoto. Se senti la necessità di inserire il valore manualmente e stai usando Vespucci, puoi modificare il campo nella pagina dettagli e poi tornare alla scheda per le modifiche. Se pensi che l’etichetta per gli orari di apertura debba far parte delle preimpostazioni, segnala il problema agli sviluppatori del tuo editor.

Se hai definito un modello predefinito (puoi farlo attraverso il menù “Gestisci modelli”) questo verrà caricato automaticamente con un valore vuoto all’avvio dell’editor. Con la funzione “Carica modello” carichi un qualsiasi modello precedentemente salvato mentre con il menù “Salva modello” salvi il valore attuale in un nuovo modello. Puoi definire modelli diversi e predefiniti per le etichette “opening_hours”, “collection_times” e “service_times”.

Chiaramente puoi anche decidere di scrivere di tuo pugno un valore per gli orari di apertura, ma è consigliato partire da un modello preesistente.

Quando viene caricato un valore esistente per gli orari di apertura, il programma cerca di renderlo conforme alle specifiche degli orari di apertura. Qualora non fosse possibile, la posizione approssimativa dell’errore verrà evidenziata nella stringa grezza degli orari di apertura così che tu possa provare a correggerla manualmente. Circa un quarto dei valori degli orari di apertura nel database di OpenStreetMap hanno qualche problema, di questi meno del 10% può essere corretto, vedi [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) per ulteriori dettagli su quali deviazioni dalle specifiche sono tollerate.

### Bottone menù principale

* __Aggiungi regola__: aggiunge una nuova regola.
* __Aggiungi regola per le festività__: aggiunge una nuova regola per i giorni festivi insieme ad un cambiamento di stato.
* __Aggiungi una regola per 24/7__: aggiunge una regola  per un oggetto che è sempre aperto, le specifiche degli orari di apertura non supportano valori secondari per 24/7 comunque noi permettiamo selezioni di periodi più grandi (per esempio periodi con una durata di anni).
* __Carica modello__: carica un modello esistente.
* __Salva come modello__: salva il valore attuale degli orari di apertura in un modello da usare in futuro.
* __Gestisci modelli__: modifica, per esempio cambia il nome, e cancella i modelli esistenti.
* __Aggiorna__: ricontrolla il valore degli orari di apertura.
* __Elimina tutto__: elimina tutte le regole.

### Regole

Le regole predefinite sono aggiunte come _normali_ regole, ciò implica che esse sostituiscono i valori delle regole precedenti per gli stessi giorni. Questo potrebbe dare problemi quando si specifica orari estesi, in questi casi probabilmente dovrai impostare il tipo di regola nel menù _Mostra tipo di regola_ su _additivo_.

#### Menù regole

* __Aggiungi modificatore/commento__: modifica l’effetto di questa regola e aggiungi un commento facoltativo.
* __Aggiungi festività__: aggiungi un selettore per festività pubbliche o scolastiche.
* __Aggiungi intervallo di tempo__
    * __Orario - orario__: un orario di inizio ed uno di fine nello stesso giorno.
    * __Orario - orario esteso__: da un orario di inizio fino ad un orario di fine del giorno seguente (ad esempio 26:00 che significa le 2 del mattino del giorno seguente).
    * __Orario var. - orario__: da un orario di inizio variabile (crepuscolo, alba, tramonto) ad uno di fine nello stesso giorno.
    * __Orario var. - orario esteso__: da un orario variabile di inizio ad uno di fine nel giorno seguente.
    * __Orario - orario var.__: da un orario di inizio ad uno di fine variabile.
    * __Orario var. - orario var.__: da un orario di inizio variabile ad uno di fine variabile.
    * __Orario__: un punto nel tempo.
    * __Orario-senza un termine__: da un orario di inizio in poi.
    * __orario variabile__: ad un orario non preciso
    * __Orario variabile-senza un termine__: da un orario di inizio variabile in poi
* __Aggiungi intervallo di giorni__: aggiungi un selettore di giorni della settimana.
* __Aggiungi intervallo di date...__
    * __Data - data__: da una data di inizio (anno, mese, giorno) fino ad una di fine.
    * __Data variabile - data__: da una data di inizio non precisa (per ora ciò significa _Pasqua_) fino ad una data di fine.
    * __Data - data variabile__: da una data di inizio ad una data non precisa.
    * __Data variabile - data variabile__: da una data non precisa ad un’altra.
    * __Occorrenza nel mese - occorrenza nel mese__: da un numero di giorno settimanale del mese ad un altro numero di giorno settimanale dello stesso mese.
    * __Occorrenza nel mese - data__: da un numero di giorno settimanale del mese ad una data di fine.
    * __Data - occorrenza nel mese__: da una data di inizio fino a un numero di giorno settimanale del mese.
    * __Occorrenza nel mese - data variabile__: da un numero di giorno settimanale del mese fino a una data non precisa.
    * __Data variabile - occorrenza nel mese__: da una data non precisa di inizio fino al numero di giorno settimanale nel mese.
    * __Data - senza termine__: da una data di inizio in poi.
    * __Data variabile - senza un termine__: da una data di inizio variabile in poi.
    * __Occorrenza nel mese - senza un termine__: dal numero di giorno della settimana di un mese in poi.
    * __Con scostamenti...__: le stesse voci come sopra solo con specificati degli scostamenti (raramente usato).
* __Aggiungi intervallo annuale__: aggiungi un selettore per gli anni.
* __Aggiungi intervallo di settimane__: aggiungi un selettore per il numero della settimana.
* __Duplica__: crea una copia di questa regola e inseriscila dopo la posizione corrente.
* __Mostra tipo di regola__: visualizza e modifica il tipo di regola _normale_, _additivo_ e _di riserva_ (non disponibile per la prima regola).
* __Sposta su__: sposta questa regola di una posizione in alto (non disponibile per la prima regola).
* __Sposta giù__: sposta questa regola di una posizione in basso.
* __Elimina__: elimina questa regola.

### Intervalli di tempo

Per rendere la modifica degli intervalli di tempo più semplice possibile, quando vengono caricati valori già esistenti scegliamo un intervallo di tempo ed una granularità per le barre di intervallo che riteniamo ottimali. Per gli intervalli temporali nuovi le barre iniziano alle 6 del mattino e hanno incrementi di 15 minuti, questo può essere modificato attraverso il menù.

#### Menù intervalli di tempo

* __Mostra selettore orario__: visualizza un grande selettore dell’orario per scegliere l’ora di inizio e quella di fine, negli schermi molto piccoli è il modo migliore di modificare gli orari.
* __Passa a scatti di 15 minuti__: utilizza una granularità di 15 minuti per la barra dell’intervallo.
* __Passa a scatti di 5 minuti__: utilizza una granularità di 5 minuti per la barra dell’intervallo.
* __Passa a scatti di 1 minuto__: utilizza una granularità di 1 minuto per la barra dell’intervallo.
* __Inizia a mezzanotte__: fa iniziare la barra dell’intervallo dalla mezzanotte.
* __Mostra intervallo__: visualizza il campo dell’intervallo per specificare un intervallo in minuti.
* __Elimina__: elimina questo intervallo di tempo.

