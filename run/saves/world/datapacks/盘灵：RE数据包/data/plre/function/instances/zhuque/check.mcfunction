execute if function plre:check/bless/zhuque run return run dialog show zhuque_into

execute unless score @s zhuque_qualification matches 2.. run return run dialog show misson_cant_into

scoreboard players set @s locate_at -1

plre bafangyi off @s

plre instance start zhuque