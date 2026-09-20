from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
def ascii_img(im, step=2, title=""):
    W,H=im.size
    print(f"--- {title} {W}x{H}")
    a=im.getchannel("A")
    chars=" .:-=+*#%@"
    for y in range(0,H,step):
        line=""
        for x in range(0,W,step):
            v=a.getpixel((x,y))
            line+=chars[min(9,v*10//256)]
        print("   |"+line+"|")
def f(im,i):
    W,H=im.size; return im.crop((0,i*W,W,(i+1)*W))
im=Image.open(os.path.join(base,"v9_cloud_slow_rotation_00.png")).convert("RGBA")
for i in [0,1,2]:
    ascii_img(f(im,i),2,f"cloud_00 frame{i}")
im1=Image.open(os.path.join(base,"v9_cloud_slow_rotation_01.png")).convert("RGBA")
ascii_img(f(im1,0),2,"cloud_01 frame0")
