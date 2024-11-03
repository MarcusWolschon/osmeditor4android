_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Introduction à Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Éditer avec Vespucci

Selon la taille de votre écran et l'âge de l'appareil, les actions d'édition sont accessibles par des icônes dans la barre du haut, par un menu déroulant sur la droite de la barre du haut, par la barre du bas (si présente), ou par la touche menu.

<a id="download"></a>

### Télécharger des données OSM

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

La manière la plus simple de télécharger des données est de zoomer et de se déplacer vers le lieu que vous voulez éditer, puis de sélectionner « Télécharger la vue courante ». Vous pouvez zoomer avec deux doigts, avec les boutons de zoom, ou avec les boutons de volume de l'appareil. Vespucci va alors télécharger les données de la vue courante. Il n'y a pas besoin d'être authentifié pour télécharger les données sur votre appareil. 

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Éditer

<a id="lock"></a>

#### Verrouillage, déverrouillage et changement de mode

Pour éviter des modifications accidentelles Vespucci démarre en mode « verrouillé », lequel ne permet que de zoomer et de se déplacer sur la carte. Appuyez sur l’icône ![verrouillé](../images/locked.png) pour débloquer l'édition. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Appui simple, appui double, et appui long

Par défaut, les nœuds et les voies sélectionnables sont entourés d'une zone orange qui indique approximativement où vous devez appuyer pour sélectionner cet objet. Vous avez trois options :

* Appui simple : sélectionner l'objet. 
    * Un nœud ou un chemin isolé est mis en évidence immédiatement.
    * Cependant, si vous essayez de sélectionner un objet et que Vespucci détermine que la sélection pourrait correspondre à plusieurs objets, il présente un menu de sélection qui vous permet de choisir l'objet que vous souhaitez sélectionner.
    * Les objets sélectionnés sont mis en évidence en jaune.
    * Pour de plus amples informations voir [Sélection de nœud](Node%20selected.md), [Sélection de voie](Way%20selected.md) et [Sélection de relation](Relation%20selected.md).
* Appui double : démarrer le [mode de sélection multiple](Multiselect.md).
* Appui long : créer une « croix » qui vous permet d'ajouter des nœuds, voir plus bas et [Créer de nouveaux objets](Creating%20new%20objects.md). Cela n'est possible que si le « mode simple » est désactivé.

Il est conseillé de zoomer plus quand vous éditez une zone avec une grande densité de données.

Vespucci a un bon système pour annuler ou refaire des actions donc n'ayez pas peur d'expérimenter sur votre appareil. Cela dit, n'envoyez pas de données de test au serveur.

#### Sélectionner et désélectionner (appui simple et « menu de sélection »)

Appuyez sur un objet pour le sélectionner et le mettre en évidence. Touchez l'écran dans une région vide le désélectionnera. Si vous avez sélectionné un objet et que vous avez besoin de sélectionner autre chose, appuyez simplement sur l'objet en question ; il n'y a pas besoin de désélectionner au préalable. Un appui double sur un objet démarrera le [Mode de sélection multiple](Multiselect.md).

Remarquez que si vous essayez de sélectionner un objet et que Vespucci détermine que la sélection peut correspondre à plusieurs objets (tel qu'un nœud ou une voie ou d'autre objets se chevauchant), il vous présentera un menu de sélection : appuyez sur l'objet que vous souhaitez sélectionner et cet objet sera sélectionné. 

Les objets sélectionnés sont indiqués par une bordure jaune mince. La bordure jaune peut être dure à repérer, en fonction de la carte en arrière plan et du niveau de zoom. Une fois la sélection faite, vous verrez une notification confirmant la sélection.

Une fois un objet sélectionné, vous verrez (soit sous la forme de boutons, soit sous la forme d'éléments de menu) un liste d'opérations supportées pour l'objet en question : pour plus d'information, voir [Sélection de nœud](Node%20selected.md), [Sélection de voie](Way%20selected.md) et [Sélection de relation](Relation%20selected.md).

#### Objets sélectionnés : édition des attributs

Un deuxième appui sur l'objet sélectionné ouvre l'éditeur d'attributs et vous pouvez modifier les attributs associés à l'objet

Remarquez que pour les objets se chevauchant (tel qu'un nœud sur une voie) le menu de sélection apparaît une seconde fois. Sélectionnez le même objet pour faire apparaître l'éditeur de tag ; sélectionnez un autre objet pour sélectionner simplement cet autre objet.

### Objet sélectionné : déplacer un nœud ou une voie

Une fois que vous avez sélectionné un objet, vous pouvez le déplacer. Notez que les objets ne peuvent être glissés/déplacés que lorsqu'ils sont sélectionnés. Faites simplement glisser l'objet sélectionné à proximité (c'est-à-dire dans la zone de tolérance de celui-ci) pour le déplacer. Si vous avez sélectionné la grande zone de glissement dans les [paramètres] (Preferences.md), vous obtenez une grande zone autour du nœud sélectionné qui facilite le positionnement de l'objet. 

#### Ajouter un nouveau nœud ou un nouveau point sur un chemin 

Au premier démarrage, l'appli se lance en « mode simple », ce que vous pouvez changer dans le menu principal en désélectionnant la case correspondante.

##### Mode simple

Appuyez sur le gros bouton vert flottant sur la carte pour afficher un menu. Après avoir sélectionné l'un des éléments, on vous demandera d'appuyer sur l'écran à l'emplacement où vous voulez créer l'objet, mais le déplacement et le zoom continuent de fonctionner si vous devez ajuster la vue de la carte. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Mode avancé (appui long)
 
Appuyez longuement à l'endroit où vous voulez créer un nouveau nœud (que ce soit pour créer uniquement un nœud ou pour démarrer un nouveau chemin) ou appuyez longuement sur un nœud existant. Vous verrez un symbole de type croix noire. 
* Si vous souhaitez que ce nœud soit non connecté à un objet, appuyez loin des objets existants.
* Si vous souhaitez que ce nœud soit rattaché à un nœud ou à un chemin existant, touchez dans la "zone de tolérance" du nœud ou du chemin (la zone de tolérance est indiquée par les zones autour d'un nœud ou d'un chemin).

Une fois que vous pouvez voir le symbole « réticule », vous avez trois options :

* _Appuyez normalement au même endroit._
    * Si la croix n'est pas proche d'un nœud, toucher à nouveau le même emplacement crée un nouveau nœud. Si vous êtes près d'un chemin (mais pas près d'un nœud), le nouveau nœud sera sur le chemin (et connecté au chemin).
   * Si le réticule est proche d'un nœud (c'est-à-dire dans la zone de tolérance du nœud), toucher le même emplacement sélectionne simplement le nœud et l'éditeur des attributs s'ouvre. Aucun nouveau nœud n'est créé. L'action est la même que la sélection ci-dessus.
* _Toucher normalement à un autre endroit._ Toucher un autre endroit (en dehors de la zone de tolérance du réticule) ajoute un segment de chemin de la position d'origine à la position actuelle. Si le réticule était proche d'un chemin ou d'un nœud, le nouveau segment sera connecté à ce nœud ou à ce chemin.

Il suffit de toucher l'écran à l'endroit où vous souhaitez ajouter d'autres nœuds. Pour terminer, touchez deux fois le nœud final. Si le nœud final est situé sur une voie ou un nœud, le segment sera automatiquement connecté à la voie ou au nœud. 

Vous pouvez aussi utiliser un élément du menu : voir [Créer de nouveau objets](Creating%20new%20objects.md) pour plus d'information.

#### Ajouter un polygone

OpenStreetMap n'a pour l'instant pas de type d'objet « polygone » contrairement aux autres systèmes de données géographiques. L'éditeur en ligne iD essaie de créer une abstraction de la zone à partir des éléments sous-jacents d'OSM, ce qui fonctionne dans certains cas, mais pas dans d'autres. Vespucci n'essaie pas de faire ça, donc vous devez savoir un peu comment les polygones sont représentés :

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Améliorer la géométrie du chemin

Si vous zoomez assez loin sur un chemin sélectionné vous verrez un petit « x » au milieu des segments du chemin qui sont assez longs. Déplacez le « x » pour créer un nœud du chemin à cet emplacement. Remarquez que pour éviter de créer des nœuds de manière accidentelle, la zone interactive de cette opération est assez petite.

#### Couper, copier et coller

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Ajouter efficacement des adresses

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

L'ajout de tags peut être fait en cliquant ![Adresse](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

La prédiction du numéro de bâtiment nécessite normalement pour fonctionner qu'au moins deux numéros de chaque côté de la voie soient déjà présents. Plus nombreux sont les numéros déjà renseignés, meilleure est la prédiction.

Consider using this with one of the [Auto-download](#download) modes.  

#### Ajouter des interdictions de tourner

Vespucci permet d'ajouter rapidement des interdictions de tourner. Si nécessaire il scindera les voies automatiquement et vous demandera de ré-sélectionner les éléments. 

* sélectionnez le chemin avec un attribut « highway » (les interdictions de tourner ne peuvent être ajoutés que pour les routes, si vous avez besoin de faire ça pour d'autres chemins, utilisez le mode générique « créer une relation ») ;
* sélectionnez « Ajouter une interdiction » dans le menu ;
* sélectionnez le nœud ou le chemin « via » (seuls les éléments « via » possibles auront une zone interactive) ;
* sélectionnez le chemin « to » (il est possible de revenir en arrière et appuyer sur l'élément « from », auquel cas Vespucci supposera que vous voulez ajouter une interdiction de faire demi-tour) ;
* indiquez le type d'interdiction.

### Vespucci en mode verrouillé

Quand le cadenas rouge est affiché, les fonctions d'édition sont indisponibles. Un appui long sur ou à proximité d'un objet d'OSM affichera un écran d'information détaillée sur cet objet.

### Enregistrer vos modifications

*(nécessite un accès au réseau)*

Utilisez le même bouton ou menu que pour le téléchargement des données et sélectionnez « Envoyer les données au serveur OSM ».

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Si vous voulez sauvegarder vos modifications alors que vous n'avez pas accès à internet, il est possible de les enregistrer dans un fichier « .osm » compatible avec JOSM et de les envoyer au serveur plus tard avec Vespucci ou JOSM. 

#### Résoudre des conflits lors de l'envoi

Vespucci dispose d'un outil de résolution des conflits simples. Cependant, si vous soupçonnez un problème important avec vos modifications, exportez-les dans un fichier « .osc » (menu « Transfert des données » puis « Exporter les modifications ») pour les corriger dans JOSM avant de les envoyer. Une aide détaillée est disponible sur [la résolution de conflit](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

 ## En utilisant les traces GPS et GPX

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci supporte le téléchargement, les commentaires et la fermeture des notes OSM (précédemment connues sous le nom de bugs OSM) et des fonctions équivalentes pour les « bugs » produits par [l'outil d'assurance qualité Osmose](http://osmose.openstreetmap.fr/fr/map/). Les deux doivent être téléchargés explicitement ou vous pouvez utiliser la fonction de téléchargement automatique pour accéder aux éléments proches de vous. Une fois modifié ou fermé, vous pouvez soit envoyer un bug ou une note immédiatement, soit les envoyer tous d'un coup. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtres

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Mode intérieur

La cartographie d'intérieur est difficile à cause du grand nombre d'objets qui se recouvrent souvent. Vespucci a un mode intérieur dédié qui vous permet de filtrer tous les objets du même niveau et qui ajoutera automatiquement le niveau actuel aux nouveaux objets créés.

Le mode peut être activé par un appui long sur le cadenas, voir [Lock, unlock, mode switching](#lock) et en sélectionnant l'entrée du menu correspondant.

<a id="c-mode"></a>

## Mode C

En mode C seuls les objets avec un avertissement sont affichés, ce qui rend facile la détection des objets qui ont leur propre problème ou qui correspondent à des tests configurables. Si un objet est sélectionné et que l'éditeur de propriétés est démarré dans le mode C, le modèle d'attributs le plus proche sera automatiquement appliqué.

Le mode peut être activé par un appui long sur le cadenas, voir [Lock, unlock, mode switching](#lock) et en sélectionnant l'entrée du menu correspondant.

### Configurer des tests

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Entrées de demande de nouveaux relevés de terrain

Les entrées de demande de nouveaux relevés de terrain ont les propriétés suivantes :

* **Clef** : la clef d'un attribut qui vous intéresse.
* **Valeur** : la valeur qu'un attribut qui vous intéresse devrait avoir. Si elle est vide, la valeur est ignorée.
* **Âge** : nombre de jours après le dernier changement de l'élément après lequel il devrait être vérifié. Si un attribut _check_date_ est présent il sera utilisé, sinon ce sera la date de la version actuelle. Mettre la valeur à zéro fera que le test n'utilisera que la clef et la valeur.
* **Expression régulière** : si la case est cochée, la **Valeur** est traitée comme une expression régulière JAVA.

**Clef** et **Valeur** doivent correspondre aux attributs _existants_ de l'objet en question.

Le groupe _Annotations_ des modèles standards contient un élément qui ajoutera automatiquement l'attribut _check_date_ avec la date actuelle.

#### Entrées de vérification

Les entrées de vérification ont les deux propriétés suivantes :

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

Cette vérification fonctionne en déterminant d'abord le modèle correspondant puis en vérifiant que la **Clef** est une clef « recommandée » pour cet objet d'après l'attribut, **Attributs facultatifs requis » étendra la vérification aux attributs « facultatifs » de l'objet. Remarquez qu'actuellement les modèles liés ne sont pas vérifiés.

## Filtres

### Filtres basés sur les attributs

On peut activer le filtre depuis le menu principal, puis on peut le changer en appuyant sur l'icône de filtre. Plus de documentation sur [Filtre d'attribut](Tag%20filter.md).

### Filtre basé sur les modèles

Autrement, les objets peuvent être filtrés suivant des modèles individuels ou des groupes de modèles. Appuyez sur l'icône de filtre pour afficher un dialogue de sélection de modèles similaire à ceux utilisés ailleurs dans Vespucci. On peut sélectionner des modèles individuels en cliquant simplement dessus, et des groupes de modèles en cliquant longuement (un clic normal ouvre le groupe). Plus de documentation sur [Fitre de modèle](Preset%20filter.md).

## Personnaliser Vespucci

De multiples aspects de l'application sont personnalisables. [Le site internet de Vespucci](https://vespucci.io/) contiens des informations complémentaires par rapport aux informations intégrée à l'appareil, notamment sur les personnalisations avancées.

<a id="layers"></a>

### Configuration des couches

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Types de calques disponibles :

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Plus d'informations sont disponibles dans la section de [l'affichage de la carte](Main%20map%20display.md).

#### Paramètres

* Garder l'écran allumé. Par défaut : désactivé.
* Zone élargie de déplacement des nœuds. Déplacer des nœuds sur un appareil tactile est problématique parce que vos doigts cachent l'emplacement actuel sur l'écran. Activer cette option fournira une zone large pour déplacer en étant décalé par rapport au centre (la sélection et les autres opérations continuent d'utiliser la même zone de tolérance). Par défaut : désactivé.

Vous pouvez trouver les descriptions complètes ici [Paramètres](Preferences.md)

#### Paramètres avancés

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

Une description complète est disponible ici [Paramètres avancés](Advanced%20preferences.md)

## Reporting and Resolving Issues

Si Vespucci plante, ou qu'il détecte un état incohérent, il vous demandera d'envoyer un rapport de plantage. Faîtes-le si cela arrive, mais une seule fois par situation spécifique. Si vous voulez donner plus d'informations ou ouvrir un rapport de bug pour une demande de fonctionnalités, faîtes-le ici : [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). La fonction « faire un retour » du menu principal ouvrira un nouveau ticket et inclura les informations utiles sur l'appareil et l'appli en vous évitant de les écrire vous-même.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


