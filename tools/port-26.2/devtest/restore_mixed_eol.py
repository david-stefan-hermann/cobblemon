"""Re-apply the blob's per-line CRLF endings to a working file whose line endings an editor normalized.
Lines whose content exists in HEAD with a CR get it back; new lines stay LF."""
import subprocess, sys, os

os.chdir(r"C:\Users\vault-boy\Documents\Programming\Minecraft Mod Portierung 26.2\cobblemon")
for path in sys.argv[1:]:
    blob = subprocess.run(["git", "show", "HEAD:" + path], capture_output=True).stdout
    had_cr = {}
    for line in blob.split(b"\n"):
        if line.endswith(b"\r"):
            had_cr[line[:-1]] = True
        else:
            had_cr.setdefault(line, False)
    wt = open(path, "rb").read()
    lines = wt.split(b"\n")
    out = []
    for i, line in enumerate(lines):
        bare = line[:-1] if line.endswith(b"\r") else line
        last = i == len(lines) - 1
        if not last and had_cr.get(bare, False):
            out.append(bare + b"\r")
        else:
            out.append(bare)
    open(path, "wb").write(b"\n".join(out))
    print("restored", path)
