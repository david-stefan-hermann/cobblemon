import gzip, sys

# Flips the allowCommands byte in a level.dat so the dev world has cheats on.
path = sys.argv[1]
data = bytearray(gzip.open(path).read())
key = b"allowCommands"
i = data.find(key)
if i < 0:
    sys.exit("allowCommands not found")
data[i + len(key)] = 1
with gzip.open(path, "wb") as f:
    f.write(bytes(data))
print("allowCommands=1")
