"""
port/26.2: generates assets/cobblemon/items/<id>.json client item definitions.

Since 1.21.4 every item needs an item definition; without one it renders as the missing (purple and black)
model. Cobblemon only ships legacy models/item/*.json files whose "overrides" select variants, so this turns
each registered item's model into a definition:

  - plain model                       -> minecraft:model
  - "custom_model_data" overrides     -> minecraft:range_dispatch on custom_model_data floats[0]
                                         (legacy semantics: the highest threshold <= value wins)
  - "cast" overrides (poke rods)      -> minecraft:condition on minecraft:fishing_rod/cast
  - Cobblemon range predicates        -> minecraft:range_dispatch on the Cobblemon property of the same id,
                                         only when --cobblemon-properties is given (they must be registered
                                         client-side first, or the definition fails to load)

Input is the list of registered item ids (item_ids.txt next to this script), because models/item also holds
sub-models that are only override targets and must not become items. Regenerate that list after upstream
merges by dumping BuiltInRegistries.ITEM for the cobblemon namespace from a dev server.

Usage (from the repo root):
    python tools/port-26.2/generate_item_definitions.py [--cobblemon-properties]
"""
import json
import os
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(REPO, "common", "src", "main", "resources", "assets", "cobblemon")
MODELS = os.path.join(ASSETS, "models", "item")
OUT = os.path.join(ASSETS, "items")
IDS = os.path.join(os.path.dirname(__file__), "item_ids.txt")

COBBLEMON_RANGE_PROPERTIES = {"cobblemon:aprijuice_quality", "cobblemon:ponigiri_overlay", "cobblemon:poke_puff_combined"}

# Definitions that cannot be derived from a legacy model and are maintained by hand (never overwritten).
# pokemon_model used the removed builtin/entity parent; it needs a minecraft:special renderer.
HAND_WRITTEN = {"pokemon_model"}


def model(ref):
    return {"type": "minecraft:model", "model": ref}


def definition_for(item_id, use_cobblemon_properties):
    path = os.path.join(MODELS, item_id + ".json")
    with open(path, encoding="utf-8") as f:
        legacy = json.load(f)
    base = model("cobblemon:item/" + item_id)
    overrides = legacy.get("overrides") or []
    if not overrides:
        return {"model": base}

    predicates = {key for o in overrides for key in o.get("predicate", {})}
    if len(predicates) != 1:
        raise ValueError(f"{item_id}: mixed override predicates {sorted(predicates)} need a hand-written definition")
    predicate = predicates.pop()

    if predicate == "cast":
        cast = [o for o in overrides if o["predicate"]["cast"] >= 1]
        if len(cast) != 1:
            raise ValueError(f"{item_id}: expected one cast override")
        return {"model": {
            "type": "minecraft:condition",
            "property": "minecraft:fishing_rod/cast",
            "on_true": model(cast[0]["model"]),
            "on_false": base,
        }}

    if predicate in ("custom_model_data", "minecraft:custom_model_data"):
        property_def = {"property": "minecraft:custom_model_data", "index": 0}
    elif predicate in COBBLEMON_RANGE_PROPERTIES:
        if not use_cobblemon_properties:
            return {"model": base}
        property_def = {"property": predicate}
    else:
        raise ValueError(f"{item_id}: unknown override predicate {predicate}")

    entries = sorted(
        ({"threshold": o["predicate"][predicate], "model": model(o["model"])} for o in overrides),
        key=lambda e: e["threshold"],
    )
    return {"model": {"type": "minecraft:range_dispatch", **property_def, "entries": entries, "fallback": base}}


def main():
    use_cobblemon_properties = "--cobblemon-properties" in sys.argv
    with open(IDS, encoding="utf-8") as f:
        ids = [line.strip() for line in f if line.strip() and not line.startswith("#")]

    os.makedirs(OUT, exist_ok=True)
    written, skipped = 0, []
    for item_id in ids:
        if not os.path.exists(os.path.join(MODELS, item_id + ".json")):
            skipped.append(item_id)
            continue
        if item_id in HAND_WRITTEN:
            continue
        target = os.path.join(OUT, item_id + ".json")
        with open(target, "w", encoding="utf-8", newline="\n") as f:
            json.dump(definition_for(item_id, use_cobblemon_properties), f, indent=2)
            f.write("\n")
        written += 1
    print(f"wrote {written} item definitions to {os.path.relpath(OUT, REPO)}")
    if skipped:
        print("no models/item file (need a hand-written definition):", ", ".join(skipped))


if __name__ == "__main__":
    main()
