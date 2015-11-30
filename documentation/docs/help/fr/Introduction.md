# Introduction à Vespucci

Vespucci est un éditeur OpenStreetMap complet qui supporte la plupart des opérations fournies par les éditeurs desktop. Il a été testé avec succès sur Google Andoid 2.3 à 6.0 et sur quelques variantes AOSP. Attention tout de même : bien que les possibilités des appareils portables ont rattrapé celles des desktop, ils  disposent (surtout les plus vieux) de peu de mémoire et ont tendance à être lents. Gardez cela en tête pendant votre utilisation de Vespucci, par exemple n'éditez pas une trop grande zone d'un coup. 

## Première utilisation

Au démarrage Vespucci affiche le dialogue "Télécharger autre endroit"/"Télécharger une zone". Si des coordonées sont affichées et que vous voulez télécharger immédiatement, sélectionez l'option correspondante et donnez le rayon autour de l'endroit que vous voulez télécharger. Ne sélectionez pas une zone trop grande sur un appareil lent. 

Ou alors, fermez le dialogue en tapant le bouton "Aller à la carte", zoomez/déplacez justqu'à l'endroit que vous voulez éditer, et téléchargez les données à partir de là (voir ci-dessous "Éditer avec Vespucci"). 

## Éditer avec Vespucci

Selon la taille de votre écran et l'age de l'appareil, les actions d'édition sont accessibles par des icones dans la barre du haut, par un menu déroulant sur la droite de la barre du haut, par la barre du bas (si présente), ou par la touche menu.

### Télécharger des données OSM

Selectionez soit l'icône transfert ![](../images/menu_transfer.png)  soit "Transfer" dans le meny. Cela affiche sept options :

* **Télécharger la vue courante** - télécharge la zone visible à l'écran et remplace les données existantes *(nécessite une connection réseau)*
* **Ajouer les données de la vue courante** - télécharge la zone visible à l'écran et la fusione avec les données existantes *(nécessite une connection réseau)*
* **Télécharger un autre endroit** - affiche un formulaire permettant de rechercher un lieu, d'entrer des coordonnées directement, ou d'utiliser la position actuelle pour télécharger une zone autour de ce point *(nécessite une connection réseau)*
* **Envoyer les données au serveur OSM** - envoie les modification vers OpenStreetMap *(nécessite d'être authentifié)* *(nécessite une connection réseau)*
* **Téléchargement automatique** - télécharge automatiquement la zone autour de la position courante *(nécessite une connection réseau)* *(nécessite le GPS)*
* **Fichier...** - sauvegarde ou ouvre les données OSM dans des fichiers en local
* **Notes/Bugs** télécharge (automatiquement et manuellement) les Notes OSM et les "Bugs" des outils d'assurance qualité (OSMOSE pour l'instant) *(nécessite une connection réseau)*

La plus simple manière de télécharger des données est de zoomer et se déplacer vers le lieu que vous voulez éditer, puis de sélectioner "télécharger la vue courante". Vous pouvez zoomer en utilisant le geste pincer/agrandir, avec les botons de zoom, ou avec les boutons de volume du téléphone. Vespucci va alors télécharger les données de la vue courante. Il n'y a pas besoin d'être authentifié pour télécharger les donnes sur votre appareil. 

### Éditer

Pour éviter des modifications accidentelles Vespucci démarre en mode "bloqué", lequel ne permet que de zoomer et de se déplacer sur la carte. Appuyez sur l'icone ![bloqué](../images/locked.png) pour débloquer l'édition. Un appui long sur cette icône active le mode "modification des attributs uniquement" qui empèche de créer de nouveaux objets ou de modifier les géométries. Ce mode est indiqué avec une icône un peu plus blanche.

Par défaut, les noeuds et chemins sélectionables ont une marge orange indiquant approximativement où toucher pour sélectioner un objet. Si vous essayez de sélectioner un objet et que Vespucci vois que cela pourrais correspondre à plusieur objets, il affichera un menu de sélection. Les objets sélectionés sont en surbrillance jaune.

Il est conseillé de zoomer plus quand vous éditez une zone avec une grande densité de données.

Vespucci a un bon système d'"annuler/refaire" donc n'ayez pas peur d'expérimenter sur votre appareil. Cela dit n'envoyez pas de données de test au serveur.

#### Sélectioner / désélectioner

Touchez un objet pour le sélectioner et le mettre en surbrillance; un deuxième appui sur le même objet ouvre l'éditeur d'attributs de cet élément. Toucher l'écran dans une zone vide désélectione. Si vous avec sélectioné un objet et que vous voulez sélectioner autrechose, touchez simplement l'objet en question, il n'y a pas besoin de désélectioner au préalable. Un double appui sur un object démarre le [mode de sélection multiple](../en/Multiselect.md).

#### Ajouter un nouveau noeud ou chemin

Faites un appui long là ou vous voulez commencer. Vous verrez un symbole noir "réticule". Appuyer au même endroit crée un noeud, appuyer en dehors de la marge de sélection du noeud commence un chemin entre les deux positions. 

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


