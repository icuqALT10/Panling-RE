import json
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
print("outliner len",len(bb["outliner"]))
for n in bb["outliner"]:
    if isinstance(n,dict):
        print("DICT group:",n.get("name"),"children:",len(n.get("children",[])),"keys:",list(n.keys()))
    else:
        print("STR:",n)
print()
print("groups len",len(bb["groups"]))
def walk(ns,d=0):
    for n in ns:
        if isinstance(n,dict):
            ch=n.get("children",[])
            print("  "*d+"-",n.get("name"),"n=",len(ch), "keys=",list(n.keys()))
            walk([c for c in ch if isinstance(c,dict)],d+1)
        else:
            pass
walk(bb["groups"])
