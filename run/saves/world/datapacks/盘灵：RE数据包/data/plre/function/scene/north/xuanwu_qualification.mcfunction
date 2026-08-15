execute unless score @s xuanwu_qualification matches 1.. run return run dialog show misson_cant_into

execute if score @s xuanwu_qualification matches 2 run return run tellraw @s {"translate":"pl.info.turtle_incheckagain"}

execute store success score @s temp \
if block -341 18 -694 minecraft:hopper{Items:[{Slot:2b,id:"panlingre:ys1_jin",count:10}]} \
if block -343 18 -696 minecraft:hopper{Items:[{Slot:2b,id:"panlingre:ys1_mu",count:10}]} \
if block -342 18 -698 minecraft:hopper{Items:[{Slot:2b,id:"panlingre:ys1_shui",count:10}]} \
if block -340 18 -699 minecraft:hopper{Items:[{Slot:2b,id:"panlingre:ys1_huo",count:10}]} \
if block -338 18 -697 minecraft:hopper{Items:[{Slot:2b,id:"panlingre:ys1_tu",count:10}]}

execute unless score @s temp matches 1 run return 0

data merge block -341 18 -694 {Items:[]}
data merge block -343 18 -696 {Items:[]}
data merge block -342 18 -698 {Items:[]}
data merge block -340 18 -699 {Items:[]}
data merge block -338 18 -697 {Items:[]}
tellraw @s {"translate":"pl.info.turtle_incheck"}
scoreboard players set @s xuanwu_qualification 2

advancement grant @s only plre:misson/north/qualification