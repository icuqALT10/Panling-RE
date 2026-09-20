from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
chars=" .:-=+*#%@"
def show(im,title,step=1,alpha=True):
    W,H=im.size
    print(f"--- {title} {W}x{H}")
    a=im.getchannel("A") if alpha else None
    for y in range(0,H,step):
        line=""
        for x in range(0,W,step):
            p=im.getpixel((x,y))
            if alpha:
                v=p[3]
            else:
                v=(p[0]+p[1]+p[2])//3
            line+=chars[min(9,v*10//256)]
        print("   |"+line+"|")
def first(fn):
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    return im.crop((0,0,W,W)) if H>W else im
for fn in ["chaos.png","hunyuan_yang.png","hunyuan_vermilion.png","hysd0.png","v3_cloud_0.png","v3_cloud_1.png"]:
    show(first(fn),fn,1 if Image.open(os.path.join(base,fn)).size[0]<=32 else 4)
