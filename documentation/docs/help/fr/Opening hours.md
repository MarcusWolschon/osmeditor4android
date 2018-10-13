# Éditeur d'horaires d'ouverture OpenStreetMap

La spécification des horaires d'ouverture d'OpenStreetMap est assez compliquée et ne se prête pas directement à une interface utilisateur simple et intuitive.

Cependant, la plupart du temps vous n'utiliserez surement qu'une petite partie de la définition. L'éditeur prend cela en compte en essayant de cacher les possibilités les plus obscures dans les menus et réduit le travail normal à quelques personnalisations de modèles pré-définis.

_Cette documentation est inachevée_

## Utiliser l'éditeur d'horaires d'ouverture

Dans votre travail quotidien, l'objet que vous modifiez peut soit avoir déjà un attribut d'horaires d'ouverture (opening_hours, service_times ou collection_times) ou vous pouvez ré-appliquer le modèle d'attribut pour obtenir un attribut vide. Si vous avez besoin d'ajouter le champs manuellement et que vous utilisez Vespucci, vous pouvez saisir la clef sur la page de détails puis revenir sur le formulaire pour la modifier. Si vous pensez que l'attribut d'horaires d'ouverture aurait dû faire parti du modèle, ouvrez un rapport de bogue pour votre éditeur.

Si vous avez défini un modèle par défaut (pour cela, utilisez l'élément de menu « Gérer les modèles  »), il sera chargé automatiquement au démarrage de l'éditeur avec une valeur vide. Vous pouvez charger n'importe quel modèle sauvegardé avec la fonctionnalité « Charger un modèle » et vous pouvez sauvegarder les valeurs actuelles dans un modèle avec « Enregistrer le modèle ». Vous pouvez définir des modèles et un modèle par défaut différents pour les attributs « opening_hours », « collection_times » et « service_times ».

Naturellement vous pouvez construire des horaires d'ouverture à partir de rien, mais nous recommandons d'utiliser l'un des modèles existants comme point de départ.

Si une valeur d'horaires d'ouverture existante est chargée, le logiciel tentera de la corriger automatiquement pour qu'elle soit conforme avec la spécification. Si cela n'est pas possible, l'emplacement approximatif où l'erreur a eu lieue sera surlignée dans l'affichage de la valeur brute et vous pourrez essayer de la corriger manuellement. Environ un quart des valeurs d'horaires d'ouverture ont des problèmes, mais moins de 10% ne peuvent pas être corrigées. Voir [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) pour plus d'information sur les déviations à la spécification qui sont tolérées.

### Bouton de menu principal

* __Ajouter une règle__ : ajouter une nouvelle règle.
* __Ajouter une règle pour les vacances__ : ajouter une nouvelle règle pour les vacances en même temps qu'un changement d'état.
* __Ajouter une règle pour 24h/24 7j/7__ : ajouter une règle pour un objet qui est tout le temps ouvert. La spécification n'accepte pas d'autre sous-valeurs pour 24h/24 7j/7 mais nous permettons l'ajout de sélecteurs de plus haut niveau (par exemple un intervalle d'années).
* __Charger un modèle__ : charger un modèle existant.
* __Enregistrer dans un modèle__ : sauvegarder la valeur actuelle des horaires d'ouverture comme un modèle pour pouvoir l'utiliser plus tard.
* __Manage templates__: edit, for example change the name, and delete existing templates.
* __Rafraîchir__ : ré-analyser la valeur des horaires d'ouverture.
* __Tout supprimer__ : Supprimer toutes les règles.

### Règles

Les règles par défaut sont ajoutées comme des règles _normales_, ce qui implique qu'elles annuleront les règles précédentes pour les mêmes jours. Ce peut être un problème pour spécifier des horaires étendues. Vous pouvez alors changer les règles en changeant l'élément de menu _Montrer le type de règle_ en _additif_.

#### Menu des règles

* __Ajouter un modificateur ou un commentaire__ : changer l'effet de cette règle et ajouter un commentaire facultatif.
* __Ajouter des jours fériés__ : ajouter un sélecteur pour les vacances publiques ou scolaires.
* __Ajouter un intervalle de temps…__
    * __Heure fixe à heure fixe__ : une heure de début et de fin le même jour.
    * __Heure fixe à heure fixe étendue__ : une heure de début et de fin le jour suivant (par exemple 26:00 est 02:00 (du matin) le jour suivant).
    * __Heure variable à heure fixe__ : d'une heure variable (aube, crépuscule, levé du soleil ou couché du soleil) à une heure de fin le même jour.
    * __Heure variable à heure fixe étendue__ : d'une heure de début variable à une heure de fin le jour suivant.
    * __Heure fixe à heure variable__ : d'une heure de début à une heure de fin variable.
    * __Heure variable à heure variable__ : d'une heure de début variable à une heure de fin variable.
    * __Heure fixe__ : un point dans le temps.
    * __Heure fixe à durée indéterminée__ : à partir d'un point dans le temps.
    * __Heure variable__ : un point dans le temps variable.
    * __Heure variable à durée indéterminée__ : à partir d'un point variable dans le temps.
* __Ajouter un intervalle de jours de semaine__ : ajouter un sélecteur de jours de la semaine.
* __Ajouter un intervalle de dates…__
    * __Date fixe à date fixe__ : d'une date de début (année, mois, jour) à une date de fin.
    * __Date variable à data fixe__ : d'une date de début variable (actuellement la spécification ne définie que _pâques_) à une date de fin.
    * __Date fixe à date variable__ : d'une date de début à une date de fin variable.
    * __Date variable à date variable__ : d'une date de début variable à une date de fin variable.
    * __Occurrence dans le mois à occurrence dans le mois__ : d'une occurrence d'un jour de la semaine à un autre.
    * __Occurrence dans le mois à date fixe__ : d'une occurrence d'un jour de la semaine de début à une date de fin.
    * __Date fixe à occurrence dans le mois__ : d'une date de début à une occurrence d'un jour de la semaine à la fin.
    * __Occurrence dans le mois à date variable__ : d'une occurrence d'un jour de la semaine dans un mois à une date variable de fin.
    * __Date variable à occurrence dans le mois__ : d'une date de début variable à une occurrence d'un jour de la semaine dans un mois.
    * __Date fixe à durée indéterminée__  : à partir d'une date de début.
    * __Date variable à durée indéterminée__ : à partir d'une date de début variable.
    * __Occurrence dans le mois à durée indéterminée__ : à partir d'une occurrence d'un jour de la semaine dans un mois.
    * __Avec décalages…__ : les mêmes entrées que précédemment, mais avec des décalages spécifiés (c'est rarement utile).
* __Ajouter un intervalle d'années__ : ajouter un sélecteur d'années.
* __Ajouter un intervalle de semaines__ : ajouter un sélecteur de numéro de semaine.
* __Dupliquer__ : créer une copie de cette règle et l'insérer après la position actuelle.
* __Montrer le type de règle__ : afficher et permettre de changer le type de règle entre _normal_, _additif_ et _par défaut_ (indisponible sur la première règle).
* __Déplacer vers le haut__ : déplacer cette règle d'une position vers le haut (indisponible sur la première règle).
* __Déplacer vers le bas__ : déplacer cette règle d'une position vers le bas.
* __Supprimer__ : supprimer cette règle.

### Intervalles de temps

Pour rendre la modification d'intervalles de temps le plus simple possible, nous essayons de choisir un intervalle de temps optimal et une granularité pour la barre d'intervalle lors du chargement de valeurs existantes. Pour les nouveaux intervalles, la barre commence à 6:00 (du matin) avec des incrément de 15 minutes, ce qui peut se changer via le menu.

#### Menu d'intervalle de temps

* __Montrer le sélecteur d'heure__ : montrer un sélecteur d'heure pour choisir l'heure de début et de fin. C'est la méthode privilégiée sur les petits écrans pour changer les horaires.
* __Passer à des intervalles de 15 minutes__ : utiliser une granularité à 15 minutes sur la barre d'intervalle.
* __Passer à des intervalles de 5 minutes__ : utiliser une granularité à 5 minutes sur la barre d'intervalle.
* __Passer à des intervalles de 1 minutes__ : utiliser une granularité à 1 minutes sur la barre d'intervalle. C'est très difficile à utiliser sur un téléphone.
* __Commencer à minuit__ : commencer la barre d'intervalle à minuit.
* __Montrer l'intervalle__ : montrer le champ d'intervalle pour spécifier un intervalle en minutes.
* __Effacer__  : supprimer cet intervalle de temps.

