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
- Added scaling to "baby" pokemon (smaller at level 1 -> normal sized at level 10)
- Added conditional seats for riding. Seats can be gated by molang such as "is_alpha".
- Added Alpha Pokemon
    - Added alpha scaling (alphas are larger than thier normal counterparts)
    - Alphas spawn with 2 random TM moves from their learnset
    - Added alpha level matching and stat boost
    - Added pitch-down and reverb to alpha cries
    - Added alpha pokemon bait effect
    - Added alpha eye rendering including: eye overlay, bloom, eye trail, and particles.


### Changes
- Data Monitor can accept Disks (TMs, Music Disks, Upgrade, Dubious Disk, etc) to change look
- Rider positioning is now client-driven (Remove poseOffsets and offset per seat/pose) in favor of sending real time passenger position.
- Cleaned up seat data. Removed all now unused fields from seats in all species files. Also made locator seat names explicit in species file (however, this doesn't break legacy format)
- Marks can now provide aspects.

### Fixes
- Fixed rare Pokémon being far too likely to spawn multiple times in a single spawn cycle. The dev told me that the exact bug is too complicated to explain.

### Developer
- Tweaked the spawning selector interface to take a bucket function rather than a bucket directly.

### Molang & Datapacks
- Added q.world.spawn_loot_table_items(loot_table_id, x, y, z) which spawns the items from the loot table at the position. (Use type: "minecraft:chest" for the loot table.)
- Added q.pokemon.entity to access the entity of pokemon.

### Particles

### Localization
- Updated translations for: