import zipfile
z=zipfile.ZipFile(r"build\moddev\artifacts\neoforge-21.1.224-sources.jar")
for n in ["net/minecraft/client/renderer/block/model/BlockElementRotation.java","net/minecraft/client/renderer/block/model/BlockElement.java"]:
    print("="*30, n)
    print(z.read(n).decode("utf-8","replace"))
