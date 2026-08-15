advancement grant @s only plre:misson/qinglong

plre bless @s add qinglong

curios set bless @s 1
curios replace bless 0 @s with panlingre:bless_shengshou

dialog show qinglong_out

scoreboard players set @s locate_at 9

plre bafangyi on @s

tp @s 1697.0 116.0 726.0 0 ~

execute if function plre:check/curios/race/ren run return run plre task guide on misson/ren/misson5
execute if function plre:check/curios/race/shen run return run plre task guide on misson/shen/misson2
execute if function plre:check/curios/race/xian run return run plre task guide on misson/xian/misson5
execute if function plre:check/curios/race/yao run return run plre task guide on misson/yao/misson6
execute if function plre:check/curios/race/zhan run return run plre task guide on misson/zhan/misson7