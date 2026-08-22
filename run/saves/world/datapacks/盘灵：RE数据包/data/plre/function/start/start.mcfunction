spawnpoint @s 205 54 -1771 0

curios replace ldl 0 @s with air
clear @s

#仓库 开启
plre byd on

give @s panlingre:tong_qian 15
give @s bread 32
give @s panlingre:he_ding_dan
execute if function plre:check/curios/zhiye/warlock run function plre:give/item/start_warlock
execute if function plre:check/curios/zhiye/warrior run advancement grant @s only plre:check/zhiye/warrior
execute if function plre:check/curios/zhiye/archer run advancement grant @s only plre:check/zhiye/archer


execute if function plre:check/curios/race/ren run return run function plre:start/start/ren
execute if function plre:check/curios/race/shen run return run function plre:start/start/shen
execute if function plre:check/curios/race/xian run return run function plre:start/start/xian
execute if function plre:check/curios/race/yao run return run function plre:start/start/yao
execute if function plre:check/curios/race/zhan run function plre:start/start/zhan