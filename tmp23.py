import json
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
for t in bb["textures"]:
    if t["id"] in ("25","19","22","0","1","20") or "cloud_slow_rotation_00" in t["name"]:
        print({k:t.get(k) for k in ("id","name","folder","namespace","width","height","uv_width","uv_height","particle","frame_time","fps")})
print()
els=[e for e in bb["elements"] if e["name"].startswith("云雾慢旋帧00")]
for e in els[:4]:
    print(e["name"])
    print("  from",e["from"],"to",e["to"],"rot",e.get("rotation"))
    for f,v in e["faces"].items():
        print("   ",f,v)
