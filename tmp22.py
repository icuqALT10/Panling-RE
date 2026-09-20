import json,collections
p=r"src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json"
js=json.load(open(p,encoding="utf-8"))
tex=js["textures"]
# group elements by name prefix
pref=collections.Counter()
used=collections.Counter()
for e in js["elements"]:
    for f in e["faces"].values():
        used[f["texture"]]+=1
print("texture usage (id->ref):")
for k in sorted(tex, key=lambda x:int(x) if x.isdigit() else 999):
    print(f"  {k:>4} {tex[k]:<60} faces={used.get('#'+k,0)}")
