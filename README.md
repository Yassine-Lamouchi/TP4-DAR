# Faculté des Sciences de Sfax

**Département d'Informatique et des Communications**  
**Matière :** Développement d'applications réparties (DAR)  
**Responsable du cours :** Amal ABID  
**Enseignante des TPs :** Amal ABID  
**Année Universitaire :** 2026-2027  
**Auditoire :** LSI 3 (Groupe 2.1)

---

# Compte Rendu — TP 4 : Sockets en mode non-connecté UDP

### Service de Chat Client / Serveur Multi-Clients en Temps Réel

- **Étudiant :** Yassine Lamouchi
- **Groupe :** LSI3 2.1
- **Activité :** Échange de datagrammes en mode non-connecté — Service de Chat UDP

---

## Sommaire

1. [Introduction & Objectifs](#1-introduction--objectifs)
2. [Structure et Distribution des Fichiers dans `TP4-DAR`](#2-structure-et-distribution-des-fichiers-dans-tp4-dar)
3. [Description Détaillée des Étapes](#3-description-détaillée-des-étapes)
   - [Étape 2 : Chat Unidirectionnel (Client unique vers Serveur)](#étape-2--chat-unidirectionnel-client-unique-vers-serveur)
   - [Étape 3 : Chat Bidirectionnel Multi-Clients avec Threads](#étape-3--chat-bidirectionnel-multi-clients-avec-threads)
4. [Guide d'Exécution Pas-à-Pas](#4-guide-dexécution-pas-à-pas)
   - [Exécution sous IntelliJ IDEA](#a-exécution-sous-intellij-idea)
   - [Astuce d'affichage côte à côte des consoles](#b-astuce-daffichage-côte-à-côte-des-consoles-dans-intellij)
   - [Exécution en Ligne de Commande (Terminal)](#c-exécution-en-ligne-de-commande-terminal)
5. [Captures d'Écran et Synthèse des Tests](#5-captures-décran-et-synthèse-des-tests)
   - [Capture 1 : Serveur UDP Unidirectionnel (Étape 2)](#capture-1--serveur-udp-unidirectionnel-serveur4_2--étape-2)
   - [Capture 2 : Serveur UDP Multi-Clients (Étape 3)](#capture-2--serveur-udp-multi-clients-serveur4_3--étape-3)
   - [Capture 3 : Client "Moahamed" (Étape 3)](#capture-3--client-moahamed-client4_3--étape-3)
   - [Capture 4 : Client "Salma" (Étape 3)](#capture-4--client-salma-client4_3--étape-3)
   - [Synthèse du Fonctionnement Multi-Clients](#synthèse-du-fonctionnement-multi-clients)
6. [Conclusion & Notions Clés](#6-conclusion--notions-clés)

---

## 1. Introduction & Objectifs

Le protocole **UDP (User Datagram Protocol)** fonctionne en mode **non-connecté**. Contrairement à TCP :

- Il n'y a pas d'établissement de connexion préalable (`connect` / `accept`).
- Les données sont encapsulées dans des unités autonomes appelées **datagrammes** (`DatagramPacket`).
- La communication s'effectue via des points de terminaison représentés par `DatagramSocket`.

### Objectifs atteints dans ce TP :

1. Maîtriser la création, l'émission et la réception de datagrammes UDP via `DatagramSocket` et `DatagramPacket` en Java.
2. Implémenter une communication unidirectionnelle point-à-point (Étape 2).
3. Concevoir une architecture de **Chat bidirectionnel multi-clients** en temps réel (Étape 3) :
   - Gestion de threads concurrents (émission et réception asynchrones).
   - Maintien d'un registre dynamique d'adresses (`SocketAddress`) pour simuler la présence de clients connectés.
   - Diffusion sélective (_broadcasting_) des messages à tous les pairs connectés, à l'exception de l'expéditeur.
   - Capacité du serveur à intervenir et communiquer directement avec les clients.

---

## 2. Structure et Distribution des Fichiers dans `TP4-DAR`

Le projet est organisé dans IntelliJ selon une séparation claire entre le code serveur et le code client au sein du dossier `src/` :

```text
TP4-DAR/
│
├── screenshots/
│   ├── Screenshot 2026-11-01 193753.png   # Capture Serveur4_2 (Étape 2)
│   ├── Screenshot 2026-11-01 194133.png   # Capture Serveur4_3 Multi-Clients (Étape 3)
│   ├── Screenshot 2026-11-01 194147.png   # Capture Client "Moahamed" (Étape 3)
│   └── Screenshot 2026-11-01 194211.png   # Capture Client "Salma" (Étape 3)
│
├── src/
│   ├── ClientPackage/
│   │   ├── Client4_2.java       # Étape 2 : Client UDP unidirectionnel simple
│   │   └── Client4_3.java       # Étape 3 : Client UDP multi-thread (Envoi / Réception)
│   │
│   └── ServeurPackage/
│       ├── Serveur4_2.java      # Étape 2 : Serveur UDP écoutant sur le port 1234
│       └── Serveur4_3.java      # Étape 3 : Serveur UDP avec diffusion et émission
│
└── README.md                    # Rapport complet et documentation du TP
```

### Rôle des Fichiers :

| Fichier               | Package          | Description                                                                                                                                                                                                                                                              |
| :-------------------- | :--------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`Serveur4_2.java`** | `ServeurPackage` | Serveur UDP lié au port `1234` recevant un datagramme et affichant son contenu ainsi que la taille.                                                                                                                                                                      |
| **`Client4_2.java`**  | `ClientPackage`  | Client UDP créant un `DatagramPacket` contenant la chaîne `" Mon message "` et l'expédiant vers `localhost:1234`.                                                                                                                                                        |
| **`Serveur4_3.java`** | `ServeurPackage` | Serveur central de chat : gère la liste dynamique des clients (`List<SocketAddress>`), retransmet les messages à tous les autres clients, souhaite la bienvenue aux nouveaux arrivants et permet à l'administrateur serveur d'écrire des messages via son propre thread. |
| **`Client4_3.java`**  | `ClientPackage`  | Client de chat multi-thread : un thread d'arrière-plan écoute et affiche les messages entrants du serveur, tandis que le thread principal lit les saisies utilisateur (`Scanner`), envoie les messages et gère la commande de sortie (`exit`).                           |

---

## 3. Description Détaillée des Étapes

### Étape 2 : Chat Unidirectionnel (Client unique vers Serveur)

- **Serveur (`Serveur4_2`) :**
  - Instancie une `DatagramSocket` sur le port `1234`.
  - Entre dans une boucle infinie avec `socket.receive(paquet)` pour bloquer en attente d'un datagramme.
  - Reconstitue la chaîne reçue à partir du buffer d'octets et l'affiche.

- **Client (`Client4_2`) :**
  - Crée une socket locale avec un port aléatoire (`new DatagramSocket()`).
  - Convertit la chaîne à transmettre en tableau d'octets via `ch.getBytes()`.
  - Construit un `DatagramPacket` avec l'adresse IP (`InetAddress.getByName("localhost")`) et le port cible `1234`.
  - Expédie le paquet au serveur via `socket.send(donneesEmises)`.

---

### Étape 3 : Chat Bidirectionnel Multi-Clients avec Threads

Cette étape résout les limites d'un échange bloquant séquentiel en introduisant le multi-threading côté client et côté serveur :

#### 1. Côté Serveur (`Serveur4_3`) :

- **Enregistrement des adresses :** En UDP, le serveur ne dispose pas d'objets `Socket` persistants. Il enregistre l'adresse distante (`SocketAddress` = IP + Port) extraite de chaque paquet reçu dans une liste synchronisée :
  ```java
  SocketAddress adresseExpediteur = paquet.getSocketAddress();
  if (!listeClients.contains(adresseExpediteur)) {
      listeClients.add(adresseExpediteur);
      // Envoi direct d'un message d'accueil personnalisé
      socket.send(new DatagramPacket(msgBienvenue, length, adresseExpediteur));
  }
  ```
- **Diffusion (Broadcasting) :** Dès qu'un client transmet un message, le serveur itère sur sa liste et le réexpédie à **tous les autres clients**, en excluant l'expéditeur :
  ```java
  for (SocketAddress client : listeClients) {
      if (!client.equals(adresseExpediteur)) {
          socket.send(new DatagramPacket(donnees, donnees.length, client));
      }
  }
  ```
- **Capacité d'expression du Serveur :** Un thread dédié `threadEnvoiServeur` surveille la console du serveur (`Scanner`). Tout message saisi par le serveur est diffusé à l'ensemble des clients sous le préfixe `[Serveur]: ...`.

#### 2. Côté Client (`Client4_3`) :

- **Thread de Réception (Daemon) :** Tourne en boucle fermée sur `socket.receive()`. Dès qu'un paquet arrive du serveur (qu'il provienne d'un pair ou du serveur lui-même), il est instantanément affiché dans la console.
- **Thread d'Envoi (Main Thread) :**
  1. Demande le nom d'utilisateur (pseudo).
  2. Envoie immédiatement un paquet d'initialisation : `[Nom] a rejoint le chat !`. Cela permet au serveur d'enregistrer le client avant même son premier message.
  3. Lit les messages rédigés au clavier, affiche `[Moi]: ...` en local et transmet `[Nom]: ...` au serveur.
  4. Intercepte la saisie `exit` pour notifier le serveur (`[Nom] a quitté le chat.`) et fermer proprement les sockets.

---

## 4. Guide d'Exécution Pas-à-Pas

### A. Exécution sous IntelliJ IDEA

#### Pour l'Étape 2 :

1. Faites un clic droit sur `ServeurPackage/Serveur4_2.java` $\rightarrow$ **Run 'Serveur4_2.main()'**.
2. Faites un clic droit sur `ClientPackage/Client4_2.java` $\rightarrow$ **Run 'Client4_2.main()'**.
3. Observez la console du serveur affichant le message reçu.

#### Pour l'Étape 3 (Multi-clients) :

1. **Lancer le serveur :** Exécutez `ServeurPackage/Serveur4_3.java`.
2. **Autoriser plusieurs instances pour le client :**
   - En haut à droite d'IntelliJ, ouvrez le sélecteur de configuration de démarrage à côté du bouton Play vert.
   - Cliquez sur **Edit Configurations...**
   - Sélectionnez la configuration `Client4_3`.
   - Cliquez sur le lien bleu **Modify options** (ou appuyez sur `Alt + V`).
   - Cochez **Allow multiple instances** (Autoriser plusieurs instances).
   - Validez en cliquant sur **Apply** puis **OK**.
3. **Lancer les clients :**
   - Lancez une 1ère instance de `Client4_3` $\rightarrow$ Saisissez le nom d'utilisateur (ex: `Moahamed`).
   - Relancez à nouveau `Client4_3` $\rightarrow$ Saisissez un 2ème nom (ex: `Yassine`).
   - Relancez une 3ème fois `Client4_3` $\rightarrow$ Saisissez un 3ème nom (ex: `Salma`).

---

### B. Astuce d'affichage côte à côte des consoles dans IntelliJ

Pour visualiser les échanges en direct sur toutes les fenêtres sans basculer d'onglet :

1. Dans le panneau inférieur **Run**, repérez les onglets `Serveur4_3`, `Client4_3`, `Client4_3 (1)`, `Client4_3 (2)`.
2. Faites un **clic droit sur l'onglet d'un client** $\rightarrow$ sélectionnez **Split Right** (Scinder vers la droite).
3. Répétez l'opération pour les autres consoles.
4. Les consoles s'alignent en colonnes côte à côte : tout message tapé dans une colonne se répercute instantanément dans les autres !

---

### C. Exécution en Ligne de Commande (Terminal)

Depuis la racine du projet `TP4-DAR` :

```bash
# 1. Compilation de tous les fichiers source
javac -d out src/ClientPackage/*.java src/ServeurPackage/*.java

# 2. Démarrage du serveur dans un premier terminal
java -cp out ServeurPackage.Serveur4_3

# 3. Démarrage des clients dans des terminaux séparés
# Terminal Client 1 :
java -cp out ClientPackage.Client4_3

# Terminal Client 2 :
java -cp out ClientPackage.Client4_3
```

---

## 5. Captures d'Écran et Synthèse des Tests

Les 4 captures d'écran ci-dessous illustrent les résultats d'exécution des étapes du TP sous IntelliJ IDEA.

### Capture 1 : Serveur UDP Unidirectionnel (`Serveur4_2` — Étape 2)

Réception du datagramme émis par le client `Client4_2` sur le port `1234` :

<img src="screenshots/Screenshot 2026-11-01 193753.png" alt="Serveur4_2 - Étape 2" width="650" />

- **Analyse :** Le serveur se met en attente bloquante sur le port `1234`. Dès l'envoi du client, le paquet est reçu et le contenu `Donnees reçues = Mon message` s'affiche directement.

---

### Capture 2 : Serveur UDP Multi-Clients (`Serveur4_3` — Étape 3)

Console du serveur central avec enregistrement dynamique des clients et diffusion des messages :

<img src="screenshots/Screenshot 2026-11-01 194133.png" alt="Serveur4_3 - Étape 3" width="650" />

- **Analyse :**
  - Détection et enregistrement automatique de chaque client dès son premier message : Moahamed, Yassine et Salma.
  - Diffusion des messages reçus aux autres pairs sans renvoi à l'expéditeur (_No-Echo_).
  - Envoi direct d'un message par l'administrateur serveur depuis sa propre console : `[Moi (Serveur)]: Bienvenue`.

---

### Capture 3 : Client "Moahamed" (`Client4_3` — Étape 3)

Console du premier client connecté :

<img src="screenshots/Screenshot 2026-11-01 194147.png" alt="Client Moahamed - Étape 3" width="650" />

- **Analyse :**
  - Réception immédiate du message d'accueil : `[Serveur]: Bienvenue dans le chat !`.
  - Notification en temps réel lorsque d'autres utilisateurs rejoignent : `[Yassine] a rejoint le chat !`, `[Salma] a rejoint le chat !`.
  - Émission de `Bonjour!` (écho local `[Moi]: Bonjour!`) et réception instantanée des messages de Yassine, du Serveur et de Salma.

---

### Capture 4 : Client "Salma" (`Client4_3` — Étape 3)

Console du troisième client connecté :

<img src="screenshots/Screenshot 2026-11-01 194211.png" alt="Client Salma - Étape 3" width="650" />

- **Analyse :**
  - Réception immédiate du message de bienvenue et des messages déjà échangés par les autres participants actifs.
  - Émission du message `Le chat est agréable!`, validé localement par `[Moi]: ...` et transmis en temps réel à l'ensemble du groupe.

---

### Synthèse du Fonctionnement Multi-Clients

Les tests confirment que l'architecture répond à l'ensemble des exigences du cahier des charges :

1. **Diffusion sélective (_No-Echo_) :** Un client ne reçoit pas de doublon de son propre message via le réseau grâce au filtre `!client.equals(adresseExpediteur)`.
2. **Rôle actif du serveur :** Le serveur ne se contente pas de router ; il possède son propre thread pour diffuser des annonces système ou dialoguer avec les clients (`[Serveur]: ...`).
3. **Absence de blocage :** L'usage de threads d'écoute en arrière-plan (`setDaemon(true)`) garantit qu'un client peut recevoir des messages à tout moment, même lorsqu'il est en train de réfléchir ou d'écrire dans son terminal.

---

## 6. Conclusion & Notions Clés

Ce TP 4 a permis d'approfondir les concepts fondamentaux de la programmation réseau en mode non-connecté avec Java :

| Notion                           | Application dans le TP                                                                                             |
| :------------------------------- | :----------------------------------------------------------------------------------------------------------------- |
| **`DatagramSocket`**             | Point d'ancrage local permettant l'émission et la réception de paquets UDP.                                        |
| **`DatagramPacket`**             | Enveloppe contenant le tableau d'octets de données, la longueur, ainsi que l'adresse IP et le port de destination. |
| **Mode non-connecté**            | Absence de circuit virtuel fixe : chaque paquet est routé indépendamment.                                          |
| **Simulation de session**        | Utilisation d'une structure de données (`List<SocketAddress>`) côté serveur pour mémoriser les correspondants.     |
| **Concurrence & Multithreading** | Séparation obligatoire des flux d'E/S (clavier vs réseau) pour obtenir un chat interactif bidirectionnel fluide.   |

---

_Compte rendu réalisé par **Yassine Lamouchi** (LSI3 2.1) — Faculté des Sciences de Sfax — 2026-2027._
