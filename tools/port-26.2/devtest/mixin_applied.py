"""After a run with -Dmixin.debug.export=true: for every injector handler of every registered Cobblemon
mixin whose target class was loaded (exported), check that the merged handler is actually called from
the target class. A handler that is merged but never invoked means its injector found no target."""
import json, os, re, subprocess

REPO = r"C:\Users\vault-boy\Documents\Programming\Minecraft Mod Portierung 26.2\cobblemon"
OUT = os.path.join(REPO, r"fabric\runClient\.mixin.out\class")
JAVAP = r"C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javap.exe"

configs = [
    (os.path.join(REPO, r"common\src\main\resources\mixins.cobblemon-common.json"), os.path.join(REPO, r"common\src\main\java")),
    (os.path.join(REPO, r"fabric\src\main\resources\mixins.cobblemon-fabric.json"), os.path.join(REPO, r"fabric\src\main\java")),
]
INJECTORS = r"Inject|ModifyArg|ModifyArgs|Redirect|ModifyVariable|ModifyConstant|WrapOperation|WrapMethod|ModifyReturnValue|ModifyExpressionValue|WrapWithCondition|ModifyReceiver"

_dump = {}
def dump(cls_path):
    if cls_path not in _dump:
        r = subprocess.run([JAVAP, "-p", "-c", cls_path], capture_output=True, text=True)
        _dump[cls_path] = r.stdout
    return _dump[cls_path]

not_loaded, ok, broken = [], 0, []
for cfg, src in configs:
    data = json.load(open(cfg, encoding="utf-8"))
    pkg = data["package"]
    for section in ("mixins", "client", "server"):
        for name in data.get(section, []):
            path = os.path.join(src, *(pkg + "." + name).split(".")) + ".java"
            text = open(path, encoding="utf-8").read()
            imports = dict((i.split(".")[-1], i) for i in re.findall(r"^import\s+([\w.]+);", text, re.M))
            mt = re.search(r"@Mixin\s*\((.*?)\)\s*(?:public|abstract|final|class|interface|@)", text, re.S)
            targets = []
            for simple in re.findall(r"([\w.]+)\.class", mt.group(1)):
                parts = simple.split(".")
                base = imports.get(parts[0], parts[0])
                targets.append(base + ("$" + "$".join(parts[1:]) if len(parts) > 1 else ""))
            # handlers: injector annotation (balanced parens) followed by the method declaration
            handlers = []
            for m in re.finditer(r"@(" + INJECTORS + r")\b", text):
                i = m.end()
                while i < len(text) and text[i] in " \t\r\n":
                    i += 1
                if i < len(text) and text[i] == "(":
                    depth = 0
                    while i < len(text):
                        if text[i] == "(":
                            depth += 1
                        elif text[i] == ")":
                            depth -= 1
                            if depth == 0:
                                break
                        i += 1
                    i += 1
                decl = re.search(r"([\w$]+)\s*\(", text[i:i + 600])
                prefix = text[:m.start()]
                if prefix.rstrip().endswith("//") or re.search(r"//[^\n]*$", prefix):
                    continue
                if decl:
                    handlers.append((m.group(1), decl.group(1)))
            if not handlers:
                continue
            for t in targets:
                cp = os.path.join(OUT, *t.split(".")) + ".class"
                if not os.path.exists(cp):
                    not_loaded.append(f"{name} -> {t}")
                    continue
                code = dump(cp)
                for kind, h in handlers:
                    calls = re.findall(r"(?:invoke\w+|// (?:Method|InvokeDynamic)[^\n]*)[^\n]*" + re.escape(h) + r"[^\n]*", code)
                    calls = [c for c in calls if "Method" in c or "InvokeDynamic" in c]
                    if calls:
                        ok += 1
                    else:
                        broken.append(f"{name} @{kind} {h} (target {t})")
print(f"applied handlers: {ok}")
print(f"NOT applied ({len(broken)}):")
for b in broken:
    print("  - " + b)
print(f"targets not loaded in this run ({len(not_loaded)}):")
for n in not_loaded:
    print("  . " + n)
