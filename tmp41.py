from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
rows=[]
for k in range(64):
    fn=f"v9_cloud_slow_rotation_{k:02d}.png"
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size; n=H//W
    idx=[]
    for i in range(n):
        a=im.crop((0,i*W,W,(i+1)*W)).getchannel("A")
        h=a.histogram(); op=sum(h[1:])/(W*W)
        if op>0.01: idx.append((i,round(op,2)))
    rows.append((k,idx))
    print(f"{fn}: nonblank frames {len(idx)} -> {idx[:6]}")
