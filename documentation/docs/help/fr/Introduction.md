# Introduction à Vespucci

Vespucci est un éditeur OpenStreetMap complet qui supporte la plupart des opérations que les éditeur de bureau fournissent. Il a été testé avec succès sur Android de la version 2.3 à 7.0 et sur diverses variantes basés sur AOSP. Un mot d'avertissement : bien que les appareils mobiles ont rattrapé leur retard, les appareils les plus anciens ont une mémoire disponible très limité et ont tendance à être assez lent. Vous devez prendre cela en compte quand vous utilisez Vespucci et garder, par exemple, les aires que vous éditez dans une taille raisonnable.   

## Première utilisation

Au démarrage Vespucci affiche le dialogue "Télécharger autre endroit"/"Télécharger une zone". Si des coordonnées sont affichées et que vous voulez télécharger immédiatement, sélectionnez l'option correspondante et donnez le rayon autour de l'endroit que vous voulez télécharger. Ne sélectionnez pas une zone trop grande sur un appareil lent. 

Ou alors, fermez le dialogue en tapant le bouton "Aller à la carte", zoomez/déplacez jusqu'à l'endroit que vous voulez éditer, et téléchargez les données à partir de là (voir ci-dessous "Éditer avec Vespucci"). 

## Éditer avec Vespucci

Selon la taille de votre écran et l'age de l'appareil, les actions d'édition sont accessibles par des icônes dans la barre du haut, par un menu déroulant sur la droite de la barre du haut, par la barre du bas (si présente), ou par la touche menu.

<a id="download"></a>

### Télécharger des données OSM

Selectionner soit l'icone de transfert ![Transfer](../images/menu_transfer.png), soit l'onglet Transfet dans le menu. Cela affiche sept options : 

* **Télécharger la vue courante** - télécharge la zone visible à l'écran et remplace les données existantes *(nécessite une connexion réseau)*
* **Ajouter les données de la vue courante** - télécharge la zone visible à l'écran et la fusionne avec les données existantes *(nécessite une connexion réseau)*
* **Télécharger un autre endroit** - affiche un formulaire permettant de rechercher un lieu, d'entrer des coordonnées directement, ou d'utiliser la position actuelle pour télécharger une zone autour de ce point *(nécessite une connexion réseau)*
* **Envoyer les données au serveur OSM** - envoie les modification vers OpenStreetMap *(nécessite d'être authentifié)* *(nécessite une connexion réseau)*
* **Téléchargement automatique** - télécharge automatiquement la zone autour de la position courante *(nécessite une connexion réseau)* *(nécessite le GPS)*
* **Fichier...** - sauvegarde ou ouvre les données OSM dans des fichiers en local
* **Notes/Bugs** télécharge (automatiquement et manuellement) les Notes OSM et les "Bugs" des outils d'assurance qualité (OSMOSE pour l'instant) *(nécessite une connexion réseau)*

La plus simple manière de télécharger des données est de zoomer et se déplacer vers le lieu que vous voulez éditer, puis de sélectionner "télécharger la vue courante". Vous pouvez zoomer en utilisant le geste pincer/agrandir, avec les boutons de zoom, ou avec les boutons de volume de l'appareil. Vespucci va alors télécharger les données de la vue courante. Il n'y a pas besoin d'être authentifié pour télécharger les donnes sur votre appareil. 

### Éditer

<a id="lock"></a>

#### Lock, unlock, mode switching

Pour éviter des modifications accidentelles Vespucci démarre en mode "verrouillé", lequel ne permet que de zoomer et de se déplacer sur la carte. Appuyez sur l’icône ![verrouillé](../images/locked.png) pour débloquer l'édition. 

A long press on the lock icon will display a menu currently offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Simple pression, double pression, et pression longue

Par défaut, les noeuds et les voies selectionnables on une zone orange autour d'eux indiquant approximativement ou vous devez toucher pour sélectionner un objet. Vous avez trois options :

*Simple appui : Sélectionner l'objet. 
    * Un nœud/Chemin isolé est mis en évidence immédiatement.
    * Cependant, si vous essayer de sélectionner un objet et que Vespucci détermine que la sélection pourrai déterminer plusieurs objets, il présente un menu de sélection, vous permettant de choisir l'objet que vous souhaitez sélectionner.
    * Les objets sélectionnés sont mis en évidence en jaune.
    * Pour de plus amples informations voir [Selection de Noeud](../fr/Node%20selected.md),[Sélection de voie](../fr/Way%20selected.md) et [Sélection de relation](../fr/Relation%20selected.md).
*Double appui : Commencer[Mode Multiple sélection](../fr/Multiselect.md)
*Appui long : Créer une "croix", vous permettant d'ajouter des noeuds, voir plus bas et [Créer de nouveaux objets](../fr/Creating%20new%20objects.md)    

Il est conseillé de zoomer plus quand vous éditez une zone avec une grande densité de données.

Vespucci a un bon système d'"annuler/refaire" donc n'ayez pas peur d'expérimenter sur votre appareil. Cela dit, n'envoyez pas de données de test au serveur.

#### Sélectionner / Désélectionner (simple pression et "menu sélection")

Toucher un objet pour le sélectionner et le mettre en évidence. Toucher l'écran dans une région vide le désélectionnera. Si vous avez sélectionné un objet et que vous avez besoin de sélectionner autre chose, toucher simplement l'objet en question, il n'y a pas besoin de désélectionner au préalable . Un double appui sur un objet démarera le[Mode multiselection]../en/Multiselect.md).

Notez que si vous essayez de sélectionner un objet et que Vespucci détermine que la sélection peut signifier des objets multiples (tel q'un noeud ou une voie ou d'autre objets se chevauchant), il vous présentera un menu de sélection : Appuiez sur l'objet que vous souhaitez sélectionner et cet objet est sélectionné. 

Les objets sélectionnés sont indiqué par une bordure jaune mince. La bordure jaune peut être dure à repérer, dépendant de la carte en arrière plan et du niveau de zoom. Une fois la sélection faite, vous verrez une notification confirmant la sélection.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](../en/Node%20selected.md), [Way selected](../en/Way%20selected.md) and [Relation selected](../en/Relation%20selected.md).

#### Objets selectionnés : Edition des tags

Un deuxième appui sur l'objet selectioné ouvre l'éditeur de tag et vous pouvez editer les tags associés à l'objet

Notez que pour les objets se chevauchant (tel qu' un nœud sur une voie) le menu de sélection remonte une seconde fois. Sélectionner le même objet fait apparaître l'éditeur de tag; sélectionner un autre objet sélectionne simplement un autre objet.

### Objet sélectionné : Déplacer un nœud ou une voie

Une fois que vous avez sélectionné un objet, il peut être déplacé. Notez que les objets peuvent être déplacé uniquement lorsqu'ils sont sélectionnés.Glissez simplement (dans la zone de tolérance) l'objet sélectionné pour le déplacer Si vous sélectionnez l'option zone élargie dans les préférences vous aurez une zone plus large autour du nœud sélectionné, ce qui rendra plus facile le positionnement de l'objet 

#### Ajouter un nouveau nœud/point ou chemin (appui long)

Faites un appui long là ou vous voulez positionner votre nœud ou que votre chemin commence. Vous verrez un symbole noir "réticule". 
* Si vous voulez créer un nouveau nœud (non connecté à un objet), cliquez loin d'un objet existant.
* Si vous voulez rallonger un chemin, cliquez dans la "zone de tolérance" du chemin (ou un nœud sur le chemin). La zone de tolérance est indiquée par les zones autour du nœud ou du chemin

Une fois que vous pouvez voir le symbole "réticule", vous avez trois options

* Touch in the same place.
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* Touch another place. Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is  located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](../en/Creating%20new%20objects.md) for more information.

#### Adding an Area

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Améliorer la géométrie du chemin

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Couper, copier et coller

Vous pouvez couper ou copier des nœuds ou chemins sélectionnés, puis les coller à une ou plusieurs reprises à de nouveaux endroits. Couper conservera l'identifiant OSM et l'historique de l'élément. Pour coller, touchez de façon prolongée le lieu où vous voulez coller (qui sera marqué par une croix), puis sélectionnez "Coller" dans le menu.

#### Ajouter efficacement des adresses

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

La prédiction du numéro de bâtiment nécessite normalement pour fonctionner qu'au moins deux numéros de chaque côté de la voie soient déjà présents. Plus nombreux sont les numéros déjà renseignés, meilleure est la prédiction.

Consider using this with the [Auto-download](#download) mode.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci en mode verrouillé

Quand le cadenas rouge est affiché, les fonctions d'édition sont indisponibles. Un appui long sur ou à proximité d'un objet d'OSM affichera un écran d'information détaillée sur cet objet.

### Enregistrer vos modifications

*(nécessite un accès au réseau)*

Utilisez le même bouton ou menu que pour le téléchargement des données et sélectionnez "Envoyer les données au serveur OSM".

Vespucci accepte l'identification par OAuth ou par nom d'utilisateur et mot de passe. OAuth est à préférer car il n'envoie pas le mot de passe en clair sur le réseau.

Les versions récentes de Vespucci activent OAuth par défaut. Lors de votre premier envoi de données, une page du site OSM s'affiche. Après que vous vous êtes identifié (avec une connexion chiffrée), il vous sera demandé d'autoriser Vespucci à éditer les données en utilisant votre compte. Vous pouvez aussi autoriser l'accès Oauth à votre compte avant toute édition via le menu Outils > Autoriser OAuth.

Si vous voulez sauvegarder vos modifications alors que vous n'avez pas accès à internet, il est possible de les enregistrer dans un fichier ".osm"compatible avec JOSM et de les envoyer au serveur plus tard avec Vespucci ou JOSM. 

#### Résoudre des conflits lors de l'envoi

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict%20resolution.md).  

## Utiliser le GPS

Vous pouvez utiliser Vespucci pour créer une trace GPX et l'afficher à l'écran. Vous pouvez aussi afficher votre position GPS actuelle (avec l'option "Afficher ma position" dans le menu GPS) ou recentrer en continu l'écran sur votre position (option "Recentrer sur ma position"). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue

<a id="indoor"></a>

## Indoor mode

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created their.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

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

## Filters

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](../en/Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](../en/Preset%20filter.md).

## Customizing Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Advanced preferences

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


