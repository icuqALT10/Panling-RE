import zipfile,re
z=zipfile.ZipFile(r"build\moddev\artifacts\neoforge-21.1.224-sources.jar")
s=z.read("net/minecraft/client/renderer/block/model/BlockModel.java").decode("utf-8","replace")
i=s.find("getRotation")
print(s[:200])
# print deserializer part
j=s.find("class Deserializer")
print(s[j:j+7000])
