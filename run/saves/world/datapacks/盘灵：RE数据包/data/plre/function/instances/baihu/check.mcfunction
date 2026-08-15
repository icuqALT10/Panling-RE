execute if function plre:check/bless/baihu run return run dialog show baihu_into

execute unless score @s baihu_qualification matches 2.. run return run dialog show misson_cant_into

scoreboard players set @s locate_at -1

plre bafangyi off @s

tp @s 1827.5 11.00 -810.5 -135 ~

execute positioned 1844.5 17.00 -818.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1850.5 26.00 -816.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1842.5 23.00 -788.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1846.5 27.00 -774.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1832.5 42.00 -810.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1858.5 53.00 -800.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1846.5 67.00 -794.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon

execute positioned 1852.5 72.00 -810.5 unless entity @e[type=skeleton,distance=..1] run function plre:instances/baihu/summon