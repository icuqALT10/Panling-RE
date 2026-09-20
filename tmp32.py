from PIL import Image, ImageChops
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
def f0(fn):
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    return im.crop((0,0,W,W))
prev=None
print("cloud files: diff of frame0 vs previous file's frame0")
for i in range(64):
    fn=f"v9_cloud_slow_rotation_{i:02d}.png"
    a=f0(fn)
    if prev is not None:
        d=ImageChops.difference(a,prev).getchannel("A").histogram()
        tot=sum(d[1:]); frac=tot/(a.size[0]*a.size[1])
        print(f"  {i:02d} vs {i-1:02d}: alphadiff={frac:.3f}")
    prev=a
