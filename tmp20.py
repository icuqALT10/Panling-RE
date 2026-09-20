import struct,glob,os
def png_size(p):
    with open(p,"rb") as f: d=f.read(33)
    if d[:8]!=b"\x89PNG\r\n\x1a\n": return None
    return struct.unpack(">II",d[16:24])
for fol in ["chi_tong_lu","suo_hun_lu","qi_sha_din","huang_tong_lu"]:
    g=glob.glob(rf"src\main\resources\assets\panlingre\textures\item\{fol}\*.png")
    print(fol, [ (os.path.basename(x), png_size(x)) for x in g ])
