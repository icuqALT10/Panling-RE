import json,os
root=r"src\main\resources\assets\panlingre\models\item"
d=json.load(open(os.path.join(root,"suo_hun_lu.json"),encoding="utf-8"))
for k,v in d.items():
    if k=="elements":
        print("elements:",len(v))
        print("element[0] keys:",list(v[0].keys()))
        print(json.dumps(v[0],ensure_ascii=False))
    else:
        print(k,"=",json.dumps(v,ensure_ascii=False)[:1200])
