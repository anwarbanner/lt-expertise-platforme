# Cahier des charges — Plateforme de gestion des rapports d'expertise

**Client :** LT Expertise · **Date :** Oct 5, 2026 · **Rédigé par** **:**** ****l****a****k****l****aty ****anwa****r**

## 1. Présentation du projet

LT Expertise disposera d'une application web qui transforme ses rapports d'expertise automobile PDF en données fiables, consultables et réutilisables. Le projet démarre le Oct 13, 2026, pour une mise en service locale le Dec 7, 2026.

**Contexte.** Chaque rapport d'expertise contient des montants (main d'œuvre, fournitures, TVA, vétusté, indemnisation) et des informations sur le sinistre et le véhicule. Aujourd'hui, ces données restent enfermées dans des fichiers PDF : les retrouver, les comparer ou les analyser impose une ressaisie manuelle, lente et source d'erreurs.

**Objectifs.**

- Importer les rapports PDF et en extraire automatiquement les champs et les tableaux chiffrés.
- Faire contrôler, compléter et valider les données par un gestionnaire avant enregistrement définitif.
- Centraliser les dossiers dans une base de données unique et sécurisée.
- Retrouver n'importe quel dossier en quelques secondes grâce à la recherche et aux filtres.
- Générer de nouveaux rapports PDF, fidèles au modèle d'origine, et des exports Excel.
- Piloter l'activité du cabinet grâce à un tableau de bord et des statistiques.

**Périmètre de déploiement.** Dans un premier temps, l'application est installée en local, sur un poste ou un serveur situé dans les locaux de LT Expertise, et utilisée depuis les postes du réseau interne. Les données ne quittent pas le cabinet.

## 2. Analyse des formats PDF existants

Les rapports actuels sont des PDF générés par logiciel, au gabarit fixe de 2 pages : l'extraction est fiable sans OCR ni intelligence artificielle. Un prototype d'extraction a déjà été testé sur deux rapports réels (n° 268202-0231 et 268202-0284) : tous les montants ont été extraits et 27 contrôles de cohérence sur 27 ont réussi.

| Bloc du rapport | Page | Données | Traitement |
| --- | --- | --- | --- |
| En-tête | 1 | N° de rapport, procédure (classique, ), réf. sinistre, dates sinistre et mission, compagnie | Extraction automatique |
| Assuré / Adversaire | 1 | Nom, véhicule, immatriculation, compagnie, n° de police, intermédiaire | Extraction ou saisie manuelle |
| Contrat et réparateur | 1 | Nature du contrat (RC), réparateur, accord du réparateur | Extraction automatique |
| Caractéristiques du véhicule | 1 | Marque, modèle, n° de série, énergie, kilométrage, usure des 4 pneus, zone de choc (code 1 à 8) | Extraction automatique |
| Évaluation des dommages | 1 | 7 postes de main d'œuvre (heures, taux, HT, TVA, TTC), fournitures, totaux | Extraction automatique |
| Conclusion | 1 | Vétusté, dépannage, réforme, valeurs (neuf, vénale, épave), TVA récupérable, indemnisation, montant en lettres | Extraction automatique |
| Détail fournitures et travaux | 2 | Par pièce : quantité, type (O/A/R), prix, vétusté %, TVA %, réparation ou échange, heures, montant | Extraction automatique |
| Commentaire | 2 | Texte libre de l'expert ; pour les rapports de 2026, les valeurs importantes qu'il contient sont extraites en champs structurés (F12) | Extraction automatique |

Les champs texte absents ou masqués dans le PDF sont signalés « à compléter » et saisis par l'utilisateur dans l'application. Si le modèle de PDF change, l'import du fichier est refusé proprement pour éviter d'enregistrer des données fausses.

## 3. Flux de traitement d'un rapport

Chaque rapport suit le même chemin : PDF, extraction, données structurées, base, puis nouveau PDF.

&#91;embedded content: flux d'un rapport · de l'import au nouveau PDF\]

Un rapport dont un contrôle échoue ou dont des champs manquent reste « à vérifier » ou « à compléter » jusqu'à la validation par l'utilisateur.

**Si un PDF est refusé (modèle non reconnu)**, rien n'est enregistré automatiquement, mais le dossier n'est pas bloqué :

1. Le PDF est conservé dans la liste « Fichiers refusés », avec la raison du refus, la date et l'utilisateur.
2. Le gestionnaire peut créer le dossier par saisie manuelle : un formulaire vierge s'ouvre à côté du PDF, puis le dossier passe par les mêmes contrôles de cohérence et la même validation qu'un dossier extrait automatiquement.
3. Si ce modèle de PDF revient souvent, il est signalé à l'administrateur pour être ajouté à l'extraction automatique ; les fichiers refusés peuvent alors être réimportés en un clic.

## 4. Principes métier

L'application applique onze règles métier qui garantissent que chaque chiffre enregistré est juste, traçable et conforme au rapport de l'expert.

| N° | Principe | Règle appliquée | Bénéfice pour LT Expertise |
| --- | --- | --- | --- |
| P1 | Le PDF original fait foi | Le rapport importé est conservé tel quel, jamais modifié, et reste consultable à côté des données extraites | Preuve disponible en cas de contestation ou de litige |
| P2 | Un rapport, un dossier | Un n° de rapport ne peut exister qu'une fois ; un fichier déjà importé est détecté et refusé | Aucune indemnisation comptée deux fois |
| P3 | Validation humaine obligatoire | Aucun dossier ne passe au statut « Validé » sans l'accord d'un gestionnaire | Le cabinet garde la maîtrise de ses données |
| P4 | Cohérence des montants | Heures × taux = HT ; HT + TVA = TTC ; somme des postes = totaux ; total général TTC − vétusté = indemnisation ; montant en lettres = montant en chiffres | Erreurs de calcul ou de saisie détectées immédiatement |
| P5 | Traitement de la TVA | TVA non récupérable : indemnisation calculée TTC ; TVA récupérable : base HT | Montant d'indemnisation conforme à la situation de l'assuré |
| P6 | Cycle de vie du dossier | En cours (dossier repris du fichier Excel, en attente du rapport), puis Importé, puis À compléter ou À vérifier, puis Validé, puis Archivé ; un dossier validé ne se modifie que par un gestionnaire | État de chaque dossier connu à tout moment |
| P7 | Traçabilité complète | Chaque import, modification, validation et export est enregistré : utilisateur, date, ancienne et nouvelle valeur | Contrôle interne et réponse rapide en cas de question |
| P8 | Référentiels communs | Postes de main d'œuvre (tôlerie, peinture, mécanique), types de pièces (O origine, A adaptable, R réemploi), zones de choc 1 à 8, nature de contrat | Statistiques comparables d'un dossier à l'autre |
| P9 | Accès selon le rôle | Chaque utilisateur ne voit et ne fait que ce que son rôle autorise | Confidentialité des données des assurés (loi 09-08) |
| P10 | Rapport régénéré fidèle | Un nouveau PDF n'est produit qu'à partir de données validées, avec la structure du rapport d'origine | Documents remis aux partenaires toujours exacts |
| P11 | Base de données exclusivement interne | La base et les PDF restent sur le serveur local du cabinet : aucune copie dans le cloud, aucun accès depuis Internet, aucune connexion à un service externe ; les sauvegardes sont conservées en interne | Maîtrise totale des données des assurés et conformité à la loi 09-08 |

## 5. Architecture technique

L'application est un monolithe modulaire en Java 21 et Spring Boot : une interface React + TypeScript dialogue avec une API REST unique, qui pilote la couche PDF (Apache PDFBox) et la base PostgreSQL.

Ce n'est pas une application d'intelligence artificielle : les rapports suivent une structure standardisée (champs identifiables, informations véhicule, tableaux d'évaluation des dommages, totaux, détail des réparations). Le besoin relève donc d'une application de gestion organisée en cinq couches :

1. **Traitement documentaire** : lecture des PDF et extraction des champs et des tableaux.
2. **Règles métier** : contrôles de cohérence et calcul des totaux.
3. **Données structurées** : rapports, véhicules, dommages et opérations de réparation en base relationnelle.
4. **Workflow** : statuts du dossier, vérification et validation par l'utilisateur.
5. **Reporting** : recherche, statistiques, génération de PDF et d'exports Excel.

&#91;embedded content: architecture technique · React, Spring Boot, PDFBox, PostgreSQL, installée en local\]

Ce choix, plutôt que des microservices, réduit le coût de développement et de maintenance pour un volume de cabinet d'expertise ; chaque module reste séparé dans le code et peut évoluer seul.

| Couche | Choix | Justification |
| --- | --- | --- |
| Backend | **Java 21 / Spring Boot** | Écosystème d'entreprise mature, typage fort, maintenabilité à long terme |
| API | **REST / JSON** | Séparation nette entre l'interface et le serveur |
| Frontend | **React + TypeScript** | Formulaires dynamiques, tableaux, tableaux de bord, écran de vérification des PDF |
| Base de données | **PostgreSQL** | Modèle relationnel adapté aux rapports, véhicules, dommages et opérations de réparation |
| ORM | **Spring Data JPA / Hibernate** | Couche de persistance robuste et éprouvée |
| Extraction PDF | **Apache PDFBox** | Traitement des PDF natif en Java, sans service externe |
| Génération de rapports | **JasperReports, Apache POI** | Reproduction fidèle du modèle de rapport en PDF ; exports Excel |
| Sécurité | **Spring Security** | Authentification, autorisations et rôles |
| Déploiement | **Docker** | Déploiement reproductible et maintenance simplifiée |

## 6. Exigences fonctionnelles

L'application couvre quatorze modules fonctionnels, de l'import du PDF jusqu'à la génération d'un nouveau rapport.

| N° | Module | Fonctionnalités attendues |
| --- | --- | --- |
| F1 | Import PDF | Dépôt d'un ou plusieurs PDF par glisser-déposer ; détection des doublons (n° de rapport et empreinte du fichier) ; conservation du PDF original ; historique des imports |
| F2 | Extraction des champs | Lecture automatique des champs du rapport (en-tête, contrat, véhicule, montants, conclusion) ; montant en lettres converti en chiffres |
| F3 | Extraction des tableaux | Tableau « Évaluation des dommages » (7 postes de main d'œuvre, fournitures, totaux) et tableau « Détail fournitures / travaux » ligne par ligne |
| F4 | Nettoyage et validation | Normalisation (dates, montants, accents) ; contrôles de cohérence (heures × taux, HT + TVA = TTC, sommes, page 1 = page 2, indemnisation, montant en lettres) ; statut du dossier : validé, à compléter, à vérifier |
| F5 | Contrôle et modification manuelle | Écran de vérification avec le PDF à gauche et le formulaire à droite ; champs en erreur surlignés ; saisie des champs manquants ; validation finale par l'utilisateur ; historique des modifications (qui, quand, ancienne et nouvelle valeur) |
| F6 | Stockage | Enregistrement des données validées dans PostgreSQL : rapport, parties, véhicule, main d'œuvre, fournitures, lignes de détail |
| F7 | Recherche et filtrage | Recherche par n° de rapport, réf. sinistre, immatriculation, nom, réparateur ; filtres par période, procédure, compagnie, statut, zone de choc, tranche de montant |
| F8 | Consultation des dossiers | Fiche complète d'un dossier, accès au PDF original, liste paginée et triable des dossiers |
| F9 | Génération de rapports | Nouveau PDF reproduisant la structure du rapport d'origine à partir des données en base ; export Excel d'un dossier ou d'une liste filtrée |
| F10 | Graphiques et statistiques | Tableau de bord : nombre de dossiers et montants par mois, répartition par procédure et par zone de choc, coût moyen par sinistre, part main d'œuvre / fournitures |
| F11 | Utilisateurs | Connexion sécurisée ; rôles Administrateur, Gestionnaire (import, validation, modification) et Consultation (lecture et export) ; gestion des comptes par l'administrateur |
| F12 | Valeurs clés du commentaire (rapports 2026) | Pour les rapports de 2026, lecture du commentaire de l'expert et extraction des valeurs importantes qu'il contient (montants, dates, références, décisions) en champs structurés, consultables, filtrables et utilisables dans les statistiques ; liste exacte des valeurs définie avec LT Expertise à partir de commentaires réels ; texte complet du commentaire toujours conservé |
| F13 | Délais de traitement des dossiers | Calcul de la durée de chaque dossier, du démarrage (date de mission) à la finalisation (date du rapport), et par étape : mission, expertise du véhicule, avant réparation, après travaux, rapport ; durée moyenne par mois, par compagnie, par procédure et par expert ; évolution dans le temps ; liste des dossiers qui dépassent un délai cible paramétrable |
| F14 | Synchronisation avec le fichier Excel des rapports en cours | Import du fichier Excel existant de suivi des rapports en cours, à la demande ou à intervalle régulier ; correspondance des colonnes paramétrable ; rapprochement par n° de rapport ou réf. sinistre ; création des dossiers « En cours », complétés automatiquement quand le PDF final est importé ; export au même format pour garder le fichier Excel à jour |

## 7. Fonctionnalités complémentaires

Les quatre premières fonctionnalités font partie du projet ; les suivantes sont proposées en option, pour une phase ultérieure.

**Incluses :**

- **Import en lot** : dépôt d'un dossier entier de PDF, traité en arrière-plan avec un récapitulatif (réussis, à compléter, en erreur).
- **Alertes d'anomalies** : liste des dossiers dont un contrôle de cohérence échoue, ou dont le taux horaire s'écarte du taux habituel.
- **Journal d'audit** : trace de chaque import, modification, validation et export, avec l'utilisateur et la date.
- **Export de la liste filtrée** : les résultats d'une recherche exportés en un clic vers Excel.

**En option :**

- Statistiques par réparateur et par expert (heures moyennes, coût moyen, taux de pièces de réemploi).
- Rapport de synthèse mensuel généré automatiquement en PDF.
- Prise en charge d'un second modèle de PDF (autre cabinet ou nouvelle version du gabarit).

## 8. Exigences non fonctionnelles

Les rapports contiennent des données personnelles (noms, immatriculations, n° de police) : la sécurité et la confidentialité sont prioritaires.

| Domaine | Exigence |
| --- | --- |
| Authentification | Spring Security : connexion par identifiant et mot de passe ; mots de passe chiffrés (hachage) ; jetons d'accès JWT à durée limitée ; déconnexion automatique après inactivité ; verrouillage temporaire après 5 tentatives échouées |
| Autorisations | Droits par rôle vérifiés côté serveur sur chaque appel de l'API, pas seulement dans l'interface |
| Sécurité des échanges | Accès chiffré (HTTPS) sur le réseau interne ; protection contre les injections SQL, XSS et CSRF ; contrôle du type et de la taille des fichiers importés |
| Confidentialité | Données stockées en local, dans les locaux de LT Expertise ; aucun envoi à un service tiers ; respect de la loi 09-08 sur la protection des données personnelles |
| Sauvegarde | Sauvegarde automatique quotidienne de la base et des PDF sur un disque séparé, conservée 30 jours ; procédure de restauration testée avant la mise en production |
| Performance | Extraction d'un rapport en moins de 5 secondes ; recherche en moins de 2 secondes sur plusieurs milliers de dossiers |
| Fiabilité | Aucune donnée enregistrée comme validée sans contrôle de cohérence ou validation humaine ; PDF original toujours conservé |
| Ergonomie | Interface en français, utilisable sur ordinateur et tablette, sans formation technique |
| Maintenabilité | Application organisée en modules indépendants ; déploiement par conteneurs Docker |

## 9. Tests, déploiement, documentation et formation

L'application est livrée testée, installée en local chez LT Expertise et documentée ; ses utilisateurs sont formés à son usage.

**Tests**

- Tests automatiques de l'extraction sur un jeu d'au moins 20 rapports réels fournis par le client.
- Tests de l'API (droits d'accès, création, modification) et des contrôles de cohérence.
- Recette par le client sur un environnement de test avant la mise en production, avec une liste de vérifications signée.

**Déploiement**

- Installation en local, sur un poste ou un serveur situé dans LT Expertise : application, base PostgreSQL et serveur web, déployés par conteneurs Docker.
- Accès à l'application depuis les postes du réseau interne, par le navigateur, sans installation sur chaque poste.
- Mise en place des sauvegardes automatiques et vérification d'une restauration complète.

**Documentation**

- Guide utilisateur illustré (import, vérification, recherche, génération, statistiques).
- Documentation technique : installation, configuration, sauvegarde et restauration, documentation de l'API.

**Formation**

- Des  séances de formation pour les utilisateurs, sur site ou à distance.

## 10. Planning et livrables

Le projet démarre le Oct 13, 2026, date confirmée avec LT Expertise, et dure 8 semaines : l'application est mise en service en local le Dec 7, 2026.

&#91;embedded content: planning · 8 semaines, 3 jalons\]

| Jalon | Date | Livrables |
| --- | --- | --- |
| Démarrage | Oct 13, 2026 | Signature du cahier des charges, réception de 20 rapports réels |
| Maquettes validées | Oct 19, 2026 | Modèle de données, maquettes des écrans |
| Version de test | Nov 16, 2026 | Import, extraction, vérification, recherche et consultation utilisables |
| Mise en service locale | Dec 7, 2026 | Application installée chez LT Expertise, guides utilisateur et technique, formation réalisée, recette signée |
| Fin de garantie | Mar 7, 2027 | Corrections des anomalies sur les fonctionnalités livrées |

## 12. Hypothèses, exclusions et validation

Le périmètre et le délai reposent sur les hypothèses ci-dessous ; toute demande hors de ce cadre fera l'objet d'un avenant.

**Hypothèses**

- Les rapports suivent le modèle analysé en section 2 : PDF généré par logiciel, 2 pages, non scanné.
- Le projet démarre le 13/10/2026, date confirmée.
- LT Expertise fournit au moins 20 rapports réels représentatifs au démarrage.

**Exclusions**

- Lecture de PDF scannés (OCR) ou d'autres modèles de rapports.
- Hébergement en ligne et accès depuis l'extérieur du cabinet, prévus pour une phase ultérieure.
- Application mobile native.
- Reprise d'un historique de données provenant d'un autre logiciel, en dehors du fichier Excel des rapports en cours (F14).
- Connexion au système informatique des compagnies d'assurance.

**Validation**

|  | Nom | Fonction | Date | Signature |
| --- | --- | --- | --- | --- |
| Pour LT Expertise |  |  |  |  |
| Pour le prestataire |  |  |  |  |
