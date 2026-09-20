import json, collections, sys
p = r"D:\Minecraft\mod_projects\panlingre-1.21.1\src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json"
d = json.load(open(p, encoding="utf-8"))
print("TOP KEYS:", list(d.keys()))
print("format_version", d.get("format_version"))
print("texture_size", d.get("texture_size"))
print("elements:", len(d.get("elements", [])))
print("textures:", len(d.get("textures", {})))
print("groups:", len(d.get("groups", [])) if "groups" in d else None)
import re
tex = d["textures"]
bad = [ (k,v) for k,v in tex.items() if ":" not in v ]
print("textures without namespace:", len(bad))
for k,v in bad[:80]: print("   ", k, "->", v)
