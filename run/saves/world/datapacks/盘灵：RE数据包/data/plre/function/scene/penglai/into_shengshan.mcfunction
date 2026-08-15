tp @s 12 34 -908 90 ~

scoreboard players set @s locate_at 8

execute if function plre:check/curios/race/ren run return run advancement grant @s only plre:misson/ren/19
execute if function plre:check/curios/race/shen run return run advancement grant @s only plre:misson/shen/17
execute if function plre:check/curios/race/xian run return run advancement grant @s only plre:misson/xian/19
execute if function plre:check/curios/race/yao run return run advancement grant @s only plre:misson/yao/21
execute if function plre:check/curios/race/zhan run advancement grant @s only plre:misson/zhan/19