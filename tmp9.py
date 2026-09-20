import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
js=json.load(open(r"src\main\resources\assets\panlingre\models\item\hun_yuan_shen_din.json",encoding="utf-8"))
be=bb["elements"]; je=js["elements"]
bmap={}; 
for e in be: bmap.setdefault(e["name"],[]).append(e)
jmap={}
for e in je: jmap.setdefault(e["name"],[]).append(e)
rotlists=[x for x in (e.get("rotation") for e in be) if isinstance(x,list)]
print("total rot lists:",len(rotlists))
c=collections.Counter(tuple(x) for x in rotlists)
print("distinct rotations:",len(c))
for r,n in c.most_common(12): print("   ",r,n)
print()
# sample a few elements with rotation
cnt=0
for e in be:
    if isinstance(e.get("rotation"),list) and any(abs(v)>0.001 for v in e["rotation"]):
        j=jmap[e["name"]][0]
        print("BB :",e["name"], "rot",e["rotation"],"origin",e.get("origin"))
        print("JS :",j.get("name"),"rot",j.get("rotation"))
        cnt+=1
        if cnt>=6: break
