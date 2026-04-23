# Changelog
## [1.8.0 (MONTH Xth, 2026)](#1-8-0)

### Additions
- Added a new `cobblemon:structure`, which is a feature without any restrictions allowing the Builders to place certain structures in much more specific manner.
- Added Configs for Infinite Use TMs and Unlocking all TMs
- Added Type Gem blocks
- Added Type Gem Clusters
- Added TMs
- Added TM Machine
- Added Deepslate Core Block
- Added /givetm Command
- Added Move Dex to Pokedex
- Added Disk Rack for storing disks and being an audio sequencer for Noteblocks

#### Added sounds to the following moves
- Absorb
- Aurora Beam
- Double Team
- Fire Punch
- Fire Spin
- Flame Charge
- Flame Wheel
- Giga Drain
- Ice Punch
- Leech Life
- Magical Leaf
- Mega Drain
- Minimize
- Poison Gas
- Rock Throw
- Scary Face
- Seed bomb
- Smokescreen
- String Shot
- Tail Whip

### Changes
- Data Monitor can accept Disks (TMs, Music Disks, Upgrade, Dubious Disk, etc) to change look
- Sharp Beak can now be crafted
- Ability Capsules can now be brewed
- The following items can now be obtained by brushing the following Pokemon:
  - Pink Wool - Wigglytuff
  - White Wool - Swablu, Altaria
  - Shed Shell - Ekans, Arbok, Dratini, Dragonair, Dunsparce, Treecko, Grovyle, Sceptile, Seviper, Scraggy, Scrafty, Helioptile, Heliolisk, Salandit, Salazzle, Sobble, Drizzile, Inteleon, Silicoblra, Sandaconda, Toxel, Toxtricity, Orthworm, Dudunsparce
  - Phantom Membrane - Dreepy, Drakloak, Dragapult
- The following items can now be obtained by evolving the following Pokemon into any of their evolutions:
  - Turtle Scute - Squirtle, Wartortle, Turtwig, Grotle, Tirtouga, Chewtle
  - Shed Shell - Metapod, Kakuna, Ekans, Paras, Venonat, Krabby, Scyther, Kabuto, Dratini, Dragonair, Ledyba, Spinarak, Yanma, Pineco, Dunsparce, GLigar, Pupitar, Treecko, Grovyle, Silcoon, Cascoon, Surskit, Nincada, Trapinch, Vibrava, Corphish, Anorith, Bagon, Shelgon, Kricketot, Burmy, Combee, Skorupi, Swadloon, Whirlipede, Dwebble, Scraggy, Karrablast, Joltik, Larvesta, Spewpa, Clauncher, Helioptile, Charjabug, Crabrawler, Cutiefly, Dewpider, Salandit, Wimpod, Poipole, Sobble, Drizzile, Dottler, Silicobra, Toxel, Sizzlipede, Snom, Tarountula, Nymble, Rellor

### Fixes
- Fixed rare Pokémon being far too likely to spawn multiple times in a single spawn cycle. The dev told me that the exact bug is too complicated to explain.
- Resolved the niche issue where Structures would spawn in an incorrect location, creating very funky world gen.

### Developer
- Tweaked the spawning selector interface to take a bucket function rather than a bucket directly.
- Added a new `cobblemon:height_range` processor type to allow developers to restrict structure spawning ranges.

### Molang & Datapacks
- Flamethrower and Seismic Toss had their action effect json cleaned up

### Particles

### Localization
- Updated translations for: