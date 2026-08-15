execute if score @s misson_north matches 1 run return run plre task guide on misson/north/misson1
execute if score @s misson_north matches 2 run return run plre task guide on misson/north/misson2
execute if score @s misson_north matches 3 run return run plre task guide on misson/north/misson2
execute if score @s misson_south matches 4 if function plre:check/bless/xuanwu run return run plre task guide on misson/north/misson2
execute if score @s misson_north matches 4 if score @s xuanwu_qualification matches 1 run return run plre task guide on misson/north/qualification
execute if score @s misson_north matches 4 if score @s xuanwu_qualification matches 2 run return run plre task guide on misson/xuanwu
execute if score @s misson_north matches 5 run return run plre task guide off