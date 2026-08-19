execute as @e[x=2992,y=0,z=-2400,dx=271,dy=255,dz=303,type=panlingre:pan_gu] in the_end run tp @s -3000 0 -3000

summon panlingre:pan_gu 3119.5 129.00 -2237.5 {Rotation:[-90f,0f],Team:"monster",PersistenceRequired:1b}


scoreboard players set .system instance_pangu_tick 1


schedule function plre:instances/pangu/ready/23 12s replace