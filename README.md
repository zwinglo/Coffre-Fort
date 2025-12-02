# Coffre-Fort

Application Android Java pour la gestion sécurisée de documents avec authentification par mot de passe et automatisation de la messagerie (SMS/MMS).

## Fonctionnalités

### 🔒 Sécurité
- **Authentification par mot de passe** : Protection de l'accès à l'application
- Création et validation du mot de passe
- Stockage sécurisé dans une base de données SQLite
- Changement de mot de passe et purge complète disponibles depuis l'écran des paramètres

### 📄 Gestion des Documents
- **Ajout manuel de documents** : Création de documents texte, image, ou média
- **Organisation par catégories** :
  - Texte
  - Images
  - Média
  - Autre
- **Affichage détaillé** : Vue complète de chaque document
- **Suppression de documents** : Avec confirmation de l'utilisateur

### 📩 Messagerie et sauvegardes
- **Ingération automatique des SMS/MMS** : Import et mise à jour des messages reçus ou envoyés avec rafraîchissement de la base locale
- **Sauvegarde et consultation hors ligne** : Historique des SMS/MMS (adresse, date, contenu) stocké dans SQLite avec indicateur de lecture
- **Pièces jointes MMS** : Téléchargement et ouverture via un FileProvider sécurisé
- **Synchronisation manuelle** : Bouton de synchronisation dédié sur l'écran des messages pour relancer l'import
- **Supervision en temps réel** : Récepteurs exportés pour les SMS/MMS entrants et messages sauvegardés

### ✉️ Automatisation email
- **Transfert automatique** : Envoi des SMS/MMS vers une boîte mail configurée, avec formatage du sujet et du corps
- **Test de configuration** : Bouton d'envoi de mail de test depuis les paramètres
- **Paramètres SMTP** : Hôte, port, utilisateur, destinataire et TLS configurables dans l'application

### 🎨 Interface Utilisateur
- **Liste de documents** : Affichage avec RecyclerView
- **Filtrage par catégorie** : Spinner pour sélectionner une catégorie
- **Affichage adapté** : Cards avec titre, catégorie et aperçu du contenu
- **Floating Action Button** : Ajout rapide de documents
- **Liste de messages** : Vue dédiée aux SMS/MMS sauvegardés avec accès au détail et aux pièces jointes
- **Écran Paramètres** : Résumé du nombre de documents, état des autorisations SMS/MMS et configuration email

## Architecture

### Structure du Projet
```
app/
├── src/main/
│   ├── java/com/coffre/fort/
│   │   ├── LoginActivity.java            # Écran d'authentification
│   │   ├── MainActivity.java             # Liste des documents
│   │   ├── AddDocumentActivity.java      # Ajout de documents
│   │   ├── DocumentDetailActivity.java   # Détails d'un document
│   │   ├── MessageListActivity.java      # Liste des SMS/MMS sauvegardés
│   │   ├── MessageDetailActivity.java    # Détail d'un SMS/MMS et pièces jointes
│   │   ├── SettingsActivity.java         # Paramètres généraux (mot de passe, permissions, email)
│   │   ├── EmailSettingsActivity.java    # Configuration SMTP et test d'envoi
│   │   ├── SmsReceiver.java / MmsReceiver.java # Récepteurs pour les messages entrants
│   │   ├── MessageSyncManager.java       # Synchronisation et sauvegarde SMS/MMS
│   │   ├── DatabaseHelper.java           # Gestion SQLite (documents, auth, messages, pièces jointes)
│   │   ├── Document.java                 # Modèle de données
│   │   └── DocumentAdapter.java          # Adaptateur RecyclerView
│   ├── res/
│   │   ├── layout/                     # Layouts XML (documents, messages, paramètres)
│   │   ├── values/                     # Strings, couleurs, styles
│   │   └── mipmap/                     # Icônes de l'application
│   └── AndroidManifest.xml
└── build.gradle
```

### Technologies Utilisées
- **Android SDK** : API 24+ (Android 7.0 et supérieur)
- **Java** : Version 8
- **SQLite** : Base de données locale
- **AndroidX** :
  - AppCompat
  - Material Components
  - RecyclerView
  - CardView
  - ConstraintLayout

## Installation

### Prérequis
- Android Studio Arctic Fox ou version ultérieure
- JDK 8 ou supérieur
- Android SDK avec API 24+

### Étapes d'Installation
1. Cloner le repository :
   ```bash
   git clone https://github.com/zwinglo/Coffre-Fort.git
   ```

2. Ouvrir le projet dans Android Studio

3. Synchroniser les dépendances Gradle

4. Exécuter l'application sur un émulateur ou appareil physique

## Utilisation

### Première Utilisation
1. **Créer un mot de passe** :
   - Entrer un mot de passe dans le champ
   - Cliquer sur "Créer un mot de passe"

### Connexion
1. **Se connecter** :
   - Entrer le mot de passe
   - Cliquer sur "Se connecter"

### Gestion des Documents
1. **Ajouter un document** :
   - Cliquer sur le bouton "+" (FAB)
   - Remplir le titre
   - Sélectionner une catégorie
   - Entrer le contenu
   - Cliquer sur "Enregistrer"

2. **Consulter un document** :
   - Cliquer sur un document dans la liste
   - Voir tous les détails

3. **Filtrer par catégorie** :
   - Utiliser le spinner en haut de l'écran
   - Sélectionner "Toutes les catégories" ou une catégorie spécifique

4. **Supprimer un document** :
   - Ouvrir les détails du document
   - Cliquer sur "Supprimer"
   - Confirmer la suppression

### Gestion des Messages (SMS/MMS)
1. **Synchroniser les messages** :
   - Accéder au menu "Messages"
   - Accorder les autorisations SMS/MMS demandées
   - Appuyer sur "Synchroniser" pour importer les conversations existantes

2. **Consulter un message** :
   - Sélectionner un message dans la liste
   - Visualiser l'adresse, la date, le type (SMS ou MMS) et le contenu

3. **Ouvrir les pièces jointes** (MMS) :
   - Dans le détail du message, choisir une pièce jointe
   - L'ouverture se fait via un intent sécurisé (FileProvider)

4. **Transférer par email** :
   - Configurer l'envoi dans **Paramètres > Configuration email** (hôte, port, identifiants, destinataire, TLS)
   - Un mail formaté est envoyé automatiquement lors de la réception ou de la sauvegarde d'un message

## Base de Données

### Tables
#### `documents`
- `id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `title` : TEXT
- `content` : TEXT
- `category` : TEXT
- `timestamp` : INTEGER

#### `auth`
- `password` : TEXT

#### `messages`
- `local_id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `provider_id` : INTEGER (identifiant SMS/MMS du téléphone)
- `provider_type` : TEXT (`SMS` ou `MMS`)
- `address` : TEXT (expéditeur ou destinataire)
- `date` : INTEGER (timestamp)
- `body` : TEXT
- `box_type` : INTEGER (inbox/sent)
- `has_attachments` : INTEGER (0/1)

#### `attachments`
- `id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `message_id` : INTEGER (clé étrangère vers `messages`)
- `provider_part_id` : TEXT (identifiant de la pièce jointe côté MMS)
- `path` : TEXT (chemin local sécurisé)
- `content_type` : TEXT
- `size` : INTEGER

## Sécurité

- **Hashing du mot de passe** : Le mot de passe est hashé avec SHA-256 avant stockage
- **Base de données SQLite locale** : Tous les documents sont stockés localement sur l'appareil
- **Authentification obligatoire** : L'application nécessite une authentification à chaque ouverture
- **Permissions contrôlées** : Seules les autorisations SMS/MMS nécessaires à la synchronisation sont demandées ; le reste des
  données fonctionne hors ligne

## Évolutions Futures Possibles

- [ ] Chiffrement avancé du mot de passe (PBKDF2 ou bcrypt)
- [ ] Support des fichiers image et média réels
- [ ] Exportation/Importation de documents
- [ ] Sauvegarde cloud optionnelle
- [ ] Recherche de documents
- [ ] Tri personnalisé
- [ ] Thème sombre
- [ ] Biométrie (empreinte digitale)

## Licence

Ce projet est développé dans un cadre éducatif.

## Auteur

zwinglo
