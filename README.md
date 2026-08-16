# MagicDonutUtils

Client-side utility mod for **DonutSMP**.

- **Mod id:** `magicdonututils`
- **Minecraft:** `1.21.11`
- **Loader:** Fabric
- **Java:** 21

## Features

### Pay Confirm (safer `/pay`)
MagicDonutUtils registers a **client-side** `/pay` command that forwards the payment to the server, but can require an extra confirmation step for large amounts.

- If enabled and the amount is **>= Confirm Threshold**, the mod will not immediately send the payment.
- Instead, it shows clickable chat buttons:
  - **Confirm:** runs `/payconfirm` (client-side) which sends the stored `pay <target> <amount>` command to the server
  - **Cancel:** runs `/paycancel` (client-side) which discards the pending payment
- Pending payments expire after the configured timeout.
- Amount parsing supports suffixes: `k`, `m`, `b` (example: `250k`, `1.5m`).

### QuickPay (open `/pay <player> ...` instantly)
Adds a keybinding (default **Left Control**) used as a modifier:

- Hold the keybind and **right-click a player** to open chat with `/pay <player> ` prefilled.

### Donation HUD (highest donation tracker)
Tracks donations by parsing incoming chat messages matching:

`<name> paid you $<amount><suffix>`

Examples: `Steve paid you $500`, `.BedrockGuy paid you $2.5m`

- Stores the **highest** donation seen and renders it as a small HUD overlay.
- Includes a built-in editor to drag the HUD around and change its size.
- Command:
  - `/donation reset` clears the stored highest donation.

### Overhead Stats (stats above player name tags)
Renders a selected DonutSMP stat above other players’ name tags (not your own).

- Stats supported: Money, Shards, Playtime, Kills, Deaths
- Modes:
  - **Fixed:** always show one stat
  - **Cycle:** rotate through stats every N seconds

This feature uses the DonutSMP API:

- Requests: `https://api.donutsmp.net/v1/stats/<username>` with `Authorization: Bearer <apiKey>`
- Requires an API key (see below)
- Has basic throttling and caching (refresh interval is configurable; hard cap is ~200 requests/minute)

## Installation (Players)

1. Install **Fabric Loader** for Minecraft `1.21.11`.
2. Put `MagicDonutUtils` and its dependencies into your `.minecraft/mods` folder.
3. Launch the game.

### Dependencies
Required:
- Fabric API (dev version used here: `0.141.3+1.21.11`)
- Fabric Language Kotlin (dev version used here: `1.13.9+kotlin.2.3.10`)
- YetAnotherConfigLib v3 / YACL (dev version used here: `3.8.2+1.21.11-fabric`)

Optional (but recommended):
- Mod Menu (dev version used here: `17.0.0-beta.2`) for a convenient config button

## Configuration

Config file is saved as:
- `.minecraft/config/magicdonut.json5`

Open the config UI via:
- Mod Menu (if installed), or
- Client command: `/magicdonututils`

Config categories:
- **Pay Confirm:** enable/disable, confirm threshold, timeout
- **Donation:** enable/disable, colors, title/text, box settings, open the HUD editor
- **Overhead Stats:** enable/disable, stat selection, cycle settings, Y offset, scale
- **Dev:** DonutSMP API key and refresh rate for overhead stats

## DonutSMP API Key

Overhead Stats requires a DonutSMP API key.

- You can paste the key into the config UI under **Dev -> API Key**.
- If you are connected to a server whose IP matches `donutsmp.net` and your API key is empty, the mod also listens for chat messages like:
  - `API Key is: <32-char-token>`
  - `API Token is: <32-char-token>`
  If such a message appears, it automatically saves the token into `magicdonut.json5`.

## Development Setup (Build From Source)

Prerequisites:
- JDK 21 installed

Common tasks (Windows PowerShell):

```powershell
cd C:\Users\elias\IdeaProjects\MagicDonutUtils-1.21.11
.\gradlew.bat runClient
.\gradlew.bat build
```

Build output:
- `build\libs\MagicDonutUtils-<version>.jar`

Notes:
- The project uses Fabric Loom with split `main` and `client` source sets.
- `DevAuth` is included as a **runtime-only** dependency for development/login convenience; it is not required for normal players.

## Troubleshooting

- Overhead stats never show up:
  - Ensure **Overhead Stats** is enabled in config.
  - Ensure you are actually connected to a `donutsmp.net` server.
  - Ensure your **API Key** is set (and valid). A bad key may show `401 Unauthorized` in logs.
- Donation HUD does not update:
  - The mod only detects donations that match the exact chat pattern `paid you $...` (including suffixes `k/m/b`).

## License

All Rights Reserved (see `LICENSE.txt`).

