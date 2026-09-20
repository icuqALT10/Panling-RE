from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
def frames(fn, w):
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    n=H//W
    return [im.crop((0,i*W,W,(i+1)*W)) for i in range(n)], W, n
def diff(a,b):
    pa=a.load(); pb=b.load(); W,H=a.size
    c=0; tot=W*H
    for y in range(H):
        for x in range(W):
            if pa[x,y]!=pb[x,y]: c+=1
    return c/tot
for fn in ["v9_cloud_slow_rotation_00.png","v3_taiji_rotate_32px.png","v7_wide_flame_1.png","v7_fast_rune_kan.png","v3_star_0.png","v9_array_counterclockwise_slow.png","v9_taiji_strike_0_stage_0.png"]:
    fr,W,n=frames(fn,None)
    print(f"{fn}: {W}px, {n} frames; blankframes={sum(1 for f in fr if f.getextrema()==((0,0),(0,0),(0,0),(0,0)))}")
    print("   diff f0-f1: %.3f   f0-f2: %.3f   f0-last: %.3f" % (diff(fr[0],fr[1]), diff(fr[0],fr[2]), diff(fr[0],fr[-1])))
