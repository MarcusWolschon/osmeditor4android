# Εισαγωγή στο Vespucci

Το Vespucci είναι ένας επεξεργαστής OpenStreetMap πλήρης χαρακτηριστικών που υποστηρίζει τις περισσότερες από τις λειτουργίες που παρέχουν οι επεξεργαστές σταθερών υπολογιστών. Έχει δοκιμαστεί επιτυχώς σε Googles Android 2.3 ως 6.0 και διάφορες παραλλαγές με βάση το AOSP. Ένα σημείο προσοχής: ενώ οι δυνατότητες κινητών συσκευών έχουν φτάσει στο επίπεδο των σταθερών ανταγωνιστών τους, οι εξαιρετικά παλιές συσκευές έχουν πολύ περιορισμένη διαθέσιμη μνήμη και τείνουν να είναι σχετικά αργές. Αυτό θα πρέπει να το λάβετε υπόψη σας όταν χρησιμοποιείτε το Vespucci και να κρατάτε, για παράδειγμα, το μέγεθος των περιοχών που επεξεργάζεστε σε ένα λογικό μέγεθος. 

## Πρώτη χρήση

Κατά την εκκίνηση το Vespucci σας εμφανίζει το παράθυρο διαλόγου "Λήψη άλλης τοποθεσίας"/"Φόρτωση Περιοχής". Αν προβάλλετε τις συντεταγμένες και θέλετε να κατεβάσετε απευθείας, μπορείτε να κάνετε την κατάλληλη επιλογή και να ρυθμίσετε την ακτίνα γύρω από την τοποθεσία που θέλετε να κατεβάσετε. Μην επιλέγετε μεγάλες περιοχές σε αργές συσκευές. 

Εναλλακτικα μπορείτε να κλείσετε το παράθυρο πατώντας το κουμπί "Μετάβαση στον χάρτη" και να μετακινήσετε και να εστιάσετε σε μια τοποθεσία που θέλετε να επεξεργαστείτε και να κατεβάσετε τότε τα δεδομένα (δείτε παρακάτω: "Επεξεργασία με το Vespucci").

## Επεξεργασία με το Vespucci

Ανάλογα με το μέγεθος οθόνης και την ηλικία της συσκευής σας, οι ενέργειες επεξεργασίας μπορεί να είναι διαθέσιμες είτε μέσω των εικονιδίων της μπάρας στην κορυφή, είτε μέσω του πτυσσόμενου μενού στα δεξιά της μπάρας, από την κάτω μπάρα (αν είναι παρούσα) είτε μέσω του κουμπιού μενού.

### Λήψη Δεδομένων OSM

Επιλέξτε είτε το εικονίδιο μεταφοράς ![](../images/menu_transfer.png) ή το στοιχείο μενού "Μεταφορά". Θα εμφανιστούν επτά επιλογές:

* **Λήψη τρέχουσας προβολής** - κατέβασμα της ορατής στην οθόνη περιοχής και αντικατάσταση κάθε υπάρχοντος δεδομένου *(απαιτείται συνδεσιμότητα δικτύου)*
* **Προσθήκη της τρέχουσας προβολής στη λήψη** - κατέβασμα της ορατής στην οθόνη περιοχής και συγχώνευσή της με υπάρχοντα δεδομένα *(απαιτείται συνδεσιμότητα δικτύου)*
* **Λήψη άλλης τοποθεσίας** - εμφανίζει μια φόρμα που σας επιτρέπει να εισαγάγετε συντεταγμένες, να ψάξετε για μια τοποθεσία ή να χρησιμοποιήσετε την τρέχουσα θέση, και έπειτα να κατεβάσετε μια περιοχή γύρω από αυτή την τοποθεσία *(απαιτείται συνδεσιμότητα δικτύου)*
* **Αποστολή δεδομένων στον διακομιστή OSM** - ανέβασμα επεξεργασιών στο OpenStreetMap *(απαιτείται πιστοποίηση)* *(απαιτείται συνδεσιμότητα δικτύου)*
* **Αυτόματη λήψη** - αυτόματο κατέβασμα μιας περιοχής *(απαιτείται συνδεσιμότητα δικτύου)* *(απαιτείται GPS)*
* **Αρχείο...** - αποθήκευση και φόρτωση δεδομένων OSM σε/από αρχεία στη συσκευή.
* **Σημείωση/Σφάλματα...** - λήψη (αυτόματα και χειροκίνητα) Σημειώσεων OSM και "Σφαλμάτων" από εργαλεία QA (τώρα OSMOSE) *(απαιτείται συνδεσιμότητα δικτύου)*

Ο ευκολότερος τρόπος να κατεβάσετε δεδομένα στη συσκευή είναι να εστιάσετε και να μετακινηθείτε στην τοποθεσία που θέλετε να επεξεργαστείτε και μετά να επιλέξετε "Λήψη τρέχουσας τοποθεσίας". Μπορείτε να εστιάσετε με χειρονομίες, με τα κουμπιά εστίασης ή τα κουμπιά ελέγχου έντασης ήχου του τηλεφώνου. Το Vespucci μετά θα κατεβάσει τα δεδομένα της τρέχουσας προβολής. Δεν χρειάζεται πιστοποίηση για τη λήψη δεδομένων στη συσκευή σας.

### Επεξεργασία

Για την αποφυγή κατά λάθος επεξεργασίας το Vespucci αρχίζει σε "κλειδωμένη" λειτουργία, η οποία επιτρέπει μόνο την εστίαση και τη μετακίνηση του χάρτη. Πατήστε το εικονίδιο ![Locked](../images/locked.png) για να ξεκλειδώσετε την οθόνη. Ένα συνεχόμενο πάτημα στο εικονίδιο λουκέτου θα ενεργοποιήσει τη λειτουργία "Μόνο επεξεργασία ετικετών" που δε θα σας επιτρέψει να δημιουργείτε νέα αντικείμενα ή να αλλάζετε τη γεωμετρία των αντικειμένων. Αυτή η λειτουργία υποδεικνύεται με ένα λίγο διαφορετικό εικονίδιο με λευκό λουκέτο.

Από προεπιλογή, οι επιλέξιμοι κόμβοι και δρόμοι έχουν μία πορτοκαλιά περιοχή γύρω τους που δείχνει αδρά που πρέπει να αγγίξετε για να επιλέξετε ένα αντικείμενο. Αν προσπαθήσετε να επιλέξετε ένα αντικείμενο και το Vespucci κρίνει ότι η επιλογή ίσως σήμαινε πολλαπλά αντικείμενα, θα παρουσιάσει ένα μενού επιλογής. Τα επιλεγμένα αντικείμενα επισημαίνονται με κίτρινο χρώμα.

Είναι καλή στρατηγική να εστιάζστε αν επιχειρείτε να επεξεργαστείτε μια περιοχή υψηλής πυκνότητας.

Το Vespucci έχει ένα καλό σύστημα "αναίρεσης/επανάληψης", οπότε μην φοβάστε να πειραματιστείτε στη συσκευή σας, ωστόσο μην αποστέλλετε και αποθηκεύετε καθαρά δοκιμαστικά δεδομένα.

#### Επιλογή / Απο-επιλογή

Αγγίξτε ένα αντικείμενο για να το επιλέξετε και να το επισημάνετε, ένα δεύτερο άγγιγμα στο ίδιο αντικείμενο ανοίγει τον επεξεργαστή ετικετών στο στοιχείο. Αγγίζοντας την οθόνη σε μια άδεια περιοχή θα αποεπιλέξετε. Αν έχετε επιλέξει ένα αντικείμενο και χρειάζεστε να επιλέξετε κάτι άλλο, απλώς αγγίξτε το εν λόγω αντικείμενο, δεν χρειάζεται να αποεπιλέξετε πρώτα. Με διπλό άγγιγμα σε ένα αντικείμενο θα αρχίσει η [λειτουργία Πολλαπλής επιλογής](../el/Multiselect.md)

#### Προσθήκη νέου Κόμβου/Σημείου ή Δρόμου

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol. Touching the same location again creates a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial and  end nodes are located on a way, they will be inserted into the way automatically.

#### Moving a Node or Way

Objects can be dragged/moved only when they are selected. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Improving Way Geometry

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Cut, Copy & Paste

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efficiently Adding Addresses

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci in "locked" mode

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Saving Your Changes

*(requires network connectivity)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict resolution.md).  

## Using GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## Customizing Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Advanced preferences

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


