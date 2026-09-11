# port/26.2 dev test tooling

Helpers used while porting, kept so a later session does not have to rebuild them. Paths inside the
scripts point at this machine's checkout and `C:\jtmp` - adjust when used elsewhere.

## Dev client straight into a world

```bash
rm -rf fabric/runClient/saves/devworld
cp -r fabric/runServer/world fabric/runClient/saves/devworld && rm -f fabric/runClient/saves/devworld/session.lock
mkdir -p fabric/runClient/saves/devworld/datapacks
cp -r tools/port-26.2/devtest/smoke-test-datapack fabric/runClient/saves/devworld/datapacks/cobbletest
python tools/port-26.2/devtest/enable_cheats.py fabric/runClient/saves/devworld/level.dat
./gradlew :fabric:runClient --args="--quickPlaySingleplayer devworld"
```

`--args` is appended to loom's program arguments (do not repeat `--username`/`--uuid`).

## smoke-test-datapack

On join it gives balls, potions and a Charmander, spawns a Pidgey next to the player, tries a wild battle
through `runmolang ... attempt_wild_battle` and after a minute prints the number of Pokémon entities
(`CT-*` markers in the log). Cobblemon commands sit in macro functions: plain function lines are parsed
when the datapack loads, before Cobblemon has its species, and fail with "include a Pokémon name".
Command output from functions is suppressed, so check the log/markers rather than chat feedback.

## Mixin checks

This mixin config silently skips injectors that find no target, so "no crash" does not mean "applied".

- `mixin_sweep.py` - static check of registered mixins against the unpacked 26.2 jar (`C:\jtmp\mc262`):
  targets, `@Shadow`/`@Accessor`/`@Invoker` members and injector method names. Parser has false positives
  (`<init>`, nested classes); read the hits.
- `mixin_applied.py` - run the client with `JAVA_TOOL_OPTIONS=-Dmixin.debug.export=true`, then this lists
  every handler that was merged into a loaded target class but is never called (= its injector did not apply).

## Line endings

About 12k files are CRLF in the working tree while their blobs are LF (stat-cached, invisible until touched),
and some blobs are mixed (e.g. the access widener). Compare `git diff --stat` with
`git diff --ignore-cr-at-eol --stat` after every scripted edit. `restore_mixed_eol.py <file>` puts the blob's
per-line endings back on a file an editor normalised.
