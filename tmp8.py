import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
js=json.load(open(r"src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json",encoding="utf-8"))
be=bb["elements"]; je=js["elements"]
print("names unique bb:", len(set(e["name"] for e in be))==len(be), "json:", len(set(e["name"] for e in je))==len(je))
bn=[e["name"] for e in be]; jn=[e["name"] for e in je]
print("same name multiset:", collections.Counter(bn)==collections.Counter(jn))
bmap={e["name"]:e for e in be}; jmap={e["name"]:e for e in je}
bad=0
for n in jn:
    a=bmap[n]; b=jmap[n]
    if a["from"]!=b["from"] or a["to"]!=b["to"]:
        bad+=1
        if bad<=8: print("coords differ:",n,a["from"],b["from"])
print("coord diffs:",bad)
# rotation forms in bbmodel
forms=collections.Counter(type(e.get("rotation")).__name__ for e in be)
print("bb rotation types:",forms)
r=[e["rotation"] for e in be if isinstance(e.get("rotation"),dict)]
print("rotation angle range:", min(x["angle"] for x in r), max(x["angle"] for x in r))
print("angle values:", sorted(set(x["angle"] for x in r)))
print("rescale:", collections.Counter(x.get("rescale") for x in r))
jr=[e.get("rotation") for e in je if e.get("rotation")]
print("json angle values:", sorted(set(x["angle"] for x in jr)))
print("json rotation keys:", set(k for x in jr for k in x))
