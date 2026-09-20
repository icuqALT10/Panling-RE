import zipfile
z=zipfile.ZipFile(r"build\moddev\artifacts\neoforge-21.1.224-sources.jar")
s=z.read("net/minecraft/client/renderer/entity/ItemRenderer.java").decode("utf-8","replace")
i=s.find("    public void render(")
print(s[i:i+4200])
