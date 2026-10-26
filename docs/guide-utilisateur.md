# Guide utilisateur

Adresse : celle communiquée par l'administrateur (en développement : http://localhost:5173).

## Se connecter

- Saisir son identifiant et son mot de passe.
- Après **5 essais faux**, le compte est verrouillé **15 minutes**.
- Mot de passe oublié : il n'y a pas d'envoi d'e-mail (les données restent au cabinet) ; demander à
  l'administrateur de le réinitialiser.
- La session se ferme après **30 minutes sans activité** (et au plus tard après 10 heures).

Ce que chacun voit dépend de son rôle :

| Écran | Administrateur | Gestionnaire | Consultation |
|---|---|---|---|
| Tableau de bord, Dossiers, fiche, Analyse des prix, Génération | oui | oui | oui |
| Importer des rapports, Vérification, Validation | — | oui | — |
| Utilisateurs et journal d'audit | oui | — | — |

## Barre du haut

- **Recherche** (Ctrl+K) : n° de rapport, immatriculation, nom… puis Entrée : ouvre la liste filtrée.
- **Cloche** : nombre de dossiers à traiter (à vérifier, à compléter, prêts à valider) ; un clic ouvre la liste.
- **Menu utilisateur** : déconnexion.

## Tableau de bord

Dossiers traités, montant total, coût moyen, dossiers en attente de validation ; répartition par statut, par zone
de choc, par compagnie ; montants par mois ; alertes (dossiers en anomalie) ; derniers dossiers. Le sélecteur
« période » limite les chiffres à un mois.

## Importer des rapports (gestionnaire)

1. Glisser-déposer un ou plusieurs PDF (20 Mo maximum chacun), ou cliquer pour les choisir.
2. Plusieurs fichiers sont traités en arrière-plan ; le récapitulatif se met à jour : réussis, prêts à valider, à
   vérifier, à compléter, refusés.
3. Un fichier déjà importé est ignoré ; un autre fichier portant un n° de rapport existant, ou un PDF d'un modèle
   inconnu, est refusé : il apparaît dans **« Derniers imports »** marqué « Refusé », avec son motif. Le PDF
   refusé est conservé.

## Dossiers

- **Filtres** : recherche, période, compagnie, statut, zone de choc, tranche de montant ; onglets rapides par
  statut. Les filtres restent dans l'adresse de la page (on peut la partager).
- **Ouvrir** un dossier : un gestionnaire arrive sur la vérification tant que le dossier n'est pas validé ;
  sinon, sur la fiche.
- **Exporter en Excel** : toute la liste répondant aux filtres (pas seulement la page affichée).

### Valider plusieurs dossiers (gestionnaire)

- Cocher les dossiers « Prêt à valider » (la case en tête coche ceux de la page), puis **Valider la sélection** ;
- ou **Valider tous les prêts à valider (N)** : tous les dossiers prêts qui répondent aux filtres en cours, même
  s'ils sont des milliers (traités par paquets de 500).
- Une confirmation est demandée. Le résultat indique les dossiers validés et ceux ignorés, avec leur motif.

## Vérifier un dossier (gestionnaire)

L'écran montre le **PDF original à gauche** et les **données à droite**, en six onglets : Sinistre, Parties,
Véhicule, Évaluation des dommages, Détail des pièces, Conclusion. Le panneau **« Contrôles de cohérence »**
liste les contrôles :

| Symbole | Sens |
|---|---|
| ✓ vert | Contrôle conforme |
| × rouge | Contrôle **bloquant** en échec : à corriger avant de valider |
| ! orange | Avertissement (ex. champs absents du PDF) : n'empêche pas la validation |

- **Champs absents du PDF** : signalés « Absent du PDF » ; les remplir est **facultatif**. Seuls le montant
  d'indemnisation, la procédure et la TVA récupérable sont indispensables.
- **Zones de choc** : cocher une ou plusieurs zones (onglet Sinistre), ou cliquer les pastilles du schéma du
  véhicule (onglet Véhicule).
- **Prix unitaires faux** : si le contrôle « prix unitaire à corriger (ligne N …) » apparaît, cliquer
  **« Corriger les prix »** : les lignes en rouge ont un champ prix ; le bouton **→** propose le prix qui
  redonne le total imprimé. Le PDF original n'est jamais modifié.
- **Commentaire** (onglet Conclusion) : les valeurs clés lues dans le commentaire (valeur à neuf, valeur vénale,
  divergence, cadre de l'expertise…) s'affichent sous le texte.
- **Enregistrer** : les contrôles et le statut sont recalculés ; chaque modification est tracée (qui, quand,
  ancienne et nouvelle valeur).
- **Valider le dossier** : possible quand le dossier est « Prêt à valider » et qu'il n'y a pas de modification
  en cours. Si le bouton est grisé, son info-bulle donne la raison.

Si deux personnes modifient le même dossier, la seconde est prévenue et doit recharger.

## Fiche d'un dossier

Toutes les données, le détail des pièces (lignes au prix incohérent en rouge), les valeurs du commentaire,
l'historique des actions. Boutons :

- **Voir le PDF original** ;
- **Générer le rapport PDF** : uniquement pour un dossier **validé** ; produit un PDF reprenant la structure du
  rapport de l'expert avec les données validées ;
- **Exporter en Excel** : fiche, évaluation, détail des pièces, contrôles, valeurs du commentaire ;
- **Vérifier / Modifier** (gestionnaire).

## Analyse des prix

Prix unitaires des pièces relevés dans les rapports : minimum, moyenne, maximum, moyenne par type (origine,
adaptable, réemploi), détail des lignes. Filtres : désignation, type, compagnie, marque.
**Exporter l'analyse** : toutes les lignes en Excel.

## Génération des rapports

Choisir un dossier validé (recherche), vérifier l'aperçu, puis **Générer le rapport PDF** ou **Exporter en Excel**.
La liste « Rapports récemment générés » permet de consulter le dossier ou de régénérer le rapport.

## Utilisateurs (administrateur)

- **Comptes** : ajouter un utilisateur (identifiant, nom, rôle, mot de passe initial de 10 caractères au moins),
  modifier, désactiver / réactiver, réinitialiser le mot de passe (déverrouille le compte et ferme ses
  sessions). On ne peut pas désactiver son propre compte.
- **Journal d'audit** : toutes les actions (connexions, imports, modifications, validations, exports, comptes),
  filtrables par texte et par type ; **Exporter le journal** en Excel.
