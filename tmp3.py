import json,glob,os,re
root=r"src\main\resources\assets\panlingre\models\item"
for f in ["qi_sha_din.json","huang_tong_lu.json","chi_tong_lu.json","jing_tie_lu.json","suo_hun_lu.json"]:
    p=os.path.join(root,f)
    if not os.path.exists(p): print(f,"MISSING"); continue
    d=json.load(open(p,encoding="utf-8"))
    els=d.get("elements",[])
    ang=set()
    axt=set()
    for e in els:
        r=e.get("rotation")
        if r: ang.add(r.get("angle")); axt.add(r.get("axis"))
    inn=[v for v in d.get("textures",{}).values() if ":" not in v]
    print(f, "els=",len(els), "keys=",list(d.keys()), "angles=",sorted(x for x in ang if x is not None), "innertex=",len(inn))
