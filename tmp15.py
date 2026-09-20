import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]; byuuid={e["uuid"]:e for e in els}
groups={g["name"]:g for g in bb["groups"]}
g=groups["云雾慢旋帧_00"]
print("children:",len(g["children"]))
for u in g["children"]:
    e=byuuid[u]
    print("  ",e["name"],"from",e["from"],"to",e["to"],"rot",e.get("rotation"))
