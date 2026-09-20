import struct,glob,os
def png_size(p):
    with open(p,"rb") as f:
        d=f.read(33)
    if d[:8]!=b"\x89PNG\r\n\x1a\n": return None
    w,h=struct.unpack(">II",d[16:24])
    return w,h
for p in sorted(glob.glob(r"src\main\resources\assets\panlingre\textures\item\hun_yuan_shen_din\*.png")):
    print(os.path.basename(p), png_size(p))
