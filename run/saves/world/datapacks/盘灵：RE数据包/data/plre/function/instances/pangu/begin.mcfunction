tp @s 3175 132 -1839 90 -60

scoreboard players set @s locate_at -1

plre bafangyi off @s

#进度给予
execute if function plre:check/curios/race/ren run advancement grant @s only plre:misson/ren/20
execute if function plre:check/curios/race/shen run advancement grant @s only plre:misson/shen/18
execute if function plre:check/curios/race/xian run advancement grant @s only plre:misson/xian/20
execute if function plre:check/curios/race/yao run advancement grant @s only plre:misson/yao/22
execute if function plre:check/curios/race/zhan run advancement grant @s only plre:misson/zhan/20