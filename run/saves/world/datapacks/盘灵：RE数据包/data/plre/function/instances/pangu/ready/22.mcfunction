execute as @e[x=2992,y=0,z=-2400,dx=271,dy=255,dz=303,type=panlingre:pan_gu] in the_end run tp @s -3000 0 -3000

execute store result storage plre:boss boss.health float 2000 run execute if entity @a[x=2992,y=0,z=-2400,dx=271,dy=255,dz=303]
function plre:instances/pangu/ready/summon with storage plre:boss boss


scoreboard players set .system instance_pangu_tick 1


schedule function plre:instances/pangu/ready/23 12s replace