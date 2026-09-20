from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
chars=" .:-=+*#%@"
def showrgb(im,title,step=1):
    W,H=im.size
    print(f"--- {title} {W}x{H} (RGB ascii)")
    for y in range(0,H,step):
        line=""
        for x in range(0,W,step):
            p=im.getpixel((x,y)); v=(p[0]+p[1]+p[2])//3
            line+=chars[min(9,v*10//256)]
        print("   |"+line+"|")
def fr(fn,idx):
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    return im.crop((0,idx*W,W,(idx+1)*W))
showrgb(fr("v9_taiji_strike_0_stage_0.png",0),"strike0_0 f0",1)
print("   colors present:", fr("v9_taiji_strike_0_stage_0.png",0).getcolors(9999)[:5])
showrgb(fr("v9_cloud_slow_rotation_00.png",0),"cloud00 f0",4)
print("   colors:", Image.open(os.path.join(base,"v9_cloud_slow_rotation_00.png")).convert("RGBA").crop((0,0,64,64)).getcolors(9999)[:6])
# check every frame alpha-extrema of strike0_0
im=Image.open(os.path.join(base,"v9_taiji_strike_0_stage_0.png")).convert("RGBA")
W,H=im.size
for i in range(0,20):
    f=im.crop((0,i*W,W,(i+1)*W))
    print(i, f.getchannel("A").getextrema(), f.getcolors(99999)[:3])
