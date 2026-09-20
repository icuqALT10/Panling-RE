import json,os,glob
p=r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel"
d=json.load(open(p,encoding="utf-8"))
print("TOP KEYS:",list(d.keys()))
print("meta:",json.dumps(d.get("meta"),ensure_ascii=False)[:800])
print("elements:",len(d.get("elements",[])))
print("outliner top:",len(d.get("outliner",[])))
print("animations:",json.dumps(d.get("animations"),ensure_ascii=False)[:500] if "animations" in d else None)
print("textures:",json.dumps(d.get("textures"),ensure_ascii=False)[:1500])
print("resolution:",d.get("resolution"))
e=d["elements"][0]
print("element0:",json.dumps(e,ensure_ascii=False)[:900])
