import json
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=[e for e in bb["elements"] if e["name"].startswith("云雾慢旋帧00")]
print("found",len(els))
for e in els[:3]:
    print(e["name"],"from",e["from"],"to",e["to"],"rot",e.get("rotation"),"autouv",e.get("autouv"),"box_uv",e.get("box_uv"))
    for f,v in e["faces"].items(): print("   ",f,v)
    print()
