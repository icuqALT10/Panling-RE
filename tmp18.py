import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]; byuuid={e["uuid"]:e for e in els}
gname={g["uuid"]:g["name"] for g in bb["groups"]}
def collect(nodes,acc):
    for n in nodes:
        if isinstance(n,str): acc.append(n)
        else: collect(n["children"],acc)
    return acc
def getgrp(nodes,name):
    for n in nodes:
        if isinstance(n,dict) and gname.get(n["uuid"])==name: return n
        if isinstance(n,dict):
            r=getgrp(n["children"],name)
            if r: return r
    return None
g=getgrp(bb["outliner"],"云雾慢旋帧_00")
ch=collect(g["children"],[])
print("frame00 elements:",len(ch))
for u in ch:
    e=byuuid[u]
    print(f'   {e["name"]:<20} from={e["from"]} to={e["to"]} rot={e.get("rotation")} origin={e.get("origin")}')
print()
g2=getgrp(bb["outliner"],"01_八面青铜鼎身")
ch2=collect(g2["children"],[])
print("body elements:",len(ch2))
c=collections.Counter(tuple(x.get("rotation") or (0,0,0)) for x in (byuuid[u] for u in ch2))
for r,n in c.most_common(15): print("   ",r,n)
