from PIL import Image
import os
base=r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din"
def opaque_frac(f):
    a=f.getchannel("A")
    return sum(a.histogram()[1:])/(f.size[0]*f.size[1])
def adiff(a,b):
    ha=a.getchannel("A").histogram(); hb=b.getchannel("A").histogram()
    return sum(abs(x-y) for x,y in zip(ha,hb))/(2*a.size[0]*a.size[1])
rows=[]
for fn in sorted(os.listdir(base)):
    if not fn.endswith(".png"): continue
    im=Image.open(os.path.join(base,fn)).convert("RGBA")
    W,H=im.size
    if H<=W:
        rows.append((fn,W,H,1,1,1.0,0.0)); continue
    n=H//W
    fr=[im.crop((0,i*W,W,(i+1)*W)) for i in range(n)]
    fracs=[opaque_frac(f) for f in fr]
    nonblank=sum(1 for x in fracs if x>0.01)
    ds=[adiff(fr[i],fr[i+1]) for i in range(min(n-1,10))]
    rows.append((fn,W,H,n,nonblank/n,max(fracs),sum(ds)/max(1,len(ds))))
print(f'{"file":<38}{"W":>5}{"H":>6}{"frms":>6}{"nonblk%":>9}{"maxopq":>8}{"adiff":>7}')
for r in rows:
    print(f'{r[0]:<38}{r[1]:>5}{r[2]:>6}{r[3]:>6}{r[4]:>9.2f}{r[5]:>8.2f}{r[6]:>7.3f}')
