# Bounty SMP (v1.1.0)

Plugin Paper 1.21.x pour un SMP de chasse aux primes : monnaie **Bounty Coins**, primes sur les joueurs,
boutique, quêtes quotidiennes, offres du jour, têtes de trophée, combat tag, marchand PNJ et menus personnalisables.

## Installation
1. Compile avec GitHub Actions (le workflow `.github/workflows/build.yml` produit l'artifact **BountySMP-jar**).
2. Mets le `.jar` dans `plugins/`, **un seul jar BountySMP** (supprime les anciens), puis redémarre.
3. Au démarrage, la console doit afficher `BountySMP v1.1.0 chargé : 13 succès, /stats, ...` : c'est comme ça que tu vérifies que le bon jar tourne.
4. (Recommandé) Installe le resource pack `BountySMP-ResourcePack.zip` : hébergé en lien direct, puis dans `server.properties`
   `resource-pack=<lien>`, `resource-pack-sha1=<hash>`, `require-resource-pack=true`. Sans pack : `resource-pack.enabled: false` dans la config.
5. Fais `/setworldspawn` au centre de ton spawn (la zone sans PvP et la Pierre de Rappel s'en servent).

## Commandes joueurs
| Commande | Effet |
|---|---|
| `/bounty` | Menu principal (boutique, recherchés, top, quêtes, personnalisation) |
| `/shop` | Ouvre la boutique |
| `/sell` | Menu de vente : dépose minerais/têtes, ferme avec Échap pour encaisser |
| `/balance` | Affiche ton solde et ta prime |
| `/stats [joueur]` | Statistiques : kills, morts, K/D, primes encaissées, succès... |
| `/pay <joueur> <montant>` | Envoie des coins (aussi `/bounty pay`) |
| `/bounty add <joueur> <montant>` | Met une prime sur un joueur (tes coins sont dépensés) |

## Commandes admin (permission `bountysmp.admin`)
| Commande | Effet |
|---|---|
| `/bounty reload` | Recharge `config.yml` sans redémarrer |
| `/bounty addcoins` / `removecoins <joueur> <montant>` | Donne / retire des coins |
| `/bounty addadmin <joueur> <montant>` | Ajoute une prime sans payer |
| `/bounty daily reset <quests\|deals\|all> [joueur]` | Remet à zéro progression / achats du jour |
| `/bounty daily reroll <quests\|deals\|all>` | Génère de nouvelles quêtes / offres pour tout le monde |
| `/bounty merchant spawn` / `remove` | Place / retire un Marchand Pirate (clic droit = boutique) |
| `/bounty inv <joueur>` / `enderchest <joueur>` | Voir et modifier l'inventaire / l'enderchest d'un joueur connecté |
| `/bounty reset CONFIRMER` | Efface TOUTES les données (coins, primes, stats, succès des joueurs connectés) |

## Fonctionnement
- **Kills** : tuer un joueur sans prime rapporte `kill-reward`. Tuer un joueur **recherché** encaisse toute sa prime
  et ne fait pas monter la tienne. Enchaîner les kills fait monter ta prime tout seul (**danger**).
- **Tracker** (page Objets) : Maj + clic droit pour choisir une cible dans le menu Recherchés, clic droit pour ses coordonnées approximatives.
- **Boutique** : 4 pages (Kits, Objets, Marché Noir, Offres du jour). Items : Tracker, Feu d'Artifice du Fugitif, Fiole de Rage,
  Grenade du Chaos, Élan du Chasseur, Pierre de Rappel, Faux Lingot d'Or, Bombe Puante, Bourse Chanceuse, Œil de Judas, Bourse Trouée.
- **Quêtes** : 3 par jour (facile / moyenne / difficile), récompense automatique + bonus si les 3 sont finies.
- **Offres du jour** : 4 objets en promo, 1 achat par offre et par jour, renouvelées à minuit.
- **Têtes de trophée** : un joueur tué en PvP laisse sa tête, à vendre dans `/sell` (valeur plus haute si la victime était recherchée).
- **Combat tag** : se déconnecter en combat = mort. **Zone sans PvP** autour du spawn, **protection** des nouveaux joueurs.
- **Succès** (13, popups Minecraft) : Premier Achat, Première Prime, Chasseur Aguerri, Premier Sang, Tueur en Série, Chasseur de Légende,
  Millionnaire, Voleur, Sur la Liste, Tête Mise à Prix, Collectionneur, Bon Plan, Assidu.
- **Menu personnalisable** : le bouton palette du menu `/bounty` permet de choisir parmi 8 thèmes (choix sauvegardé par joueur).

## Configuration (`plugins/BountySMP/config.yml`)
Tout est commenté dans le fichier. Après modification : `/bounty reload`. Ce que tu peux régler :
- **Économie** : récompense de kill, anti-farm, paliers de danger, valeur des têtes, prix de chaque objet, taxe de `/pay`.
- **Vente** (`sell.rates`) : ajoute n'importe quel objet, par exemple `COAL: 1` ou `ANCIENT_DEBRIS: 40`.
- **Boutique** : masquer un objet (`enabled: false`), changer son nom ou sa description (bloc `shop.items`, exemple dans le fichier).
- **Objets** : cooldowns, durées et niveaux des effets, probabilités de la Bourse Chanceuse, imprécision du Tracker...
- **Quêtes et offres** : objectifs par type, récompenses, nombre d'offres, réductions min/max, objets exclus des promos.
- **PvP** : durée du combat tag, rayon de la zone sans PvP, protection des nouveaux joueurs.
- **Apparence** : nom du marchand, resource pack activé ou non, fuseau horaire du renouvellement quotidien.

Exemples rapides :
```yaml
sell:
  rates:
    COAL: 1            # le charbon se vend maintenant 1 coin

shop:
  items:
    tracker:
      name: "&6Super Tracker"
    netherite_kit:
      enabled: false   # masqué de la boutique
```

## Les fichiers
- `plugins/BountySMP/config.yml` : **c'est lui que tu modifies**.
- `plugins/BountySMP/data.yml` : données des joueurs (coins, primes, stats, thème, quêtes). Ne le modifie que serveur **arrêté**.
- `plugin.yml` (dans le jar) : déclaration des commandes et permissions, il n'est pas fait pour être édité.
- Pour changer des **textes** (messages, noms d'objets donnés aux joueurs) qui ne sont pas dans `config.yml`, il faut modifier le code : demande-le.

## Dépannage
- **Une commande ou un succès n'existe pas** : un ancien jar tourne. Regarde la version dans le log de démarrage, vérifie qu'il n'y a qu'un jar BountySMP dans `plugins/`, et que le jar vient bien du dernier build GitHub (vert).
- **Menus avec des carrés bizarres / icônes en papier** : le resource pack n'est pas chargé (ou pas à jour : change le `resource-pack-sha1`).
- **Un joueur n'apparaît pas dans les recherchés** : il doit être connecté ou s'être déjà connecté au serveur.
- **Message `sell.rates : matériau inconnu`** dans la console : un nom d'objet est mal écrit dans la config (MAJUSCULES, anglais, avec `_`).
