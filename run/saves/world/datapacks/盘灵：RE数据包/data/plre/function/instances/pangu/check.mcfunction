execute if entity @a[x=2992,y=0,z=-2000,dx=271,dy=255,dz=303] as @a[x=-184,y=98,z=-821,distance=..40] run return run dialog show instance_pangu_resistance

execute if entity @a[x=2992,y=0,z=-2400,dx=271,dy=255,dz=303] as @a[x=-184,y=98,z=-821,distance=..40] run return run dialog show instance_pangu_resistance

scoreboard players reset .system instance_pangu_tick

setblock -184 99 -832 minecraft:air

schedule function plre:instances/pangu/ready/1 4t replace