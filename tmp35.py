import zipfile,re
z=zipfile.ZipFile(r"build\moddev\artifacts\neoforge-21.1.224-sources.jar")
s=z.read("net/minecraft/client/renderer/entity/ItemRenderer.java").decode("utf-8","replace")
for m in re.finditer(r"\n    (public|private|protected).*\(", s):
    print(m.group(0).strip())
print("LEN",len(s))
i=s.find("void render(ItemStack")
print(s[i:i+3000])
