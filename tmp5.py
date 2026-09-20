import json,os
root=r"src\main\resources\assets\panlingre\models\item"
d=json.load(open(os.path.join(root,"qi_sha_din.json"),encoding="utf-8"))
print("GROUPS:", json.dumps(d["groups"],ensure_ascii=False)[:2000])
print()
print("DISPLAY:", json.dumps(d["display"],ensure_ascii=False,indent=1))
