import json,os
root=r"src\main\resources\assets\panlingre\models\item"
for f in ["chi_tong_lu.json","huang_tong_lu.json","jing_tie_lu.json","suo_hun_lu.json","qi_sha_din.json"]:
    d=json.load(open(os.path.join(root,f),encoding="utf-8"))
    print("==",f,"texture_size",d.get("texture_size"),"gui_light",d.get("gui_light"))
    print("   display:",json.dumps(d.get("display"),ensure_ascii=False,separators=(",",":")))
