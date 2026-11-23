# Règles métier

Ce document décrit les règles appliquées par l'application. Le cahier des charges
([cahier-des-charge.md](cahier-des-charge.md)) reste la référence ; les décisions prises en cours de projet sont
signalées.

## Statuts d'un dossier (P6)

| Statut | Signification | Comment on y arrive |
|---|---|---|
| En cours | Dossier repris du fichier Excel de suivi, en attente du PDF (F14) | Synchronisation Excel (lot 7, à venir) |
| Importé | Dossier créé à l'import | Transitoire |
| À vérifier | Un **contrôle bloquant** échoue | Calculé après import et après chaque modification |
| À compléter | Une **donnée essentielle** manque | Idem |
| Prêt à valider | Aucun blocage : en attente d'un gestionnaire | Idem |
| Validé | Validé par un gestionnaire ou un administrateur (P3), jamais automatiquement | Action « Valider », unitaire ou en lot |
| Archivé | Dossier validé puis clos : consultable, compté dans les statistiques, ne se modifie plus | Action « Archiver » d’un gestionnaire ou d’un administrateur, sur un dossier validé (fiche), ou en lot depuis la liste : dossiers validés cochés ou tous les validés selon les filtres |
| Annulé | Import erroné écarté (mauvais fichier, rapport d'un autre dossier…) | Action « Annuler l'import » d'un **administrateur**, motif obligatoire, tant que le dossier n'est pas validé |

Le statut est recalculé à chaque enregistrement de l'écran de vérification, sauf pour un dossier archivé ou annulé qui
ne se modifie plus.

### Pas de suppression d'un dossier

Un dossier importé ne se supprime jamais : le PDF original fait foi (P1, stockage verrouillé) et le journal d'audit est en
ajout seul (P7). Un import erroné s'**annule** :

- le PDF original, les données et l'historique restent consultables (onglet « Annulés » de la liste) ;
- le dossier sort de la liste par défaut, du tableau de bord, de l'analyse des prix et des recherches F12 ;
- le n° de rapport est libéré : le bon PDF portant le même n° peut être réimporté (le n° reste unique parmi les dossiers
  non annulés) ; le même fichier, lui, reste refusé comme doublon (empreinte) ;
- un dossier validé ne s'annule pas : il s'archive.

## Contrôles de cohérence (P4, P5)

28 contrôles par rapport, avec une tolérance de 0,05 DH :

| Contrôle | Nombre |
|---|---|
| Pour chacun des 7 postes de main d'œuvre : heures × taux = HT, HT + TVA = TTC | 14 |
| Somme des postes = total main d'œuvre ; somme des fournitures = total fournitures (HT, TVA, TTC) | 6 |
| Main d'œuvre + fournitures = total général (HT, TVA, TTC) | 3 |
| Indemnisation : total général TTC − vétusté ; **HT** si la TVA est récupérable (P5) ; réforme : valeur vénale − valeur épave | 1 |
| Montant en lettres = montant en chiffres | 1 |
| Page 2 = page 1 : main d'œuvre TTC ; fournitures TTC − vétusté | 2 |
| Chaque ligne de la page 2 : quantité × prix × (1 + TVA) × (1 − vétusté) = total | 1 |

Les sommes de la page 2 tolèrent un demi-centime par ligne (lignes arrondies une à une). Le prix d'une ligne tolère
0,6 centime par unité (prix unitaire imprimé arrondi).

### Contrôle bloquant ou avertissement

| Résultat | Bloque la validation ? |
|---|---|
| Contrôle de cohérence en échec (dont le prix d'une ligne) | **Oui** — le dossier passe « À vérifier » |
| Donnée essentielle absente : montant d'indemnisation, procédure, TVA récupérable | **Oui** — « À compléter » |
| Autre champ absent ou masqué dans le PDF (ex. intermédiaire, date après travaux) | Non — simple avertissement |

Décision du 22/10/2026 : l'application **extrait** ; l'utilisateur n'est pas obligé de remplir les cases que le PDF
ne contient pas. Les champs absents restent signalés (« Absent du PDF ») et peuvent être saisis.

### Prix unitaires faux imprimés dans le PDF

Le logiciel de l'expert imprime parfois un prix unitaire faux (négatif, ou sans rapport avec le total) alors que le
total de la ligne est juste. Exemple réel : `3 CHEVILLE … 9.10 … 88.45` alors que 3 × 9,10 × 1,20 × 0,90 = 29,48.

- L'extraction lit fidèlement le PDF ; c'est le contrôle de la ligne qui signale l'écart.
- Le contrôle est **bloquant** ; son libellé nomme les lignes (« ligne 2 CHEVILLE »), qui apparaissent en rouge.
- Le gestionnaire corrige le prix ; l'application propose le prix qui redonne le total imprimé :
  total ÷ (quantité × (1 + TVA) × (1 − vétusté)), soit 27,30 dans l'exemple.
- Le PDF original n'est jamais modifié (P1) ; l'ancien et le nouveau prix sont tracés (P7).

## Doublons (P2)

| Cas | Résultat |
|---|---|
| Fichier identique (même empreinte SHA-256) | Ignoré, rien n'est enregistré |
| Autre fichier, même n° de rapport | Refusé ; PDF conservé, motif visible dans « Derniers imports » |
| Modèle de PDF non reconnu, PDF illisible | Refusé, conservé avec son motif |

## Zones de choc (P8)

Codes 1 à 8 : 1 Avant/Latéral gauche, 2 Avant/Latéral droit, 3 Central/Latéral gauche, 4 Central/Latéral droit,
5 Arrière/Latéral gauche, 6 Arrière/Latéral droit, 7 Arrière, 8 Avant.

Un rapport peut en avoir **plusieurs** : la page 1 les liste l'une sous l'autre (« 7 : Arriere », « 8 : Avant »). À
défaut, l'application reprend les zones des sections de la page 2. Un dossier à plusieurs zones compte dans
chacune (filtre et statistiques).

## Valeurs clés du commentaire (F12)

Lues dans le commentaire de l'expert (phrases types des rapports 2026) et recalculées si le commentaire change :

| Valeur | Phrase du rapport | Exemple |
|---|---|---|
| Valeur à neuf (prix catalogue) | « LA VALEUR A NEUF … PRIX CATALOGUE DISPONIBLE : 519000.00 » | 519 000,00 |
| Valeur vénale (barème FMA) | « LA VALEUR VENALE … SELON LE BAREME FMA : 111400.45 » | 111 400,45 |
| Divergence usage / contrat | « NOUS N AVONS PAS CONSTATE DE DIVERGENCE … » | Non (Oui si constatée) |
| Pièces de récupération (ferraille) valorisées | « L ENSEMBLE DES PIECES DE RECUPERATION … FERRAILLE … » | Oui |
| Peinture selon les règles de l'art | « … TRAVAUX DE PEINTURE ONT ETE RETENUS CONFORMEMENT AUX REGLES DE L ART » | Oui |
| Choc décrit par l'expert | « LE VEHICULE A SUBI UN CHOC LAT ARD » | LAT ARD |
| Cadre de l'expertise | « DANS LE CADRE D'UNE EXPERTISE FORFAITAIRE » | FORFAITAIRE |

Une phrase absente ne produit pas de valeur. Le texte complet du commentaire reste conservé.

## Validation (P3)

- Seul un gestionnaire ou un administrateur actif valide (l'administrateur a tous les droits) ; seul un dossier « Prêt à valider » peut l'être ; un rapport non définitif ne
  se valide pas.
- **En lot** : les dossiers cochés, ou tous les dossiers prêts répondant aux filtres de la liste. Traitement par
  paquets de 500, chacun dans sa propre transaction ; les dossiers non prêts sont ignorés avec leur motif.
  Chaque validation est tracée comme une validation unitaire.

## Génération et exports (F9, P10)

- Le rapport PDF ne se génère qu'à partir d'un dossier **validé**, avec la structure du rapport d'origine et les
  données en base (corrections comprises). Il porte la mention de sa régénération (date, auteur, validation).
- Exports Excel : un dossier, la liste filtrée (en entier), l'analyse des prix, le journal (administrateur).
- Chaque génération et chaque export sont tracés (EXPORT_PDF, EXPORT_EXCEL).

## Traçabilité (P7)

Le journal d'audit est en **ajout seul** (triggers en base interdisant modification et suppression). Il enregistre :
connexions et échecs, imports et refus, modifications (champ, ancienne et nouvelle valeur), changements de
statut, validations, exports, gestion des comptes.
