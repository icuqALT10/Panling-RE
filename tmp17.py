import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]; byuuid={e["uuid"]:e for e in els}
gname={g["uuid"]:g["name"] for g in bb["groups"]}
# walk outliner
tree={}
def walk(nodes,parent=None):
    out=[]
    for n in nodes:
        if isinstance(n,str):
            out.append(("el",n))
        else:
            nm=gname.get(n["uuid"],"?"+n["uuid"][:6])
            out.append(("grp",nm,walk(n["children"],nm)))
    return out
t=walk(bb["outliner"])
def show(nodes,d=0):
    for x in nodes:
        if x[0]=="el":
            pass
        else:
            cnt=count(x)
            print("  "*d+f"- {x[1]}  elements={cnt}")
            show(x[2],d+1)
def count(x):
    c=0
    for y in x[2]:
        c += 1 if y[0]=="el" else count(y)
    return c
show(t)
