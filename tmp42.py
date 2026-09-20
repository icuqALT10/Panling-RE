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
for g in ["赤焰第2重","赤焰第4重","鼎底持续燃烧","混沌_深渊底云"]:
    grp=getgrp(bb["outliner"],g)
    print("###",g)
    for u in collect(grp["children"],[]):
        e=byuuid[u]
        c=[round((a+b)/2,3) for a,b in zip(e["from"],e["to"])]
        print(f'   {e["name"]:<16} from={e["from"]} to={e["to"]} rot={e.get("rotation")} origin={e.get("origin")} center={c}')
