# Εισαγωγή στο Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size. 

## Πρώτη χρήση

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Εναλλακτικα μπορείτε να κλείσετε το παράθυρο πατώντας το κουμπί "Μετάβαση στον χάρτη" και να μετακινήσετε και να εστιάσετε σε μια τοποθεσία που θέλετε να επεξεργαστείτε και να κατεβάσετε τότε τα δεδομένα (δείτε παρακάτω: "Επεξεργασία με το Vespucci").

## Επεξεργασία με το Vespucci

Ανάλογα με το μέγεθος οθόνης και την ηλικία της συσκευής σας, οι ενέργειες επεξεργασίας μπορεί να είναι διαθέσιμες είτε μέσω των εικονιδίων της μπάρας στην κορυφή, είτε μέσω του πτυσσόμενου μενού στα δεξιά της μπάρας, από την κάτω μπάρα (αν είναι παρούσα) είτε μέσω του κουμπιού μενού.

<a id="download"></a>

### Λήψη Δεδομένων OSM

Επιλέξτε είτε το εικονίδιο μεταφοράς ![Μεταφορά](../ images / menu_transfer.png) είτε το στοιχείο μενού "Μεταφορά". Αυτό θα εμφανίσει επτά επιλογές:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Ο ευκολότερος τρόπος λήψης δεδομένων στη συσκευή είναι να κάνετε μεγέθυνση και να μετακινηθείτε στη θέση που θέλετε να επεξεργαστείτε και, στη συνέχεια, να επιλέξετε "Λήψη τρέχουσας προβολής". Μπορείτε να κάνετε μεγέθυνση χρησιμοποιώντας τα χέρια σας, τα κουμπιά ζουμ ή τα κουμπιά ελέγχου της έντασης στη συσκευή. Το Vespucci θα πρέπει στη συνέχεια να πραγματοποιήσει λήψη δεδομένων για την τρέχουσα προβολή. Δεν απαιτείται επαλήθευση για τη λήψη δεδομένων στη συσκευή σας.

### Επεξεργασία

<a id="lock"></a>

#### Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας

Για να αποφύγετε τις κατά λάθος επεξεργασίες το Vespucci ξεκινάει σε "κλειδωμένη" λειτουργία, η οποία επιτρέπει μόνο τη μεγέθυνση και τη μετακίνηση του χάρτη. Αγγίξτε το εικονίδιο ![Κλειδωμένο] (../ images / locked.png) για να ξεκλειδώσετε την οθόνη. 

Με το παρατεταμένο πάτημα του εικονιδίου κλειδώματος θα εμφανιστεί ένα μενού που προσφέρει 4 επιλογές:

* **Κανονική** - η προεπιλεγμένη λειτουργία επεξεργασίας, νέα αντικείμενα μπορούν να προστεθούν, τα υπάρχοντα να επεξεργαστούν, να μετακινηθούν και να αφαιρεθούν. Εμφανίζεται το εικονίδιο μιας απλής λευκής κλειδαριάς.
* **Μόνο ετικέτα** - επιλέγοντας ένα υπάρχον αντικείμενο θα ξεκινήσει ο επεξεργαστής ιδιοτήτων, με παρατεταμένο πάτημα στην κύρια οθόνη θα προστεθούν αντικείμενα, αλλά δεν θα δουλέψουν άλλες λειτουργίες γεωμετρίας. Εμφανίζεται το εικονίδιο μιας λευκής κλειδαριάς με "T".
* **Εσωτερικό** - επιτρέπει την εσωτερική λειτουργία, δείτε [Εσωτερική λειτουργία] (# εσωτερική). Εμφανίζεται το εικονίδιο μιας λευκής κλειδαριάς με "I".
* **Λειτουργία-C** - ενεργοποιεί την Λειτουργία-C, μόνο τα αντικείμενα που έχουν μια προειδοποιητική σημαία θα εμφανιστούν, δείτε [Λειτουργία-C] (#λειτουργία-c). Εμφανίζεται το εικονίδιο μιας λευκής κλειδαριάς με "C".

#### Απλό χτύπημα, διπλό χτύπημα, και παρατεταμένο πάτημα

Από προεπιλογή, οι επιλέξιμοι κόμβοι και οι διαδρομές έχουν μια πορτοκαλί περιοχή γύρω τους δείχνοντας περίπου το σημείο όπου πρέπει να αγγίξετε για να επιλέξετε ένα αντικείμενο. Έχετε τρεις επιλογές:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

Είναι καλή στρατηγική να εστιάζστε αν επιχειρείτε να επεξεργαστείτε μια περιοχή υψηλής πυκνότητας.

Το Vespucci έχει ένα καλό σύστημα "αναίρεσης/επανάληψης", οπότε μην φοβάστε να πειραματιστείτε στη συσκευή σας, ωστόσο μην αποστέλλετε και αποθηκεύετε καθαρά δοκιμαστικά δεδομένα.

#### Επιλογή / Αποεπιλογή (απλό πάτημα και "επιλογή μενού")

Αγγίξτε ένα αντικείμενο για να το επιλέξετε και να το επισημάνετε. Αγγίζοντας την οθόνη σε μια κενή περιοχή θα το αποεπιλέξετε. Εάν έχετε επιλέξει ένα αντικείμενο και θα πρέπει να επιλέξετε κάτι άλλο, απλώς αγγίξτε το αντικείμενο που θέλετε, χωρίς να χρειάζεται να αποεπιλέξετε το πρώτο. Ένα διπλό χτύπημα σε ένα αντικείμενο θα ξεκινήσει [Λειτουργία Πολυεπιλογής] (../ en / Multiselect.md).

Σημειώστε ότι αν προσπαθήσετε να επιλέξετε ένα αντικείμενο και το Vespucci υπολογίζει ότι η επιλογή μπορεί να σημαίνει πολλαπλά αντικείμενα (όπως ένας κόμβος σε μία διαδρομή ή άλλα επικαλυπτόμενα αντικείμενα) θα παρουσιάσει ένα μενού επιλογής: Πατήστε το αντικείμενο που θέλετε να επιλέξετε και το αντικείμενο είναι επιλεγμένο. 

Τα επιλεγμένα αντικείμενα υποδεικνύονται με ένα λεπτό κίτρινο περίγραμμα. Το κίτρινο περίγραμμα μπορεί να είναι δύσκολο να εντοπιστεί, ανάλογα με το φόντο του χάρτη και το συντελεστή μεγέθυνσης. Αφού γίνει μια επιλογή, θα δείτε μια ειδοποίηση που επιβεβαιώνει την επιλογή.

Μόλις ολοκληρωθεί η επιλογή, θα δείτε (είτε ως κουμπιά ή ως αντικείμενα μενού) μια λίστα υποστηριζόμενων λειτουργιών για το επιλεγμένο αντικείμενο: Για περισσότερες πληροφορίες, δείτε στο [Επιλεγμένος Κόμβος](../en/Node%20selected.md), [Επιλεγμένη Διαδρομή](../en/Way%20selected.md) και [Επιλεγμένη Σχέση](../en/Relation%20selected.md).

#### Επιλεγμένα αντικείμενα: Επεξεργασία ετικετών

Ένα δεύτερο άγγιγμα στο επιλεγμένο αντικείμενο ανοίγει τον επεξεργαστή ετικετών και μπορείτε να επεξεργαστείτε τις ετικέτες που σχετίζονται με το αντικείμενο.

Σημειώστε ότι για τα επικαλυπτόμενα αντικείμενα (όπως ένας κόμβος σε μία διαδρομή) το μενού επιλογής εμφανίζεται για δεύτερη φορά. Επιλέγοντας το ίδιο αντικείμενο εμφανίζεται ο επεξεργαστής ετικετών, επιλέγοντας ένα άλλο αντικείμενο απλά επιλέγει το άλλο αντικείμενο.

#### Επιλεγμένα αντικείμενα: Μετακίνηση ενός κόμβου ή διαδρομής

Μόλις επιλέξετε ένα αντικείμενο, αυτό μπορεί να μετακινηθεί. Σημειώστε ότι τα αντικείμενα μπορούν να συρθούν/μετακινηθούν μόνο όταν είναι επιλεγμένα. Απλά σύρετε κοντά (π.χ. εντός της ζώνης ανοχής) του επιλεγμένου αντικειμένου για να το μετακινήσετε. Εάν επιλέξετε σύροντας μια μεγάλη περιοχή στις προτιμήσεις, θα έχετε μια μεγάλη περιοχή γύρω από τον επιλεγμένο κόμβο που διευκολύνει την τοποθέτηση του αντικειμένου. 

#### Προσθέτοντας ένα νέο Κόμβο/Σημείο ή Διαδρομή 

Κατά την πρώτη εκκίνηση η εφαρμογή ξεκινάει σε "απλή λειτουργία", αυτό μπορεί να αλλάξει στο κύριο μενού, με το ξετσεκάρισμα του αντίστοιχου πλαίσιο ελέγχου.

##### Απλή λειτουργία

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

Δείτε [Δημιουργία νέων αντικειμένων σε λειτουργία απλών ενεργειών](Creating%20new%20objects%20in%20simple%20actions%20mode.md) για περισσότερες πληροφορίες.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Μόλις δείτε το σταυρόνημα, έχετε αυτές τις επιλογές:

* Άγγιγμα στην ίδια θέση.
    * Εάν το σταυρόνημα δεν βρίσκεται κοντά σε έναν κόμβο, αγγίζοντας ξανά την ίδια θέση δημιουργεί έναν νέο κόμβο. Αν βρίσκεστε κοντά σε μία διαδρομή (αλλά όχι κοντά σε έναν κόμβο), ο νέος κόμβος θα βρίσκεται στην διαδρομή (και θα συνδεθεί με την διαδρομή).
     * Εάν το σταυρόνημα βρίσκεται κοντά σε έναν κόμβο (δηλ. Εντός της ζώνης ανοχής του κόμβου), αγγίζοντας την ίδια θέση επιλέγει απλά τον κόμβο (και ο επεξεργαστής ετικετών ανοίγει.) Δεν δημιουργείται νέος κόμβος Η ενέργεια είναι ίδια με την παραπάνω επιλογή.
* Άγγιγμα σε άλλη θέση. Εάν αγγίξετε σε άλλη θέση (εκτός της ζώνης ανοχής του σταυρονήματος), προστίθεται ένα τμήμα διαδρομής από την αρχική θέση στην τρέχουσα θέση. Εάν το σταυρόνημα ήταν κοντά σε μία διαδρομή ή κόμβο, το νέο τμήμα θα συνδεθεί με αυτήν την διαδρομή ή τον κόμβο.

Απλώς αγγίξτε την οθόνη εκεί όπου θέλετε να προσθέσετε επιπλέον κόμβους σε μία διαδρομή. Για να ολοκληρώσετε, αγγίξτε δύο φορές τον τελικό κόμβο. Εάν ο τελικός κόμβος βρίσκεται σε μία διαδρομή ή έναν κόμβο, το τμήμα θα συνδεθεί αυτόματα στη διαδρομή ή τον κόμβο. 

Μπορείτε επίσης να χρησιμοποιήσετε ένα μενού αντικειμένου: Δείτε [Δημιουργία νέων αντικειμένων] (/Creating%20new%20objects.md) για περισσότερες πληροφορίες.

#### Προσθέτοντας μια Περιοχή

Το OpenStreetMap επί του παρόντος δεν διαθέτει τύπο αντικειμένου ως "περιοχή" σε αντίθεση με άλλα συστήματα γεω-δεδομένων. Ο διαδικτυακός επεξεργαστής "iD" προσπαθεί να δημιουργήσει μια αφαίρεση περιοχής από τα υποκείμενα στοιχεία OSM που λειτουργεί καλά σε ορισμένες περιπτώσεις, ενώ σε άλλες όχι. Το Vespucci αυτή τη στιγμή δεν προσπαθεί να κάνει κάτι παρόμοιο, οπότε πρέπει να γνωρίζετε λίγο τον τρόπο με τον οποίο απεικονίζονται οι περιοχές:

* _Κλειστές διαδρομές (* πολυγώνια") _: η απλούστερη και συνηθέστερη παραλλαγή περιοχής, είναι διαδρομές που μοιράζονται τον πρώτο και τελευταίο κόμβο σχηματίζοτας ένα κλειστό "δακτύλιο" (για παράδειγμα τα περισσότερα κτίρια είναι αυτού του τύπου). Αυτό είναι πολύ εύκολο να δημιουργηθεί στο Vespucci, απλά συνδεθείτε στον πρώτο κόμβο όταν τελειώσετε με την σχεδίαση της περιοχής. Σημείωση: η ερμηνεία της κλειστής διαδρομής εξαρτάται από την ετικέτα της: για παράδειγμα, αν μία κλειστή διαδρομή έχει επισημανθεί ως κτίριο, θα θεωρηθεί σαν περιοχή, αλλά όχι σε περίπτωση που έχει επισημανθεί ως κυκλικός κόμβος. Σε ορισμένες περιπτώσεις όπου και οι δύο ερμηνείες μπορεί να είναι έγκυρες, μια ετικέτα "περιοχή" μπορεί να διευκρινίσει την προτιθέμενη χρήση.
* _πολυ-πολύγωνα_: ορισμένες περιοχές έχουν πολλαπλά μέρη, τρύπες και δακτυλίους που δεν μπορούν να αναπαρασταθούν με μία μόνο διαδρομή. Το OSM χρησιμοποιεί ένα συγκεκριμένο τύπο σχέσης για να το πετύχουμε (το αντικείμενο γενικού σκοπού που μπορεί να κάνει μοντέλο τις σχέσεις μεταξύ των στοιχείων), ένα πολυ-πολύγωνο. Ένα πολυ-πολύγωνο μπορεί να έχει πολλαπλούς "εξωτερικούς" δακτυλίους και πολλαπλούς "εσωτερικούς" δακτυλίους. Κάθε δακτύλιος μπορεί είτε να είναι μία κλειστή διαδρομή, όπως περιγράφηκε παραπάνω, ή πολλαπλές μεμονωμένες διαδρομές που έχουν κοινούς τον ακριανό κόμβο. Ενώ τα μεγάλα πολυ-πολύγων είναι δύσκολο να χειριστούν με οποιοδήποτε εργαλείο, τα μικρά δεν είναι δύσκολο να δημιουργηθούν στο Vespucci.
* _ακτογραμμές_: για πολύ μεγάλα αντικείμενα, ηπείρους και νησιά, ακόμα και το πολυ-πολυγωνικό μοντέλο δεν λειτουργεί με ικανοποιητικό τρόπο. Για τις διαδρομές natural=coastline θεωρούμε τη σημασιολογική εξάρτηση της κατεύθυνσης: η γη βρίσκεται στην αριστερή πλευρά της διαδρομής, το νερό στη δεξιά πλευρά. Μια παρενέργεια αυτού του γεγονότος είναι ότι, γενικά, δεν πρέπει να αντιστρέψετε την κατεύθυνση μιας διαδρομής με τη επισήμανση της ακτογραμμής. Περισσότερες πληροφορίες μπορείτε να βρείτε στο [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Βελτίωση της Γεωμετρίας της Διαδρομής

Εάν κάνετε μεγέθυνση αρκετά σε μία επιλεγμένη διαδρομή, θα δείτε ένα μικρό "x" στην μέση της διαδρομής των τμημάτων που είναι αρκετά μακριά. Σέρνοντας το "x" θα δημιουργηθεί ένας κόμβος στην διαδρομή σε εκείνο το σημείο. Σημείωση: για να αποφευχθεί κατά λάθος δημιουργία κόμβων, η περιοχή ανοχής αφής για αυτή τη λειτουργία είναι αρκετά μικρή.

#### Αποκοπή, Αντιγραφή και Επικόλληση

Μπορείτε να αντιγράψετε ή να αποκόψετε επιλεγμένους κόμβους και διαδρομές και στη συνέχεια να τα επικολλήσετε μια ή περισσότερες φορές σε μια νέα θέση. Η κοπή θα διατηρήσει την ταυτότητα και την έκδοση του osm. Για να επικολλήσετε πατήστε παρατεταμένα στην τοποθεσία που θέλετε να επικολλήσετε (θα δείτε ένα σταυρόνημα που σηματοδοτεί τη θέση). Στη συνέχεια, επιλέξτε "Επικόλληση" από το μενού.

#### Αποτελεσματική Προσθήκη Διευθύνσεων

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

Η πρόβλεψη αριθμού κατοικίας απαιτεί συνήθως τουλάχιστον δύο αριθμούς κατοικιών σε κάθε πλευρά του δρόμου για να τεθεί σε λειτουργία, όσο περισσότεροι αριθμοί υπάρχουν στα δεδομένα τόσο το καλύτερο.

Εξετάστε την χρήση αυτού με τη λειτουργία [Αυτόματη λήψη] (#λήψη).  

#### Προσθέτοντας Περιορισμούς Στροφής

Το Vespucci έχει έναν γρήγορο τρόπο να προσθέσει περιορισμούς στροφής. Εάν είναι απαραίτητο, θα διαιρέσει διαδρομές αυτόματα και θα σας ζητήσει να επιλέξετε εκ νέου στοιχεία. 

* επιλογή μιας διαδρομής με μια ετικέτα αυτοκινητόδρομου (οι περιορισμοί στροφής μπορούν να προστεθούν μόνο σε αυτοκινητόδρομους, εάν πρέπει να το κάνετε αυτό για άλλες διαδρομές, χρησιμοποιήστε τη γενική λειτουργία "δημιουργίας σχέσης")
* επιλογή "Προσθήκη περιορισμού" από το μενού
* επιλογή κόμβου ή διαδρομής "μέσω" (τα μόνο πιθανά στοιχεία "μέσω" θα εμφανίζουν την περιοχή αφής)
επιλέξτε την διαδρομή "προς" (είναι δυνατό να πάτε αντίθετα και να ορίσετε το στοιχείο "προς" στο στοιχείο "από", το Vespucci θα υποθέσει ότι προσθέτετε περιορισμό αναστροφής)
* ορίστε τον τύπο περιορισμού

### Το Vespucci σε  "κλειδωμένη" λειτουργία

Όταν εμφανιστεί η κόκκινη κλειδαριά, όλες οι ενέργειες μη επεξεργασίας είναι διαθέσιμες. Επιπλέον, με παρατεταμένο πάτημα πάνω ή κοντά σε ένα αντικείμενο θα εμφανίσει την οθόνη λεπτομερών πληροφοριών αν πρόκειται για αντικείμενο του OSM.

### Αποθήκευση των Αλλαγών σας

*(απαιτείται σύνδεση δικτύου)*

Επιλέξτε το ίδιο κουμπί ή μενού αντικειμένου που εκτελέσατε για τη λήψη και τώρα επιλέξτε "Μεταφόρτωση δεδομένων στον διακομιστή του OSM".

Το Vespucci υποστηρίζει την εξουσιοδότηση OAuth και την κλασική μέθοδο ονόματος χρήστη και κωδικού πρόσβασης. Το OAuth είναι προτιμότερο δεδομένου ότι αποφεύγει την φανερή αποστολή κωδικών πρόσβασης.

Οι νέες λήψεις στο Vespucci θα έχουν ενεργοποιημένο από προεπιλογή το OAuth. Κατά την πρώτη προσπάθειά σας για μεταφόρτωση τροποποιημένων δεδομένων, φορτώνεται μια σελίδα από τον ιστότοπο OSM. Αφού συνδεθείτε (μέσω κρυπτογραφημένης σύνδεσης), θα σας ζητηθεί να εξουσιοδοτήσετε τον Vespucci να επεξεργαστεί χρησιμοποιώντας το λογαριασμό σας. Αν θέλετε ή πρέπει να εξουσιοδοτήσετε το OAuth για πρόσβαση στο λογαριασμό σας πριν από την επεξεργασία, υπάρχει ένα αντίστοιχο στοιχείο στο μενού "Εργαλεία".

Εάν θέλετε να αποθηκεύσετε την εργασία σας και δεν έχετε πρόσβαση στο Internet, μπορείτε να αποθηκεύσετε σε ένα αρχείο .osm συμβατό με το JOSM και είτε να μεταφορτώσετε αργότερα με το Vespucci είτε με το JOSM. 

#### Επίλυση συγκρούσεων στις μεταφορτώσεις

Το Vespucci έχει έναν απλό επιλυτή συγκρούσεων. Ωστόσο, εάν υποψιάζεστε ότι υπάρχουν σημαντικά ζητήματα με τις επεξεργασίες σας, εξαγάγετε τις αλλαγές σας σε ένα αρχείο .osc ("Εξαγωγή" μενού αντικειμένου στο μενού "Μεταφορά") και διορθώστε τα και ανεβάστε τα με το JOSM. Δείτε την λεπτομερή βοήθεια στο [επίλυση συγκρούσεων] (../en/Conflict%20resolution.md).  

## Χρήση του GPS

Μπορείτε να χρησιμοποιήσετε το Vespucci για να δημιουργήσετε ένα ίχνος GPX και να το εμφανίσετε στη συσκευή σας. Περαιτέρω μπορείτε να εμφανίσετε την τρέχουσα θέση GPS (ορίστε στο "Εμφάνιση τοποθεσίας" στο μενού GPS) ή/και να έχετε το κέντρο της οθόνης τριγύρω και να ακολουθείτε τη θέση (ορίστε "Ακολουθήστε τη θέση GPS" στο μενού GPS). 

Εάν έχετε ορίσει το τελευταίο, η μετακίνηση της οθόνης χειροκίνητα ή η επεξεργασία θα προκαλέσει την απενεργοποίηση της λειτουργίας "παρακολούθηση του GPS" και το μπλε βέλος του GPS θα αλλάξει από περίγραμμα σε πλήρες βέλος. Για να επιστρέψετε γρήγορα στη λειτουργία "παρακολούθηση", αγγίξτε απλώς το κουμπί GPS ή επανελέγξτε την επιλογή μενού.

## Σημειώσεις και Σφάλματα

Το Vespucci υποστηρίζει τη λήψη, την σχολιασμό και το κλείσιμο των Σημειώσεων OSM (πρώην OSM Bugs) και την αντίστοιχη λειτουργικότητα για τα "Σφάλματα" που παράγονται από το εργαλείο [διασφάλιση ποιότητας του OSMOSE](http://osmose.openstreetmap.fr/en/map/). Και τα δύο πρέπει είτε να είναι κατεβαίνουν λεπτομερώς είτε μπορείτε να χρησιμοποιήσετε τη δυνατότητα αυτόματης λήψης για να αποκτήσετε πρόσβαση στα στοιχεία της περιοχής σας. Μόλις επεξεργαστείτε ή κλείσετε, μπορείτε είτε να μεταφορτώσετε το σφάλμα ή τη σημείωση αμέσως ή να τα ανεβάσετε όλα ταυτόχρονα.

Στον χάρτη οι Σημειώσεις και τα σφάλματα αντιπροσωπεύονται από ένα μικρό εικονίδιο σφάλματος ! [Σφάλμα](../images/bug_open.png), τα πράσινα είναι κλειστά/επιλυμένα, τα μπλε έχουν δημιουργηθεί ή επεξεργαστεί από εσάς και τα κίτρινο υποδεικνύουν ότι εξακολουθούν να είναι ενεργά και δεν έχουν αλλάξει. 

Η απεικόνιση σφαλμάτων του OSMOSE θα παρέχει μια σύνδεση με το επηρεασμένο αντικείμενο με μπλε χρώμα, αγγίζοντας το σύνδεσμο θα επιλέγεται το αντικείμενο, θα κεντράρεται η οθόνη πάνω του και θα φορτώνει εκ των προτέρων την περιοχή, εάν είναι απαραίτητο. 

### Φιλτράρισμα

Εκτός από την παγκόσμια ενεργοποίηση εμφάνισης των σημειώσεων και των σφαλμάτων, μπορείτε να ορίσετε ένα χονδροειδές φίλτρο απεικόνισης για να μειώσετε όλο αυτόν τον όγκο. Στις [Προηγμένες προτιμήσεις](Advanced%20preferences.md) μπορείτε να επιλέξετε ξεχωριστά:

* Σημειώσεις
* Σφάλμα Osmose
* Προειδοποίση Osmose
* Μικρό ζήτημα Osmose
* Προτίμηση

<a id="indoor"></a>

## Εσωτερική λειτουργία

Η χαρτογράφηση σε εσωτερικούς χώρους είναι δύσκολη λόγω του μεγάλου αριθμού αντικειμένων που συχνά επικαλύπτουν ο ένας τον άλλον. Το Vespucci έχει μια ειδική λειτουργία εσωτερικού χώρου που σας επιτρέπει να φιλτράρετε όλα τα αντικείμενα που δεν βρίσκονται στο ίδιο επίπεδο και τα οποία θα προσθέσουν αυτόματα το τρέχον επίπεδο σε νέα αντικείμενα που έχουν δημιουργηθεί.

Η λειτουργία μπορεί να ενεργοποιηθεί πατώντας παρατεταμένα το στοιχείο κλειδώματος, δείτε στο [Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας] (#κλειδαριά) και επιλέγοντας την αντίστοιχη καταχώρηση μενού.

<a id="c-mode"></a>

## Λειτουργία-C

Στη Λειτουργία-C εμφανίζονται μόνο αντικείμενα που έχουν μια προειδοποιητική σημαία σήμανσης, γεγονός που καθιστά εύκολο το εντοπισμό αντικειμένων που παρουσιάζουν συγκεκριμένα προβλήματα ή ταιριάζουν με ρυθμιζόμενους ελέγχους. Αν επιλεγεί ένα αντικείμενο και ο Επεξεργαστής Ιδιοτήτων ξεκινήσει στη Λειτουργία-C, θα εφαρμοστεί αυτόματα η καλύτερη προεπιλεγμένη προρύθμιση.

Η λειτουργία μπορεί να ενεργοποιηθεί πατώντας παρατεταμένα το στοιχείο κλειδώματος, δείτε στο [Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας] (#κλειδαριά) και επιλέγοντας την αντίστοιχη καταχώρηση μενού.

### Διαμόρφωση των ελέγχων

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

Ο κατάλογος των καταχωρίσεων χωρίζεται στα δύο, ο πάνω μισός κατάλογος καταγράφει την "επανεξέταση", ο κάτω μισός ελέγχει τις "καταχωρήσεις". Οι καταχωρίσεις μπορούν να επεξεργαστούν κάνοντας κλικ σε αυτές, το πράσινο κουμπί μενού επιτρέπει την προσθήκη των καταχωρήσεων.

#### Εισαγωγές επανεξέτασης

Οι εισαγωγές επανεξέτασης έχουν τις εξής ιδιότητες:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Κλειδί** και **Τιμή** ελέγχονται έναντι των _υπαρχόντων_ ετικετών του αντικειμένου στην ερώτηση.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Έλεγχος καταχωρήσεων

Οι έλεγχοι καταχωρίσεων έχουν τις εξής δύο ιδιότητες:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Φίλτρα

### Ετικέτα βασισμένη σε φίλτρο

Το φίλτρο μπορεί να ενεργοποιηθεί από το κύριο μενού, και μπορεί να αλλάξει πατώντας το εικονίδιο του φίλτρου. Περισσότερη τεκμηρίωση μπορείτε να βρείτε εδώ [Φίλτρο ετικέτας] (Tag% 20filter.md).

### Προρύθμιση βασισμένη σε φίλτρο

Μια εναλλακτική λύση στα παραπάνω αντικείμενα φιλτράρονται είτε σε μεμονωμένες προεπιλογές είτε σε προκαθορισμένες ομάδες. Χτυπώντας στο εικονίδιο του φίλτρου θα εμφανιστεί ένα προκαθορισμένο παράθυρο επιλογής παρόμοιο με αυτό που χρησιμοποιείται κάπου αλλού στο Vespucci. Οι μεμονωμένες προεπιλογές μπορούν να επιλεγούν με κανονικό κλικ, οι προκαθορισμένες ομάδες με παρατεταμένο κλικ (το κανονικό κλικ μπαίνει στην ομάδα). Περισσότερες οδηγίες μπορείτε να βρείτε εδώ [Προκαθορισμένο φίλτρο](../en/Preset%20filter.md).

## Προσαρμογή του Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

### Layer settings

Layer settings can be changed via the layer control (upper right corner), all other setting are reachable via the main menu preferences button.

* Background layer - there is a wide range of aerial and satellite background imagery available, , the default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Σύνθετες προτιμήσεις

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Αναφορά Προβλημάτων

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

Αν θέλετε να συζητήσετε κάτι που σχετίζεται με το Vespucci, μπορείτε είτε να ξεκινήσετε μια συζήτηση στο [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) είτε στο [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


