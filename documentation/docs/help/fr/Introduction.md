_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too._

# Introduction à Vespucci

Vespucci est un éditeur OpenStreetMap complet qui prend en charge la plupart des opérations fournies par les éditeurs de bureau. Il a été testé sur Android 2.3 à 10.0 et diverses variantes basées sur AOSP. Attention cependant : alors que les possibilités des appareils ont rattrapées celles des ordinateurs de bureau, les appareils particulièrement anciens sont très limités en mémoire et tendent à être lents. Vous devriez prendre cela en compte lors de votre utilisation de Vespucci et garder, par exemple, la zone d'édition à une taille raisonnable.

## Éditer avec Vespucci

Selon la taille de votre écran et l'âge de l'appareil, les actions d'édition sont accessibles par des icônes dans la barre du haut, par un menu déroulant sur la droite de la barre du haut, par la barre du bas (si présente), ou par la touche menu.

<a id="download"></a>

### Télécharger des données OSM

Selectionnez soit l’icône de transfert ![Transfer](../images/menu_transfer.png), soit l'onglet Transfert dans le menu. Cela affiche sept options :

* **Télécharger la vue actuelle** - télécharge la zone visible à l'écran et la fusionne avec les données existantes *(nécessite une connectivité réseau ou une source de données hors ligne)*
* **Effacer et télécharger la vue actuelle** - efface toutes les données en mémoire puis télécharge la zone visible à l'écran *(nécessite une connectivité réseau)*
* **Envoyer les données sur le serveur OSM** - envoi les modifications effectuées à OpenStreetMap *(nécessite une authentification)* *(nécessite une connectivité réseau)*
* **Mettre à jour les données** - télécharge à nouveau les données pour toutes les zones et met à jour ce qui est en mémoire *(nécessite une connectivité réseau)*
* **Téléchargement automatique basé sur votre position réelle** - télécharge automatiquement une zone autour de votre position géographique actuelle *(nécessite une connectivité réseau ou des données hors ligne)* *(nécessite un GPS)*
* **Téléchargement automatique en suivant le déplacement et le niveau de zoom** - télécharge automatiquement les données pour la zone de carte en cours *(nécessite une connectivité réseau ou des données hors ligne)* *(nécessite un GPS)*
* **Fichier...** - enregistre ou charge des données OSM vers ou depuis des fichiers de l'appareil.
* **Note/Bugs...** - télécharge (automatiquement ou manuellement) les notes OSM et les "Erreurs" à partir des outils d'amélioration de la Qualité (actuellement uniquement OSMOSE) *(nécessite une connectivité réseau)*

La manière la plus simple de télécharger des données est de zoomer et de se déplacer vers le lieu que vous voulez éditer, puis de sélectionner « Télécharger la vue courante ». Vous pouvez zoomer avec deux doigts, avec les boutons de zoom, ou avec les boutons de volume de l'appareil. Vespucci va alors télécharger les données de la vue courante. Il n'y a pas besoin d'être authentifié pour télécharger les données sur votre appareil. 

Avec les paramètres par défaut, toutes les zones non téléchargées seront grisées contrairement à celles téléchargées, ceci afin d'éviter d'ajouter par inadvertance des objets en double dans des zones qui n'ont pas été encore chargées. Vous pouvez changer cela dans les [Paramètres avancés](Advanced%20preferences.md).

### Éditer

<a id="lock"></a>

#### Verrouillage, déverrouillage et changement de mode

Pour éviter des modifications accidentelles Vespucci démarre en mode « verrouillé », lequel ne permet que de zoomer et de se déplacer sur la carte. Appuyez sur l’icône ![verrouillé](../images/locked.png) pour débloquer l'édition. 

Appuyez longuement sur l'icône de verrouillage pour afficher un menu qui offre actuellement 4 options :

* **Normal** - le mode d'édition par défaut. De nouveaux objets peuvent être ajoutés, ceux existants modifiés, déplacés ou supprimés. Icône affichée : un cadenas blanc simple.
* **Attributs uniquement** - la sélection d'un objet existant ouvrira l'éditeur de propriétés, un appui long sur l'écran permettra d'ajouter des objets mais aucune autre opération de géométrie (par exemple déplacer un nœud) ne fonctionnera. Icône affichée : un cadenas blanc avec un "T" (pour Tag).
* **Adresse** - active le mode Adresse, un mode légèrement simplifié avec des actions spécifiques disponibles à partir du bouton "+" [Mode simple](../en/Simple%20actions.md). Icône affichée : un cadenas blanc avec un "A".
* **Intérieur** - active le mode Intérieur, voir [Mode Intérieur](#indoor). Icône affichée : un cadenas blanc avec un "I".
* **C-Mode** - active le C-Mode. Seuls les objets qui ont un drapeau d'avertissement seront affichés, voir [C-Mode](#c-mode). Icône affichée : un cadenas blanc avec un "C".

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

Voir [Créer de nouveaux objets dans le mode d'actions simples](Creating%20new%20objects%20in%20simple%20actions%20mode.md) pour plus d'informations.

##### Mode avancé (appui long)
 
Appuyez longuement à l'endroit où vous voulez créer un nouveau nœud (que ce soit pour créer uniquement un nœud ou pour démarrer un nouveau chemin) ou appuyez longuement sur un nœud existant. Vous verrez un symbole de type croix noire. 
* Si vous souhaitez que ce nœud soit non connecté à un objet, appuyez loin des objets existants.
* Si vous souhaitez que ce nœud soit rattaché à un nœud ou à un chemin existant, touchez dans la "zone de tolérance" du nœud ou du chemin (la zone de tolérance est indiquée par les zones autour d'un nœud ou d'un chemin).

Une fois que vous pouvez voir le symbole « réticule », vous avez trois options :

* _Appuyez normalement au même endroit._
    * Si la croix n'est pas proche d'un nœud, toucher à nouveau le même emplacement crée un nouveau nœud. Si vous êtes près d'un chemin (mais pas près d'un nœud), le nouveau nœud sera sur le chemin (et connecté au chemin).
   * Si le réticule est proche d'un nœud (c'est-à-dire dans la zone de tolérance du nœud), toucher le même emplacement sélectionne simplement le nœud et l'éditeur des attributs s'ouvre. Aucun nouveau nœud n'est créé. L'action est la même que la sélection ci-dessus.
* _Toucher normalement à un autre endroit._ Toucher un autre endroit (en dehors de la zone de tolérance du réticule) ajoute un segment de chemin de la position d'origine à la position actuelle. Si le réticule était proche d'un chemin ou d'un nœud, le nouveau segment sera connecté à ce nœud ou à ce chemin.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

Vous pouvez aussi utiliser un élément du menu : voir [Créer de nouveau objets](Creating%20new%20objects.md) pour plus d'information.

#### Ajouter un polygone

OpenStreetMap n'a pour l'instant pas de type d'objet « polygone » contrairement aux autres systèmes de données géographiques. L'éditeur en ligne iD essaie de créer une abstraction de la zone à partir des éléments sous-jacents d'OSM, ce qui fonctionne dans certains cas, mais pas dans d'autres. Vespucci n'essaie pas de faire ça, donc vous devez savoir un peu comment les polygones sont représentés :

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Améliorer la géométrie du chemin

Si vous zoomez assez loin sur un chemin sélectionné vous verrez un petit « x » au milieu des segments du chemin qui sont assez longs. Déplacez le « x » pour créer un nœud du chemin à cet emplacement. Remarquez que pour éviter de créer des nœuds de manière accidentelle, la zone interactive de cette opération est assez petite.

#### Couper, copier et coller

Vous pouvez couper ou copier des nœuds ou chemins sélectionnés, puis les coller à une ou plusieurs reprises à de nouveaux endroits. Couper conservera l'identifiant OSM et l'historique de l'élément. Pour coller, touchez de façon prolongée le lieu où vous voulez coller (qui sera marqué par un réticule), puis sélectionnez « Coller » dans le menu.

#### Ajouter efficacement des adresses

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

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

Vespucci accepte l'authentification par OAuth ou par nom d'utilisateur et mot de passe. OAuth est à préférer car il n'envoie pas le mot de passe en clair sur le réseau.

Les versions récentes de Vespucci activent OAuth par défaut. Lors de votre premier envoi de données, une page du site OSM s'affiche. Après vous être identifié (avec une connexion chiffrée), il vous sera demandé d'autoriser Vespucci à modifier les données en utilisant votre compte. Vous pouvez aussi autoriser l'accès Oauth à votre compte avant toute édition depuis le menu outils.

Si vous voulez sauvegarder vos modifications alors que vous n'avez pas accès à internet, il est possible de les enregistrer dans un fichier « .osm » compatible avec JOSM et de les envoyer au serveur plus tard avec Vespucci ou JOSM. 

#### Résoudre des conflits lors de l'envoi

Vespucci dispose d'un outil de résolution des conflits simples. Cependant, si vous soupçonnez un problème important avec vos modifications, exportez-les dans un fichier « .osc » (menu « Transfert des données » puis « Exporter les modifications ») pour les corriger dans JOSM avant de les envoyer. Une aide détaillée est disponible sur [la résolution de conflit](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notes et bugs

Vespucci supporte le téléchargement, les commentaires et la fermeture des notes OSM (précédemment connues sous le nom de bugs OSM) et des fonctions équivalentes pour les « bugs » produits par [l'outil d'assurance qualité Osmose](http://osmose.openstreetmap.fr/fr/map/). Les deux doivent être téléchargés explicitement ou vous pouvez utiliser la fonction de téléchargement automatique pour accéder aux éléments proches de vous. Une fois modifié ou fermé, vous pouvez soit envoyer un bug ou une note immédiatement, soit les envoyer tous d'un coup.

Sur la carte, les notes et les bugs sont représentés par une petite icône d'insecte ![Bug](../images/bug_open.png), les verts sont fermés/résolus, les bleus sont à vous, et les jaunes indiquent qu'ils sont toujours actifs et n'ont pas été changés. 

L'affichage des bugs Osmose fournit un lien vers l'objet affecté en bleu. Appuyez sur le lien pour sélectionner l'objet, centrer l'écran dessus et télécharger la zone en avance si nécessaire. 

### Filtres

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Erreur d'Osmose
* Avertissement d'Osmose
* Problème mineur d'Osmose
* Maproulette
* Personnalisé

<a id="indoor"></a>

## Mode intérieur

La cartographie d'intérieur est difficile à cause du grand nombre d'objets qui se recouvrent souvent. Vespucci a un mode intérieur dédié qui vous permet de filtrer tous les objets du même niveau et qui ajoutera automatiquement le niveau actuel aux nouveaux objets créés.

Le mode peut être activé par un appui long sur le cadenas, voir [Lock, unlock, mode switching](#lock) et en sélectionnant l'entrée du menu correspondant.

<a id="c-mode"></a>

## Mode C

En mode C seuls les objets avec un avertissement sont affichés, ce qui rend facile la détection des objets qui ont leur propre problème ou qui correspondent à des tests configurables. Si un objet est sélectionné et que l'éditeur de propriétés est démarré dans le mode C, le modèle d'attributs le plus proche sera automatiquement appliqué.

Le mode peut être activé par un appui long sur le cadenas, voir [Lock, unlock, mode switching](#lock) et en sélectionnant l'entrée du menu correspondant.

### Configurer des tests

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the [preferences](Preferences.md). 

La liste des entrées est coupée en deux, la première moitié liste les entrée « nouveau relevé de terrain » et la deuxième moité les « vérifications ». On peut modifier les entrées en cliquant dessus et le bouton de menu vert permet d'ajouter des entrées.

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
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Paramètres

* Garder l'écran allumé. Par défaut : désactivé.
* Zone élargie de déplacement des nœuds. Déplacer des nœuds sur un appareil tactile est problématique parce que vos doigts cachent l'emplacement actuel sur l'écran. Activer cette option fournira une zone large pour déplacer en étant décalé par rapport au centre (la sélection et les autres opérations continuent d'utiliser la même zone de tolérance). Par défaut : désactivé.

Vous pouvez trouver les descriptions complètes ici [Paramètres](Preferences.md)

#### Paramètres avancés

* Icône de nœud. Par défaut : activé.
* Toujours montrer le menu contextuel. Lorsqu'elle est activée, à chaque sélection le menu contextuel sera affiché. Sinon le menu n'est affiché que lorsque aucune sélection non-ambiguë n'est déterminée. Par défaut : désactivé (activé sur d'anciennes versions).
* Activer le thème clair. Sur les appareils modernes, cette option est activée par défaut. Vous pouvez l'activer sur des appareils Android plus anciens, mais le thème ne sera sans doute pas cohérent avec le reste du système. 

Une description complète est disponible ici [Paramètres avancés](Advanced%20preferences.md)

## Rapporter des problèmes

Si Vespucci plante, ou qu'il détecte un état incohérent, il vous demandera d'envoyer un rapport de plantage. Faîtes-le si cela arrive, mais une seule fois par situation spécifique. Si vous voulez donner plus d'informations ou ouvrir un rapport de bug pour une demande de fonctionnalités, faîtes-le ici : [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). La fonction « faire un retour » du menu principal ouvrira un nouveau ticket et inclura les informations utiles sur l'appareil et l'appli en vous évitant de les écrire vous-même.

Si vous voulez discuter de quelque chose en lien avec Vespucci, vous pouvez démarrer une discussion soit sur le [Google group de Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android), soit sur le [forum Android d'OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


