import json,collections,math
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]; byuuid={e["uuid"]:e for e in els}
gname={g["uuid"]:g["name"] for g in bb["groups"]}
def collect(nodes,acc):
    for n in nodes:
        if isinstance(n,str): acc.append(n)
        else: collect(n["children"],acc)
    return acc
def walkgroups(nodes,d=0,parent=None,out=None):
    if out is None: out=[]
    for n in nodes:
        if isinstance(n,dict):
            nm=gname.get(n["uuid"],"?")
            out.append((d,nm,n)); walkgroups(n["children"],d+1,nm,out)
    return out
gs=walkgroups(bb["outliner"])
def elements_of(g):
    return collect(g["children"],[])
print(f'{"group":<35} {"elems":>5}  rotations')
for d,nm,g in gs:
    if d==0 and nm not in ("01_八面青铜鼎身"): continue
    r=collections.Counter()
    for u in collect(g["children"],[]):
        rot=byuuid[u].get("rotation")
        if rot and any(abs(v)>1e-9 for v in rot): r[tuple(round(v,3) for v in rot)]+=1
    print(f'{nm:<35} {len(collect(g["children"],[])):>5}  distinct_nonzero={len(r)}')
    for k,v in list(r.items())[:20]: print("        ",k,"x",v)
