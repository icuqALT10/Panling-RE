import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]
def findoutliner(nodes,path=""):
    res=[]
    for n in nodes:
        if isinstance(n,str):
            res.append((path,n))
        else:
            res+=findoutliner(n["children"],path+"/"+n.get("name","?"))
    return res
ols=findoutliner(bb["outliner"])
byuuid={e["uuid"]:e for e in els}
def grp(prefix):
    return [(p,byuuid[u]) for p,u in ols if p.endswith(prefix)]
for pr in ["/06_星辰闪烁","/05_宽焰双环与鼎底火/赤焰第2重"]:
    print("###",pr)
    for p,e in grp(pr)[:8]:
        print("  ",p,"|",e["name"],e["from"],e["to"],e.get("rotation"))
print()
print("### 云雾慢旋帧_00 group")
sub=[n for n in bb["outliner"] if isinstance(n,dict)]
def find(nodes,name):
    for n in nodes:
        if isinstance(n,dict):
            if n.get("name")==name: return n
            r=find(n["children"],name)
            if r is not None: return r
    return None
g=find(bb["outliner"],"07_混沌云雾与霹雳")
f=find(g["children"],"云雾慢旋帧_00")
print("children:",len(f["children"]), f["children"][:3])
for u in f["children"]:
    e=byuuid[u]
    print("  ",e["name"],e["from"],e["to"],e.get("rotation"))
