"""Static sweep: for every registered mixin, check that @Mixin targets, @Shadow members,
@Accessor/@Invoker names and injector method names exist in the 26.2 bytecode (target + supers)."""
import json, os, re, subprocess, sys

REPO = r"C:\Users\vault-boy\Documents\Programming\Minecraft Mod Portierung 26.2\cobblemon"
MC = r"C:\jtmp\mc262"
JAVAP = r"C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\javap.exe"

configs = [
    (os.path.join(REPO, r"common\src\main\resources\mixins.cobblemon-common.json"), os.path.join(REPO, r"common\src\main\java")),
    (os.path.join(REPO, r"fabric\src\main\resources\mixins.cobblemon-fabric.json"), os.path.join(REPO, r"fabric\src\main\java")),
]

_cache = {}
def members(cls):
    """Return (fields, methods, super, interfaces) for a binary class name, or None."""
    if cls in _cache:
        return _cache[cls]
    r = subprocess.run([JAVAP, "-p", "-classpath", MC, cls], capture_output=True, text=True)
    if r.returncode != 0 or "Error:" in r.stdout:
        _cache[cls] = None
        return None
    fields, methods, sup, ifaces = set(), set(), None, []
    for line in r.stdout.splitlines():
        m = re.match(r".*\b(class|interface|enum|record)\s+([\w.$]+)(?:<.*?>)?(?:\s+extends\s+([\w.$]+))?(?:<.*?>)?(?:\s+implements\s+(.*))?\s*\{", line)
        if m and sup is None and not line.startswith("  "):
            sup = m.group(3)
            if m.group(4):
                ifaces = [re.sub(r"<.*", "", x.strip()) for x in m.group(4).split(",")]
            continue
        line = line.strip()
        mm = re.match(r"(?:[\w.$<>\[\], ?]+\s)?([\w$<>]+)\((.*)\)(?:\s+throws.*)?;$", line)
        if mm:
            name = mm.group(1)
            if name == cls or name.endswith("." + cls.split(".")[-1]) or "." in name:
                name = "<init>"
            methods.add(name)
            continue
        fm = re.match(r".*\s([\w$]+);$", line)
        if fm:
            fields.add(fm.group(1))
    res = (fields, methods, sup, ifaces)
    _cache[cls] = res
    return res

def hierarchy(cls):
    seen, stack, out = set(), [cls], []
    while stack:
        c = stack.pop()
        if not c or c in seen or c.startswith("java."):
            continue
        seen.add(c)
        m = members(c)
        if m is None:
            continue
        out.append((c, m))
        stack.append(m[2])
        stack.extend(m[3])
    return out

def has(cls, name, kind):
    for c, (f, m, _, _) in hierarchy(cls):
        if kind == "field" and name in f:
            return True
        if kind == "method" and name in m:
            return True
    return False

problems = 0
for cfg, src in configs:
    data = json.load(open(cfg, encoding="utf-8"))
    pkg = data["package"]
    for section in ("mixins", "client", "server"):
        for name in data.get(section, []):
            path = os.path.join(src, *(pkg + "." + name).split(".")) + ".java"
            if not os.path.exists(path):
                print(f"[MISSING SOURCE] {name}")
                problems += 1
                continue
            text = open(path, encoding="utf-8").read()
            imports = dict((i.split(".")[-1], i) for i in re.findall(r"^import\s+([\w.]+);", text, re.M))
            mt = re.search(r"@Mixin\s*\((.*?)\)\s*(?:public|abstract|final|class|interface|@)", text, re.S)
            if not mt:
                print(f"[NO @Mixin] {name}")
                problems += 1
                continue
            targets = []
            for simple in re.findall(r"([\w.]+)\.class", mt.group(1)):
                parts = simple.split(".")
                base = imports.get(parts[0], parts[0])
                targets.append(base + ("$" + "$".join(parts[1:]) if len(parts) > 1 else ""))
            targets += [t.replace("/", ".") for t in re.findall(r"targets\s*=\s*\"([^\"]+)\"", mt.group(1))]
            if not targets:
                print(f"[?? no targets parsed] {name}: {mt.group(1)[:80]}")
                continue
            for t in targets:
                if members(t) is None:
                    # try nested class form a.b.C.D -> a.b.C$D
                    alt = re.sub(r"\.([A-Z]\w*)$", r"$\1", t)
                    if members(alt) is None:
                        print(f"[TARGET NOT FOUND] {name} -> {t}")
                        problems += 1
                        continue
                    targets[targets.index(t)] = alt
            issues = []
            # @Shadow members (annotation block followed by declaration)
            for m in re.finditer(r"@Shadow(?:\s*\([^)]*\))?\s*((?:@\w+(?:\([^)]*\))?\s*)*)((?:public|private|protected|static|final|abstract|\s)*)[\w.<>\[\], ?]+\s+([\w$]+)\s*(\(|;|=)", text):
                member, kind = m.group(3), ("method" if m.group(4) == "(" else "field")
                if not any(has(t, member, kind) for t in targets):
                    issues.append(f"@Shadow {kind} {member}")
            for ann in ("Accessor", "Invoker"):
                for m in re.finditer(r"@" + ann + r"\s*\(\s*(?:value\s*=\s*)?\"([^\"]+)\"", text):
                    member = m.group(1)
                    kind = "field" if ann == "Accessor" else "method"
                    if not any(has(t, member, kind) for t in targets):
                        issues.append(f"@{ann} {member}")
            # injector method names
            for m in re.finditer(r"method\s*=\s*(\{[^}]*\}|\"[^\"]*\")", text):
                for raw in re.findall(r"\"([^\"]+)\"", m.group(1)):
                    mname = raw.split("(")[0]
                    if "*" in mname or not mname:
                        continue
                    mname = mname.split(";")[-1] if mname.startswith("L") and ";" in mname else mname
                    if not any(has(t, mname, "method") for t in targets):
                        issues.append(f"method {raw}")
            if issues:
                problems += 1
                print(f"[{section}] {name} -> {targets}")
                for i in issues:
                    print(f"     - {i}")
print(f"done, {problems} mixins with findings")
