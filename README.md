# Coffre-Fort

Application Android Java pour la gestion sécurisée de documents avec authentification par mot de passe.

## Fonctionnalités

### 🔒 Sécurité
- **Authentification par mot de passe** : Protection de l'accès à l'application
- Création et validation du mot de passe
- Stockage sécurisé dans une base de données SQLite

### 📄 Gestion des Documents
- **Ajout manuel de documents** : Création de documents texte, image, ou média
- **Organisation par catégories** :
  - Texte
  - Images
  - Média
  - Autre
- **Affichage détaillé** : Vue complète de chaque document
- **Suppression de documents** : Avec confirmation de l'utilisateur

### 🎨 Interface Utilisateur
- **Liste de documents** : Affichage avec RecyclerView
- **Filtrage par catégorie** : Spinner pour sélectionner une catégorie
- **Affichage adapté** : Cards avec titre, catégorie et aperçu du contenu
- **Floating Action Button** : Ajout rapide de documents

## Architecture

### Structure du Projet
```
app/
├── src/main/
│   ├── java/com/coffre/fort/
│   │   ├── LoginActivity.java          # Écran d'authentification
│   │   ├── MainActivity.java           # Liste des documents
│   │   ├── AddDocumentActivity.java    # Ajout de documents
│   │   ├── DocumentDetailActivity.java # Détails d'un document
│   │   ├── Document.java               # Modèle de données
│   │   ├── DocumentAdapter.java        # Adaptateur RecyclerView
│   │   └── DatabaseHelper.java         # Gestion SQLite
│   ├── res/
│   │   ├── layout/                     # Layouts XML
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

## Sécurité

- Le mot de passe est stocké dans une base de données SQLite locale
- Tous les documents sont stockés localement sur l'appareil
- L'application nécessite une authentification à chaque ouverture
- Les permissions READ_EXTERNAL_STORAGE et WRITE_EXTERNAL_STORAGE sont déclarées pour une évolution future

## Évolutions Futures Possibles

- [ ] Chiffrement du mot de passe (hash avec SHA-256)
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
