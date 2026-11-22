# Changelog

## 1.4.3

### Auction
- Cleaner Auction settings: only Duration, Extra time per top bid, Stream delay, Sound volume, Particle style, Particle color and Size are left.
- New **Sound volume** slider for all auction sounds (tick, countdown, bid bell, winner jingle). 0 mutes them.
- Countdown sounds, bid bell, winner jingle, hologram, bobbing, bid effect and particles are now always on.
- Removed the Minimum bid setting. Set it with `/bid start` instead; without one it stays at 1k.
- Fixed: the particles were far below the item and sank downwards. They now sit right under the item, so the item rests on them.

## 1.4.2

### Auction
- Only a bid above the current top bid adds time, rings the bell and spins the item, so $1/$2 spam does nothing.
- Countdown sounds: a tick every second, then a rising "dim" for the last 10 seconds (tick volume adjustable).
- Bid bell on every new top bid and a level-up jingle when the auction has a winner.
- The item spins once on a new top bid.
- Particle effects under the item: Ring, Helix, Atom, Sparkles, Fountain and Vortex, each with selectable colors.
- Stream delay (0 to 60 s): when the timer hits 0 it restarts at the delay so late bids still count. Use `/bid delay <seconds>`.
- The timer shakes harder and harder during the last 10 seconds.
- Shorter winner buttons in chat: **[Teleport]** and **[Orders]**.
- Removed the "Remember to refund" message.

### HUDs (all movable in the OneConfig HUD editor)
- New Auction Panel HUD: item name, a smooth countdown ring and a glowing outline during the countdown.
- New Earnings HUD and Earnings Stats HUD with a graph and a reset button.
- New Goal HUD (progress towards a goal amount) and Info HUD (custom text).
- Each HUD's settings live in its own HUD settings.

### Pay Confirm
- The popup now reads "Send $X to Name?" with the amount in green and the player in aqua.
- **Yes** is on the right, **No** on the left. ESC always cancels.

### Other
- All payments are read by one shared listener instead of each feature parsing chat itself.
- Cleaned up the config: Pay Confirm, Overhead Stats, Auction, Developer.
- Old leftover config entries are removed automatically on start.
