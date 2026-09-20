import zipfile
from pathlib import Path

MC = Path('C:/Users/icuqA/.gradle/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_9126a95b86f1792ecbe3ca980c77a3d0423e89f7_output.jar')
z = zipfile.ZipFile(MC)
src = z.read('net/minecraft/world/entity/player/Player.java').decode('utf-8', 'replace').splitlines()
start = None
for i, l in enumerate(src):
    if 'public void attack(Entity target)' in l:
        start = i
        break
for j in range(start, start + 100):
    print(f'{j+1}: {src[j]}')
