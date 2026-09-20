import json
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
sel=[e for e in bb["elements"] if any(f["texture"]==25 for f in e["faces"].values())]
print("elements using tex25:",len(sel))
for e in sel[:3]:
    print(repr(e["name"]),"from",e["from"],"to",e["to"],"rot",e.get("rotation"))
    for f,v in e["faces"].items(): print("   ",f,v)
    print()
# also a body element using tex 22
sel2=[e for e in bb["elements"] if any(f["texture"]==22 for f in e["faces"].values())]
print("elements using tex22:",len(sel2))
for e in sel2[:2]:
    print(repr(e["name"]),"from",e["from"],"to",e["to"],"rot",e.get("rotation"))
    for f,v in e["faces"].items(): print("   ",f,v)
