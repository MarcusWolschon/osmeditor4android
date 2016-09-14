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

Par défaut, les nœuds et chemins sélectionnables ont une marge orange indiquant approximativement où toucher pour sélectionner l'objet. Si vous essayez de sélectionner un objet et que Vespucci détermine que cette sélection concerne plusieurs objets, il affichera un menu de sélection. Les objets sélectionnés sont en surbrillance jaune.

Il est conseillé de zoomer plus quand vous éditez une zone avec une grande densité de données.

Vespucci a un bon système d'"annuler/refaire" donc n'ayez pas peur d'expérimenter sur votre appareil. Cela dit n'envoyez pas de données de test au serveur.

#### Sélectioner / désélectioner

Touchez un objet pour le sélectioner et le mettre en surbrillance; un deuxième appui sur le même objet ouvre l'éditeur d'attributs de cet élément. Toucher l'écran dans une zone vide désélectione. Si vous avec sélectioné un objet et que vous voulez sélectioner autrechose, touchez simplement l'objet en question, il n'y a pas besoin de désélectioner au préalable. Un double appui sur un object démarre le [mode de sélection multiple](../en/Multiselect.md).

#### Ajouter un nouveau nœud ou chemin

Faites un appui long là ou vous voulez commencer. Vous verrez un symbole noir "réticule". Appuyer au même endroit crée un nœud, appuyer en dehors de la marge de sélection du nœud commence un chemin entre les deux positions. 

Touchez simplement l'écran aux endroits où vous voulez ajouter des nœuds au chemin. Pour finir, toucher le dernier nœud deux fois. Si les nœuds initial et final sont sur le même chemin, ils seront insérés automatiquement dans le chemin.

#### Déplacer un nœud ou un chemin

Les objets ne peuvent être déplacés que quand ils sont sélectionnés. Si vous cochez l'option Zone élargie dans les préférences, vous aurez une zone plus large autour du nœud sélectionné, à partir de laquelle le positionnement de l'objet sera plus facile. 

#### Améliorer la géométrie du chemin

Si vous zoomez assez, vous allez voir un petit "x" au milieu des segments de chemin qui sont suffisamment longs. Déplacer ce "x" créera à cet endroit un nouveau nœud sur le chemin. Notez que, pour éviter les créations accidentelles de nœuds, la tolérance au toucher pour cette opération est assez faible. 

#### Couper, copier et coller

Vous pouvez couper ou copier des nœuds ou chemins sélectionnés, puis les coller à une ou plusieurs reprises à de nouveaux endroits. Couper conservera l'identifiant OSM et l'historique de l'élément. Pour coller, touchez de façon prolongée le lieu où vous voulez coller (qui sera marqué par une croix), puis sélectionnez "Coller" dans le menu.

#### Ajouter efficacement des adresses

Vespucci propose une fonction "ajouter un tag d'adresse" qui essaie de rendre plus efficace la collecte des adresses. Elle peut être utilisée : 

* Après un appui prolongé : Vespucci ajoutera un nœud à cet endroit, essaiera de déterminer le numéro du bâtiment et ajoutera un tag d'adresse. Si le nœud se situe sur le chemin traçant un bâtiment, un tag "entrance=yes" y sera automatiquement ajouté. L'éditeur de tag s'ouvrira alors pour l'objet en question pour vous permettre de faire des ajout ou des changements.
* Dans le mode de sélection de nœud ou de chemin : Vespucci ajoutera les tags d'adresse comme ci-dessus et affichera l'éditeur de tags.
* Dans l'éditeur de tags.

La prédiction du numéro de bâtiment nécessite normalement pour fonctionner qu'au moins deux numéros de chaque côté de la voie soient déjà présents. Plus nombreux sont les numéros déjà renseignés, meilleure est la prédiction.

Vous pouvez utiliser cette fonction avec le mode "Téléchargement automatique".  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci en mode verrouillé

Quand le cadenas rouge est affiché, les fonctions d'édition sont indisponibles. Un appui long sur ou à proximité d'un objet d'OSM affichera un écran d'information détaillée sur cet objet.

### Enregistrer vos modifications

*(nécessite un accès au réseau)*

Utilisez le même bouton ou menu que pour le téléchargement des données et sélectionnez "Envoyer les données au serveur OSM".

Vespucci accepte l'identification par OAuth ou par nom d'utilisateur et mot de passe. OAuth est à préférer car il n'envoie pas le mot de passe en clair sur le réseau.

Les versions récentes de Vespucci activent OAuth par défaut. Lors de votre premier envoi de données, une page du site OSM s'affiche. Après que vous vous êtes identifié (avec une connexion chiffrée), il vous sera demandé d'autoriser Vespucci à éditer les données en utilisant votre compte. Vous pouvez aussi autoriser l'accès Oauth à votre compte avant toute édition via le menu Outils > Autoriser OAuth.

Si vous voulez sauvegarder vos modifications alors que vous n'avez pas accès à internet, il est possible de les enregistrer dans un fichier ".osm"compatible avec JOSM et de les envoyer au serveur plus tard avec Vespucci ou JOSM. 

#### Résoudre des conflits lors de l'envoi

Vespucci dispose d'un outil de résolution des conflits simples. Cependant, si vous soupçonnez un problème important avec vos modifications, exportez-les dans un fichier ".osc" (menu "Transfert des données" puis "Exporter les modifications") pour les corriger dans JOSM avant de les envoyer. Une aide détaillée est disponible sur [la résolution de conflit](../en/Conflict resolution.md).  

## Utiliser le GPS

Vous pouvez utiliser Vespucci pour créer une trace GPX et l'afficher à l'écran. Vous pouvez aussi afficher votre position GPS actuelle (avec l'option "Afficher ma position" dans le menu GPS) ou recentrer en continu l'écran sur votre position (option "Recentrer sur ma position"). 

Si cette option de recentrage est activée, déplacer manuellement l'écran ou éditer des données la désactivera. La flèche bleue indiquant votre position, dont seuls les contours étaient affichés, deviendra alors pleine. Pour revenir rapidement au mode Recentrage, il suffit de toucher la flèche ou de recocher l'option.

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


