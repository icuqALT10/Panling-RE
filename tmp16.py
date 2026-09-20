import json,collections
bb=json.load(open(r"C:\Users\icuqA\.dsh\attachments\v1\files\73\738cb3d12d879955e86049cbc8ffd86753390f201cc716d12dafcfb04dab91bf\混元神鼎.bbmodel",encoding="utf-8"))
els=bb["elements"]; byuuid={e["uuid"]:e for e in els}
groups={g["name"]:g for g in bb["groups"]}
def rots(name):
    g=groups[name]
    nz=[]; z=0; axes=collections.Counter()
    for u in g["children"]:
        e=byuuid[u]; r=e.get("rotation")
        if not r or all(abs(v)<1e-6 for v in r): z+=1; continue
        nz.append(tuple(round(v,4) for v in r))
    return z,nz
for nm in ["01_八面青铜鼎身","02_八卦金铭","03_三足与双足" if False else "03_三足与双耳","04_像素太极_贴图旋转6_4秒","05_宽焰双环与鼎底火","06_星辰闪烁","08_浮空八卦_缩至上版75百分比","内膛青铜底","混沌内间歇雷光","混沌_深渊底云","鼎底持续燃烧","赤焰第2重","赤焰第4重","赤阳法阵_贴图直接逆时针急转","星群_0"]:
    if nm not in groups: print("missing",nm); continue
    z,nz=rots(nm)
    print(f"{nm}: zero={z} nonzero={len(nz)} distinct={len(set(nz))}")
    for r in sorted(set(nz))[:12]: print("      ",r)
