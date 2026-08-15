tellraw @s {"translate": "plre.instance.dialog.pangu.end.1"}
tellraw @s {"translate": "plre.instance.dialog.pangu.end.2"}
tellraw @s {"translate": "plre.instance.dialog.pangu.end.3"}
tellraw @s {"translate": "plre.instance.dialog.pangu.end.4"}

tp @s -23.5 48.00 -919.5 180 ~

scoreboard players set @s locate_at 8

plre bafangyi on @s

#进度给予
execute if function plre:check/curios/race/ren run advancement grant @s only plre:misson/ren/21
execute if function plre:check/curios/race/shen run advancement grant @s only plre:misson/shen/19
execute if function plre:check/curios/race/xian run advancement grant @s only plre:misson/xian/21
execute if function plre:check/curios/race/yao run advancement grant @s only plre:misson/yao/23
execute if function plre:check/curios/race/zhan run advancement grant @s only plre:misson/zhan/21

give @s panlingre:loot_key[panlingre:key_type="golden",panlingre:key_id="sheng_shan"]
execute unless score .system instance_model matches 1 run return 1

execute if function plre:check/curios/zhiye/warrior unless items entity @s container.* panlingre:ding_hai_shen_zhen run return run function plre:instances/pangu/reward/warrior
execute if function plre:check/curios/zhiye/archer unless items entity @s container.* panlingre:zhu_ri run return run function plre:instances/pangu/reward/archer
execute if function plre:check/curios/zhiye/warlock unless items entity @s container.* panlingre:hun_yuan_shen_din unless function plre:check/curios/ldl/hun_yuan_shen_din run return run function plre:instances/pangu/reward/warlock