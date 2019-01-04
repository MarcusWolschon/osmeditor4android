# Introduction à Vespucci

Vespucci est un éditeur OpenStreetMap complet qui supporte la plupart des opérations que les éditeurs de bureau fournissent. Il a été testé avec succès sur Android de la version 2.3 à 7.0 et sur diverses variantes basées sur AOSP. Un mot d'avertissement : bien que les appareils mobiles aient rattrapé leur retard, les appareils les plus anciens ont une mémoire disponible très limitée et ont tendance à être assez lents. Vous devez prendre cela en compte quand vous utilisez Vespucci et utiliser, par exemple, des aires de taille raisonnable pendant l'édition. 

## Première utilisation

Au démarrage Vespucci affiche la boîte de dialogue « Télécharger à un autre endroit » ou « Télécharger une zone ». Si des coordonnées sont affichées et que vous voulez télécharger immédiatement, sélectionnez l'option correspondante et donnez le rayon autour de l'endroit que vous voulez télécharger. Ne sélectionnez pas une zone trop grande sur un appareil lent. 

Ou alors, fermez le dialogue en appuyant sur le bouton « Aller à la carte », zoomez et déplacez-vous jusqu'à l'endroit que vous voulez éditer, puis téléchargez les données à partir de là (voir ci-dessous « Éditer avec Vespucci »).

## Éditer avec Vespucci

Selon la taille de votre écran et l'âge de l'appareil, les actions d'édition sont accessibles par des icônes dans la barre du haut, par un menu déroulant sur la droite de la barre du haut, par la barre du bas (si présente), ou par la touche menu.

<a id="download"></a>

### Télécharger des données OSM

Selectionnez soit l’icône de transfert ![Transfer](../images/menu_transfer.png), soit l'onglet Transfert dans le menu. Cela affiche sept options :

* **Télécharger la vue courante** : télécharge la zone visible à l'écran et remplace les données existantes *(nécessite une connexion réseau)*
* **Ajouter les données de la vue courante** : télécharge la zone visible à l'écran et la fusionne avec les données existantes *(nécessite une connexion réseau)*
* **Télécharger à un autre endroit** : affiche un formulaire permettant de rechercher un lieu, d'entrer des coordonnées directement, ou d'utiliser la position actuelle pour télécharger une zone autour de ce point *(nécessite une connexion réseau)*
* **Envoyer les données au serveur OSM** : envoie les modifications vers OpenStreetMap *(nécessite d'être authentifié)* *(nécessite une connexion réseau)*
* **Téléchargement automatique** : télécharge automatiquement la zone autour de la position courante *(nécessite une connexion réseau)* *(nécessite le GPS)*
* **Fichier…** : sauvegarde ou ouvre les données OSM dans des fichiers locaux
* **Notes/Bugs…** : télécharge (automatiquement et manuellement) les Notes OSM et les « bugs » des outils d'assurance qualité (Osmose pour l'instant) *(nécessite une connexion réseau)*

La manière la plus simple de télécharger des données est de zoomer et de se déplacer vers le lieu que vous voulez éditer, puis de sélectionner « Télécharger la vue courante ». Vous pouvez zoomer avec deux doigts, avec les boutons de zoom, ou avec les boutons de volume de l'appareil. Vespucci va alors télécharger les données de la vue courante. Il n'y a pas besoin d'être authentifié pour télécharger les données sur votre appareil. 

### Éditer

<a id="lock"></a>

#### Verrouillage, déverrouillage et changement de mode

Pour éviter des modifications accidentelles Vespucci démarre en mode « verrouillé », lequel ne permet que de zoomer et de se déplacer sur la carte. Appuyez sur l’icône ![verrouillé](../images/locked.png) pour débloquer l'édition. 

Appuyez longuement sur l'icône de verrouillage pour afficher un menu qui offre actuellement 4 options :

* **Normal** : le mode d'édition par défaut, qui permet d'ajouter de nouveaux objets, de modifier, déplacer et supprimer des objets existants. Un cadenas blanc simple s'affiche.
* **Attributs seuls** : l'éditeur de propriétés sera ouvert en sélectionnant un objet et de nouveaux objets seront ajoutés avec un appui long sur l'écran principal, mais aucune opération géométrique ne fonctionnera. Un cadenas blanc avec la lettre « T » s'affiche.
* **Intérieur** : active le mode cartographie intérieure, voir le [mode intérieur](#indoor). Un cadenas blanc avec la letter « I » s'affiche.
* **Mode C** - active le mode C, où seuls les objets avec un drapeau d'avertissement seront affichés, voir le [mode C](#c-mode). Un cadenas blanc avec la lettre « C » s'affiche.

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

Une fois que vous avez sélectionné un objet, il peut être déplacé. Remarquez que les objets peuvent être déplacés uniquement lorsqu'ils sont sélectionnés. Glissez simplement (dans la zone interactive) l'objet sélectionné pour le déplacer Si vous sélectionnez l'option zone élargie dans les préférences vous aurez une zone plus large autour du nœud sélectionné, ce qui rendra plus facile le positionnement de l'objet 

#### Ajouter un nouveau nœud ou un nouveau point sur un chemin 

Au premier démarrage, l'appli se lance en « mode simple », ce que vous pouvez changer dans le menu principal en désélectionnant la case correspondante.

##### Mode simple

Appuyez sur le gros bouton vert flottant sur la carte pour afficher un menu. Après avoir sélectionné l'un des éléments, on vous demandera d'appuyer sur l'écran à l'emplacement où vous voulez créer l'objet, mais le déplacement et le zoom continuent de fonctionner si vous devez ajuster la vue de la carte. 

Voir [Créer de nouveaux objets dans le mode d'actions simples](Creating%20new%20objects%20in%20simple%20actions%20mode.md) pour plus d'informations.

##### Mode avancé (appui long)
 
Appuyez longuement là où vous voulez ajouter le nœud ou commencer le chemin. Vous verrez un réticule noir.
* Si vous voulez créer un nouveau nœud (non connecté à un objet), cliquez à l'écart des objets existants.
* Si vous voulez étendre un chemin, cliquez dans la zone interactive du chemin (ou d'un nœud du chemin). La zone interactive est indiquée par la zone autour du nœud ou du chemin.

Une fois que vous pouvez voir le symbole « réticule », vous avez trois options :

* Appuyez au même endroit.
  * Si le réticule n'est pas près d'un nœud, appuyez de nouveau au même endroit pour créer un nouveau nœud. Si vous êtes près d'une voie (mais pas d'un nœud), le nouveau nœud sera sur la voie (et connecté à la voie).
  * Si le réticule est près d'un nœud (c.-à-d. dans la zone interactive du nœud), appuyez au même endroit pour simplement sélectionner le nœud (et l'éditeur d'attributs). Aucun nœud n'est créé. L'action est la même que pour la sélection ci-dessus.
* Appuyez ailleurs. Appuyez ailleurs (en dehors de la zone interactive du réticule) pour ajouter un segment de voie depuis la position de départ à la position actuelle. Si le réticule était proche d'une voie ou d'un nœud, le nouveau segment sera connecté à ce nœud ou cette voie.

Touchez simplement l'écran aux endroits où vous voulez ajouter des nœuds au chemin. Pour finir, toucher le dernier nœud deux fois. Si le nœud final se trouve sur une voie ou un nœud, le segment sera connecté à la voie ou au nœud automatiquement. 

Vous pouvez aussi utiliser un élément du menu : voir [Créer de nouveau objets](Creating%20new%20objects.md) pour plus d'information.

#### Ajouter un polygone

OpenStreetMap n'a pour l'instant pas de type d'objet « polygone » contrairement aux autres systèmes de données géographiques. L'éditeur en ligne iD essaie de créer une abstraction de la zone à partir des éléments sous-jacents d'OSM, ce qui fonctionne dans certains cas, mais pas dans d'autres. Vespucci n'essaie pas de faire ça, donc vous devez savoir un peu comment les polygones sont représentés :

* _voies fermées (« polygones »)_ : la version la plus courante et la plus simple du polygone, c'est une voie qui dont le premier et le dernier nœud sont communs, ce qui forme un « anneau » fermé (par exemple la plupart des bâtiments sont de ce type). Ils sont très faciles à créer dans vespucci, en connectant simplement le dernier nœud au premier à la fin du dessin du polygone. Remarquez : l'interprétation de la voie fermée dépend de ses attributs : par exemple si une voie fermée a les attributs d'un bâtiment, elle sera considérée comme un polygone. Si elle a les attributs d'un rond-point, elle ne le sera pas. Dans certains cas où les deux interprétations pourraient être valides, un attribut « area » permet de clarifier l'utilisation souhaitée.
* _multi-polygones_ : certains polygones ont plusieurs parties, des trous et des anneaux qui ne peuvent pas être représentés avec une seule voie. OSM utilise un type de relation spécifique (l'objet général qui modélise des relations entre des éléments) pour cela, un multi-polygone. Un multi-polygone peut avoir plusieurs anneaux « outer » (externes) et plusieurs anneau « inner » (internes). Chaque anneau peut être soit une voie fermée comme décrite ci-dessus, soit plusieurs voies individuelles qui partagent leurs nœuds finals. Tandis que les multi-polygones sont difficiles à gérer avec les outils, les plus petits restent facile à créer dans Vespucci.
* _lignes de côte_ : pour les objets très vastes, les continents et les îles, même le modèle de multi-polygone ne marche pas de manière satisfaisante. Pour les voies natural=coastline on suppose que la sémantique dépend de l'orientation : la terre est à gauche de la voie et l'eau à droite. Cela a pour effet de bord qu'en général, on ne devrait pas inverser le sens d'une ligne de côte. Plus d'informations se trouvent sur le [wiki d'OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Améliorer la géométrie du chemin

Si vous zoomez assez loin sur un chemin sélectionné vous verrez un petit « x » au milieu des segments du chemin qui sont assez longs. Déplacez le « x » pour créer un nœud du chemin à cet emplacement. Remarquez que pour éviter de créer des nœuds de manière accidentelle, la zone interactive de cette opération est assez petite.

#### Couper, copier et coller

Vous pouvez couper ou copier des nœuds ou chemins sélectionnés, puis les coller à une ou plusieurs reprises à de nouveaux endroits. Couper conservera l'identifiant OSM et l'historique de l'élément. Pour coller, touchez de façon prolongée le lieu où vous voulez coller (qui sera marqué par un réticule), puis sélectionnez « Coller » dans le menu.

#### Ajouter efficacement des adresses

Vespucci propose une fonction « ajouter un attribut d'adresse » qui essaie de rendre plus efficace la collecte des adresses. Elle peut être utilisée :

* Après un appui prolongé : Vespucci ajoutera un nœud à cet endroit, essaiera de déterminer le numéro du bâtiment et ajoutera les attributs d'adresse que vous avez récemment utilisés. Si le nœud se situe sur le chemin traçant un bâtiment, un tag « entrance=yes » y sera automatiquement ajouté. L'éditeur d'attributs s'ouvrira alors pour l'objet en question pour vous permettre de faire les changements nécessaires.
* Dans le mode de sélection de nœud ou de chemin : Vespucci ajoutera les attributs d'adresse comme ci-dessus et affichera l'éditeur d'attributs.
* Dans l'éditeur d'attributs.

La prédiction du numéro de bâtiment nécessite normalement pour fonctionner qu'au moins deux numéros de chaque côté de la voie soient déjà présents. Plus nombreux sont les numéros déjà renseignés, meilleure est la prédiction.

Vous pourriez envisager d'utiliser cette fonction avec le mode de [téléchargement automatique](#download)  

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

## Utiliser le GPS

Vous pouvez utiliser Vespucci pour créer une trace GPX et l'afficher à l'écran. Vous pouvez aussi afficher votre position GPS actuelle (avec l'option « Afficher ma position » dans le menu GPS) ou recentrer en continu l'écran sur votre position (option « Recentrer sur ma position »). 

Si vous avez activé cette dernière, le déplacement manuel de l'écran et l'édition désactivera le mode de recentrage et la flèche GPS bleue vide se remplira. Pour revenir rapidement au mode de recentrage, appuyez simplement sur le bouton GPS ou cochez de nouveau l'option du menu.

## Notes et bugs

Vespucci supporte le téléchargement, les commentaires et la fermeture des notes OSM (précédemment connues sous le nom de bugs OSM) et des fonctions équivalentes pour les « bugs » produits par [l'outil d'assurance qualité Osmose](http://osmose.openstreetmap.fr/fr/map/). Les deux doivent être téléchargés explicitement ou vous pouvez utiliser la fonction de téléchargement automatique pour accéder aux éléments proches de vous. Une fois modifié ou fermé, vous pouvez soit envoyer un bug ou une note immédiatement, soit les envoyer tous d'un coup.

Sur la carte, les notes et les bugs sont représentés par une petite icône d'insecte ![Bug](../images/bug_open.png), les verts sont fermés/résolus, les bleus sont à vous, et les jaunes indiquent qu'ils sont toujours actifs et n'ont pas été changés. 

L'affichage des bugs Osmose fournit un lien vers l'objet affecté en bleu. Appuyez sur le lien pour sélectionner l'objet, centrer l'écran dessus et télécharger la zone en avance si nécessaire. 

### Filtres

En plus de l'activation générale des notes et des bugs vous pouvez indiquer un filtre d'affichage grossier pour réduire l'encombrement. Dans les [préférences avancées](Advanced%20preferences.md) vous pouvez sélectionner individuellement :

* Notes
* Erreur Osmose
* Avertissement Osmose
* Problème mineur Osmose
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

Actuellement il y a deux tests configurables (le test pour l'attribut FIXME et le test pour l'attribut de type manquant sur les relations ne sont pas actuellement configurables). Ces deux tests peuvent être configurés en sélectionnant les « préférences du validateur » dans « Préférences ». 

La liste des entrées est coupée en deux, la première moitié liste les entrée « nouveau relevé de terrain » et la deuxième moité les « vérifications ». On peut modifier les entrées en cliquant dessus et le bouton de menu vert permet d'ajouter des entrées.

#### Entrées de demande de nouveaux relevés de terrain

Les entrées de demande de nouveaux relevés de terrain ont les propriétés suivantes :

* **Clef** : la clef d'un attribut qui vous intéresse.
* **Valeur** : la valeur qu'un attribut qui vous intéresse devrait avoir. Si elle est vide, la valeur est ignorée.
* **Âge** : nombre de jours après le dernier changement de l'élément après lequel il devrait être vérifié. Si un champ check_date est présent il sera utilisé, sinon ce sera la date de la version actuelle. Mettre la valeur à zéro fera que le test n'utilisera que la clef et la valeur.
* **Expression régulière** : si la case est cochée, la **Valeur** est traitée comme une expression régulière JAVA.

**Clef** et **Valeur** doivent correspondre aux attributs _existants_ de l'objet en question.

#### Entrées de vérification

Les entrées de vérification ont les deux propriétés suivantes :

* **Clef** : la clef qui devrait être présente sur l'objet d'après le modèle d'attribut correspondant.
* **Attributs facultatifs requis** : Requiert la clef même si elle fait partie des attributs facultatifs du modèle correspondant.

Cette vérification fonctionne en déterminant d'abord le modèle correspondant puis en vérifiant que la **Clef** est une clef « recommandée » pour cet objet d'après l'attribut, **Attributs facultatifs requis » étendra la vérification aux attributs « facultatifs » de l'objet. Remarquez qu'actuellement les modèles liés ne sont pas vérifiés.

## Filtres

### Filtres basés sur les attributs

On peut activer le filtre depuis le menu principal, puis on peut le changer en appuyant sur l'icône de filtre. Plus de documentation sur [Filtre d'attribut](Tag%20filter.md).

### Filtre basé sur les modèles

Autrement, les objets peuvent être filtrés suivant des modèles individuels ou des groupes de modèles. Appuyez sur l'icône de filtre pour afficher un dialogue de sélection de modèles similaire à ceux utilisés ailleurs dans Vespucci. On peut sélectionner des modèles individuels en cliquant simplement dessus, et des groupes de modèles en cliquant longuement (un clic normal ouvre le groupe). Plus de documentation sur [Fitre de modèle](Preset%20filter.md).

## Personnaliser Vespucci

### Paramètres que vous pourriez vouloir changer

* Couche de fond de carte
* Surcouche. Ajouter une surcouche peut poser des problèmes avec les anciens appareils avec une mémoire limitée. Par défaut : aucune.
* Affichage des notes et des bugs. Les notes et les bugs ouverts seront affichés avec une icône d'insecte jaune, ceux qui sont fermés avec une icône verte. Par défaut : activé.
* Couche photo. Affiche des photos géo-référencées avec une icône d'appareil rouge, si l'information de direction est disponible, l'icône sera tournée. Par défaut : désactivé.
* Garder l'écran allumé. Par défaut : désactivé.
* Zone élargie de déplacement des nœuds. Déplacer des nœuds sur les appareils avec une entrée tactile est difficile parce que vos doigts cachent la position actuelle. Activer cette option fournira de plus grandes zones pour déplacer un objet sans devoir en toucher le centre (la sélection et les autres opérations utiliseront toujours la zone interactive normale). Par défaut : désactivé.

#### Préférences avancées

* Icône de nœud. Par défaut : activé.
* Toujours montrer le menu contextuel. Lorsqu'elle est activée, à chaque sélection le menu contextuel sera affiché. Sinon le menu n'est affiché que lorsque aucune sélection non-ambiguë n'est déterminée. Par défaut : désactivé (activé sur d'anciennes versions).
* Activer le thème clair. Sur les appareils modernes, cette option est activée par défaut. Vous pouvez l'activer sur des appareils Android plus anciens, mais le thème ne sera sans doute pas cohérent avec le reste du système.
* Montrer les statistiques. Affichera certaines statistique pour le débogage, pas vraiment utile. Par défaut : désactivé (activé sur d'anciennes versions).  

## Rapporter des problèmes

Si Vespucci plante, ou qu'il détecte un état incohérent, il vous demandera d'envoyer un rapport de plantage. Faîtes-le si cela arrive, mais une seule fois par situation spécifique. Si vous voulez donner plus d'informations ou ouvrir un rapport de bug pour une demande de fonctionnalités, faîtes-le ici : [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). La fonction « faire un retour » du menu principal ouvrira un nouveau ticket et inclura les informations utiles sur l'appareil et l'appli en vous évitant de les écrire vous-même.

Si vous voulez discuter de quelque chose en lien avec Vespucci, vous pouvez démarrer une discussion soit sur le [Google group de Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android), soit sur le [forum Android d'OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


