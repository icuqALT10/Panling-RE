import json
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
js=json.load(open(r"src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json",encoding="utf-8"))
be=bb["elements"]; je=js["elements"]
print(len(be), len(je))
diff=0
for i,(a,b) in enumerate(zip(be,je)):
    if a["name"]!=b.get("name") or a["from"]!=b["from"] or a["to"]!=b["to"]:
        diff+=1
        if diff<6: print("MISMATCH",i,a["name"],b.get("name"),a["from"],b["from"])
print("mismatches:",diff)
# rotation stats in bbmodel
import collections
ang=collections.Counter()
res=collections.Counter()
nores=0
for e in be:
    r=e.get("rotation")
    if r:
        ang[r["angle"]]+=1
        res[r.get("rescale")]+=1
print("angles:",sorted(ang.items()))
print("rescale:",dict(res))
# faces texture refs
tref=collections.Counter()
for e in be:
    for f in e["faces"].values():
        tref[f["texture"]]+=1
print("texture refs count:",len(tref), sorted(tref.items())[:10])
# bb textures
print("bb textures:",len(bb["textures"]))
for t in bb["textures"][:3]:
    print({k:t[k] for k in t if k in ("name","id","folder","namespace","relative_path","particle")})
