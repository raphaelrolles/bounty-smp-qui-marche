# BountySMP

Plugin Paper 1.21.x implémentant le concept **Bounty SMP** : monnaie Bounty Coins,
primes automatiques ("système de danger"), contrats entre joueurs, classement `/wanted`,
boutique avec Tracker / Contrat Amélioré / Hunter Kit, et titres de réputation.

## Compilation

Prérequis : Java 17+ et Maven.

```bash
mvn clean package
```

Le fichier `target/BountySMP-1.0.0.jar` est le plugin à déposer dans le dossier
`plugins/` de ton serveur Paper 1.21.x, puis redémarrer/reload le serveur.

## Fonctionnalités incluses

- **Bounty Coins** : monnaie séparée des diamants, stockée par joueur.
  - `+5` coins par kill classique (anti-farm : réduit fortement si tu retues
    le même joueur avant le cooldown configuré).
  - Récupérer la prime totale d'un joueur recherché en le tuant.
- **Contrats** : `/bounty set <joueur> <montant>` — paye des coins pour mettre
  une prime sur quelqu'un. Plusieurs joueurs peuvent contribuer à la même prime.
- **Système de danger automatique** : plus un joueur enchaîne les kills dans une
  fenêtre de temps (configurable), plus sa prime augmente automatiquement par paliers.
- **`/wanted`** : classement des primes en cours avec nombre de kills récents.
- **`/bounty shop`** : boutique en interface graphique (GUI), avec :
  - 🧭 **Tracker** — boussole qui pointe vers le wanted le plus proche (clic droit).
  - 📜 **Contrat Amélioré** — clic droit puis tape `<joueur> <montant>` dans le chat
    pour lancer une prime annoncée à tout le serveur avec un titre à l'écran.
  - 🍎 **Hunter Kit** — pommes dorées, pain, flèches et arc.
- **Réputation** : titres automatiques (Chasseur, Chasseur Confirmé, Master Hunter...)
  selon le nombre de primes récupérées, visibles via `/bounty` et `/bounty top`.
- **Persistance** : toutes les données sont sauvegardées dans
  `plugins/BountySMP/data.yml` (sauvegarde auto toutes les 5 min + à l'arrêt du serveur).

## Commandes

| Commande | Description |
|---|---|
| `/bounty` | Affiche tes coins, ta prime et ton titre |
| `/bounty set <joueur> <montant>` | Met une prime sur un joueur |
| `/bounty shop` | Ouvre la boutique |
| `/bounty top` | Classement des meilleurs chasseurs |
| `/wanted` | Classement des joueurs recherchés |
| `/bounty addcoins <joueur> <montant>` | (admin) Ajoute des coins |
| `/bounty removecoins <joueur> <montant>` | (admin) Retire des coins |
| `/bounty setadmin <joueur> <montant>` | (admin) Fixe une prime directement |

Permission admin : `bountysmp.admin` (op par défaut).

## Configuration

Tout est réglable dans `config.yml` généré au premier lancement :
récompense de kill, cooldown anti-farm, paliers du système de danger,
prix de la boutique, et titres de réputation.
