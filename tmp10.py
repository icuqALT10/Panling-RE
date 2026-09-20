import zipfile
z=zipfile.ZipFile(r"build\moddev\artifacts\neoforge-21.1.224-sources.jar")
names=[n for n in z.namelist() if n.endswith(("client/renderer/block/model/BlockModel.java","client/renderer/block/model/BlockElement.java","client/renderer/block/model/BlockElementRotation.java","client/renderer/block/model/BlockFaceUV.java"))]
for n in names: print(n)
