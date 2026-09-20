import json, collections
p = r"D:\Minecraft\mod_projects\panlingre-1.21.1\src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json"
d = json.load(open(p, encoding="utf-8"))
els = d["elements"]
def walk(nodes, depth=0):
    for n in nodes:
        ch = n.get("children", [])
        kinds = set(type(c).__name__ for c in ch)
        print("  "*depth + f"- {n['name']}  children={len(ch)} {kinds} origin={n.get('origin')}")
        sub = [c for c in ch if isinstance(c, dict)]
        if sub: walk(sub, depth+1)
walk(d["groups"])
print()
print("first element keys:", list(els[0].keys()))
print("element names sample:")
for e in els[:5]: print("   ", e.get("name"))
