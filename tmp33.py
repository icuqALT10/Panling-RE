from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
for fn in ["v9_taiji_strike_0_stage_0.png","v9_taiji_strike_0_stage_1.png","v9_taiji_strike_1_stage_0.png","v9_taiji_strike_2_stage_2.png"]:
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size; n=H//W
    nb=[]
    for i in range(n):
        f=im.crop((0,i*W,W,(i+1)*W))
        a=f.getchannel("A"); op=sum(a.histogram()[1:])/(W*W)
        if op>0.01: nb.append((i,round(op,2)))
    print(fn,"nonblank frames:",len(nb), nb[:25])
