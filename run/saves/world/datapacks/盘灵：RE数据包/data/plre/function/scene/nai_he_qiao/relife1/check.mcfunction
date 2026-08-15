execute unless items entity @s container.* panlingre:respawn_stone run return run tellraw @s {"translate": "pl.info.relifeleft.fail"}

clear @s panlingre:respawn_stone 1

function plre:scene/nai_he_qiao/respawn

scoreboard players set @s locate_at 1

execute if score @s relife_pos matches 0 run return run tp @s 179.5 42.0 65.5 0 0

execute if score @s relife_pos matches 1 run return run tp @s 238.5 64.00 -207.5 0 0

execute if score @s relife_pos matches 2 run return run tp @s 688.5 70.00 114.5 0 0

execute if score @s relife_pos matches 3 run return run tp @s -326.5 58.00 586.5 -90 0

execute if score @s relife_pos matches 4 run return run tp @s -465.0 102.0 348.0 180 0

execute if score @s relife_pos matches 5 run return run tp @s -417.0 112.00 163.0 180 0

execute if score @s relife_pos matches 6 run return run tp @s -330.5 24.00 -699.5 0 0

execute if score @s relife_pos matches 7 run return run tp @s 316.0 50.00 -682.0 90 0


#若没有标点 返回种族地
execute if function plre:check/curios/race/ren run return run function plre:scene/ren/spawn_location

execute if function plre:check/curios/race/shen run return run function plre:scene/shen/spawn_location

execute if function plre:check/curios/race/xian run return run function plre:scene/xian/spawn_location

execute if function plre:check/curios/race/yao run return run function plre:scene/yao/spawn_location

execute if function plre:check/curios/race/zhan run return run function plre:scene/zhan/spawn_location