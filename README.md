"# IR-Shop

IR-Shop è un plugin premium per server Minecraft basato su Paper/Spigot che aggiunge un sistema di shop configurabile e comandi di gestione avanzati.

## Requisiti

- Java 21
- PaperMC/Spigot 1.21+
- Vault
- PlaceholderAPI (opzionale)

## Installazione

1. Compila il plugin con Gradle.
2. Copia il file `build/libs/*.jar` nella cartella `plugins/` del tuo server.
3. Avvia o riavvia il server.
4. Configura le cartelle `plugins/IR-Shop/` per categorie, layout e negozi.

## Comandi principali

- `/shop [open|list|help] [args...]` - apre lo shop o mostra le opzioni disponibili.
- `/shopadmin [reload]` - ricarica la configurazione del plugin.

## Permessi

- `irshop.use` - permette l'accesso allo shop.
- `irshop.admin` - permette tutti i comandi amministrativi.
- `irshop.discount.10` - sconto 10% sugli acquisti.
- `irshop.discount.25` - sconto 25% sugli acquisti.
- `irshop.discount.50` - sconto 50% sugli acquisti.
- `irshop.sell.1.5` - moltiplicatore 1.5x per la vendita.
- `irshop.sell.2` - moltiplicatore 2x per la vendita.
- `irshop.sell.3` - moltiplicatore 3x per la vendita.

## Costruzione

Esegui il comando Gradle dal progetto:

```bash
gradle --no-daemon build
```

## Dettagli

- Nome plugin: `IR-Shop`
- Main class: `com.italiarevenge.iRShop.IRShop`
- Versione attuale: `1.3.0`
- API Minecraft: `1.21`
- Sito: https://github.com/ItaliaRevengee/IR-Shop
" 
