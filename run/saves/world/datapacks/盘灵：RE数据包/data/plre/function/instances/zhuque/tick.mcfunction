execute unless entity @s[x=2441,y=29,z=8,dx=61,dy=40,dz=61] run return run scoreboard players reset @s instance_zhuque_tick

scoreboard players remove @s instance_zhuque_tick 1

title @s actionbar {"translate": "plre.instance.djs","with": [{"score": {"name": "@s","objective": "instance_zhuque_tick"},"color": "gold"}]}

execute if score @s instance_zhuque_tick matches 0 run function plre:instances/zhuque/reward