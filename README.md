# 🪦 ReviveShrine Plugin

Een Paper-plugin voor Minecraft Hardcore-achtige servers waar spelers gerevived kunnen worden in ruil voor een offer. 

## 🌍 Server Compatibiliteit

- **PaperMC 1.20.4**
- Werkt ook op toekomstige versies (zoals 1.21.x) mits Paper support

## ⚙️ Features

- ✅ Revive dode spelers met een in-game offer (64 diamonds, aanpasbaar)
- ✅ Diamond blokken worden geplaatst in een **5x5 grid** om het aantal revives visueel weer te geven
- ✅ Volledig configureerbare prijs (per item)
- ✅ Houdt automatisch bij wie dood is
- ✅ Eén revive-kist voor alle spelers
- ✅ Revives worden opgeslagen bij server herstart
- ✅ Commands voor setup en overzicht

---

## 🧪 Installatie

1. Plaats de `.jar` in je `plugins/` folder
2. Start je server
3. Gebruik onderstaande commands om alles in te stellen

---

## 💻 Commands

| Command                | Beschrijving                                               | Perms nodig |
|------------------------|------------------------------------------------------------|--------------|
| `/setrevivechest`      | Zet de locatie van de revive-kist                         | OP           |
| `/setrevivecounter`    | Zet de locatie waar diamond blocks worden geplaatst        | OP           |
| `/revivelist`          | Laat zien wie er dood is                                   | Iedereen     |

---

## 🧠 Hoe werkt het?

- Wanneer een speler doodgaat, wordt deze geregistreerd als “dood”
- Spelers kunnen **64 diamonds** (of een ander aantal) in de revive chest stoppen
- Bij voldoende diamonds wordt een dode speler willekeurig gerevived
- Bij elke revive wordt een **diamond block geplaatst** in een 5x5 grid die omhoog stapelt per 25 revives

---

## ⚙️ Configuratie

De revive prijs is instelbaar in `config.yml` (na eerste run):
```yaml
revive-price:
  DIAMOND: 64
