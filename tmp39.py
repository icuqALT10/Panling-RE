from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
chars=" .:-=+*#%@"
def show(im,title,step=1):
    W,H=im.size
    print(f"--- {title} {W}x{H}")
    a=im.getchannel("A")
    for y in range(0,H,step):
        line=""
        for x in range(0,W,step):
            p=im.getpixel((x,y)); v=p[3]
            line+=chars[min(9,v*10//256)]
        print("   |"+line+"|")
def first(fn,idx=0):
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    return im.crop((0,idx*W,W,(idx+1)*W)) if H>W else im
show(first("v3_taiji_rotate_32px.png",0),"taiji_rotate frame0",2)
show(first("v7_fast_rune_kan.png",0),"rune_kan frame0",1)
show(first("v7_fast_rune_gen.png",0),"rune_gen frame0",1)
show(first("v7_wide_flame_1.png",0),"wide_flame_1 frame0",4)
show(first("v9_array_counterclockwise_slow.png",0),"array frame0",2)
show(first("v7_under_cauldron_fire.png",0),"under_cauldron_fire frame0",2)
show(first("v7_central_fire_bed.png",0),"central_fire_bed frame0",2)
show(first("v9_taiji_strike_0_stage_0.png",0),"strike0_0 frame0",1)
show(first("v3_star_0.png",0),"star_0 frame0",1)
