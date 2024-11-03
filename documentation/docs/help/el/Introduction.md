_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Εισαγωγή στο Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Επεξεργασία με το Vespucci

Ανάλογα με το μέγεθος οθόνης και την ηλικία της συσκευής σας, οι ενέργειες επεξεργασίας μπορεί να είναι διαθέσιμες είτε μέσω των εικονιδίων της μπάρας στην κορυφή, είτε μέσω του πτυσσόμενου μενού στα δεξιά της μπάρας, από την κάτω μπάρα (αν είναι παρούσα) είτε μέσω του κουμπιού μενού.

<a id="download"></a>

### Λήψη Δεδομένων OSM

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display eleven options:

* **Upload data to OSM server...** - review and upload changes to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Review changes...** - review current changes
* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory, including pending changes, and then download the area visible on the screen *(requires network connectivity)*
* **Query Overpass...** - run a query against a Overpass API server *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Clear data** - remove any OSM data in memory, including pending changes.
* **File...** - saving and loading OSM data to/from on device files.
* **Tasks...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Ο ευκολότερος τρόπος λήψης δεδομένων στη συσκευή είναι να κάνετε μεγέθυνση και να μετακινηθείτε στη θέση που θέλετε να επεξεργαστείτε και, στη συνέχεια, να επιλέξετε "Λήψη τρέχουσας προβολής". Μπορείτε να κάνετε μεγέθυνση χρησιμοποιώντας τα χέρια σας, τα κουμπιά ζουμ ή τα κουμπιά ελέγχου της έντασης στη συσκευή. Το Vespucci θα πρέπει στη συνέχεια να πραγματοποιήσει λήψη δεδομένων για την τρέχουσα προβολή. Δεν απαιτείται επαλήθευση για τη λήψη δεδομένων στη συσκευή σας.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Επεξεργασία

<a id="lock"></a>

#### Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας

Για να αποφύγετε τις κατά λάθος επεξεργασίες το Vespucci ξεκινάει σε "κλειδωμένη" λειτουργία, η οποία επιτρέπει μόνο τη μεγέθυνση και τη μετακίνηση του χάρτη. Αγγίξτε το εικονίδιο ![Κλειδωμένο] (../ images / locked.png) για να ξεκλειδώσετε την οθόνη. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

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

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Προσθέτοντας ένα νέο Κόμβο/Σημείο ή Διαδρομή 

Κατά την πρώτη εκκίνηση η εφαρμογή ξεκινάει σε "απλή λειτουργία", αυτό μπορεί να αλλάξει στο κύριο μενού, με το ξετσεκάρισμα του αντίστοιχου πλαίσιο ελέγχου.

##### Απλή λειτουργία

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Μόλις δείτε το σταυρόνημα, έχετε αυτές τις επιλογές:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

Μπορείτε επίσης να χρησιμοποιήσετε ένα μενού αντικειμένου: Δείτε [Δημιουργία νέων αντικειμένων] (/Creating%20new%20objects.md) για περισσότερες πληροφορίες.

#### Προσθέτοντας μια Περιοχή

Το OpenStreetMap επί του παρόντος δεν διαθέτει τύπο αντικειμένου ως "περιοχή" σε αντίθεση με άλλα συστήματα γεω-δεδομένων. Ο διαδικτυακός επεξεργαστής "iD" προσπαθεί να δημιουργήσει μια αφαίρεση περιοχής από τα υποκείμενα στοιχεία OSM που λειτουργεί καλά σε ορισμένες περιπτώσεις, ενώ σε άλλες όχι. Το Vespucci αυτή τη στιγμή δεν προσπαθεί να κάνει κάτι παρόμοιο, οπότε πρέπει να γνωρίζετε λίγο τον τρόπο με τον οποίο απεικονίζονται οι περιοχές:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Βελτίωση της Γεωμετρίας της Διαδρομής

Εάν κάνετε μεγέθυνση αρκετά σε μία επιλεγμένη διαδρομή, θα δείτε ένα μικρό "x" στην μέση της διαδρομής των τμημάτων που είναι αρκετά μακριά. Σέρνοντας το "x" θα δημιουργηθεί ένας κόμβος στην διαδρομή σε εκείνο το σημείο. Σημείωση: για να αποφευχθεί κατά λάθος δημιουργία κόμβων, η περιοχή ανοχής αφής για αυτή τη λειτουργία είναι αρκετά μικρή.

#### Αποκοπή, Αντιγραφή και Επικόλληση

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Αποτελεσματική Προσθήκη Διευθύνσεων

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Η πρόβλεψη αριθμού κατοικίας απαιτεί συνήθως τουλάχιστον δύο αριθμούς κατοικιών σε κάθε πλευρά του δρόμου για να τεθεί σε λειτουργία, όσο περισσότεροι αριθμοί υπάρχουν στα δεδομένα τόσο το καλύτερο.

Consider using this with one of the [Auto-download](#download) modes.  

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

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Εάν θέλετε να αποθηκεύσετε την εργασία σας και δεν έχετε πρόσβαση στο Internet, μπορείτε να αποθηκεύσετε σε ένα αρχείο .osm συμβατό με το JOSM και είτε να μεταφορτώσετε αργότερα με το Vespucci είτε με το JOSM. 

#### Επίλυση συγκρούσεων στις μεταφορτώσεις

Το Vespucci έχει έναν απλό επιλυτή συγκρούσεων. Ωστόσο, εάν υποψιάζεστε ότι υπάρχουν σημαντικά ζητήματα με τις επεξεργασίες σας, εξαγάγετε τις αλλαγές σας σε ένα αρχείο .osc ("Εξαγωγή" μενού αντικειμένου στο μενού "Μεταφορά") και διορθώστε τα και ανεβάστε τα με το JOSM. Δείτε την λεπτομερή βοήθεια στο [επίλυση συγκρούσεων] (../en/Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Το Vespucci υποστηρίζει τη λήψη, την σχολιασμό και το κλείσιμο των Σημειώσεων OSM (πρώην OSM Bugs) και την αντίστοιχη λειτουργικότητα για τα "Σφάλματα" που παράγονται από το εργαλείο [διασφάλιση ποιότητας του OSMOSE](http://osmose.openstreetmap.fr/en/map/). Και τα δύο πρέπει είτε να είναι κατεβαίνουν λεπτομερώς είτε μπορείτε να χρησιμοποιήσετε τη δυνατότητα αυτόματης λήψης για να αποκτήσετε πρόσβαση στα στοιχεία της περιοχής σας. Μόλις επεξεργαστείτε ή κλείσετε, μπορείτε είτε να μεταφορτώσετε το σφάλμα ή τη σημείωση αμέσως ή να τα ανεβάσετε όλα ταυτόχρονα. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Φιλτράρισμα

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Εσωτερική λειτουργία

Η χαρτογράφηση σε εσωτερικούς χώρους είναι δύσκολη λόγω του μεγάλου αριθμού αντικειμένων που συχνά επικαλύπτουν ο ένας τον άλλον. Το Vespucci έχει μια ειδική λειτουργία εσωτερικού χώρου που σας επιτρέπει να φιλτράρετε όλα τα αντικείμενα που δεν βρίσκονται στο ίδιο επίπεδο και τα οποία θα προσθέσουν αυτόματα το τρέχον επίπεδο σε νέα αντικείμενα που έχουν δημιουργηθεί.

Η λειτουργία μπορεί να ενεργοποιηθεί πατώντας παρατεταμένα το στοιχείο κλειδώματος, δείτε στο [Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας] (#κλειδαριά) και επιλέγοντας την αντίστοιχη καταχώρηση μενού.

<a id="c-mode"></a>

## Λειτουργία-C

Στη Λειτουργία-C εμφανίζονται μόνο αντικείμενα που έχουν μια προειδοποιητική σημαία σήμανσης, γεγονός που καθιστά εύκολο το εντοπισμό αντικειμένων που παρουσιάζουν συγκεκριμένα προβλήματα ή ταιριάζουν με ρυθμιζόμενους ελέγχους. Αν επιλεγεί ένα αντικείμενο και ο Επεξεργαστής Ιδιοτήτων ξεκινήσει στη Λειτουργία-C, θα εφαρμοστεί αυτόματα η καλύτερη προεπιλεγμένη προρύθμιση.

Η λειτουργία μπορεί να ενεργοποιηθεί πατώντας παρατεταμένα το στοιχείο κλειδώματος, δείτε στο [Κλείδωμα, ξεκλείδωμα, εναλλαγή λειτουργίας] (#κλειδαριά) και επιλέγοντας την αντίστοιχη καταχώρηση μενού.

### Διαμόρφωση των ελέγχων

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

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
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Φίλτρα

### Ετικέτα βασισμένη σε φίλτρο

Το φίλτρο μπορεί να ενεργοποιηθεί από το κύριο μενού, και μπορεί να αλλάξει πατώντας το εικονίδιο του φίλτρου. Περισσότερη τεκμηρίωση μπορείτε να βρείτε εδώ [Φίλτρο ετικέτας] (Tag% 20filter.md).

### Προρύθμιση βασισμένη σε φίλτρο

Μια εναλλακτική λύση στα παραπάνω αντικείμενα φιλτράρονται είτε σε μεμονωμένες προεπιλογές είτε σε προκαθορισμένες ομάδες. Χτυπώντας στο εικονίδιο του φίλτρου θα εμφανιστεί ένα προκαθορισμένο παράθυρο επιλογής παρόμοιο με αυτό που χρησιμοποιείται κάπου αλλού στο Vespucci. Οι μεμονωμένες προεπιλογές μπορούν να επιλεγούν με κανονικό κλικ, οι προκαθορισμένες ομάδες με παρατεταμένο κλικ (το κανονικό κλικ μπαίνει στην ομάδα). Περισσότερες οδηγίες μπορείτε να βρείτε εδώ [Προκαθορισμένο φίλτρο](../en/Preset%20filter.md).

## Προσαρμογή του Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

<a id="layers"></a>

### Layer settings

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Σύνθετες προτιμήσεις

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Reporting and Resolving Issues

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


