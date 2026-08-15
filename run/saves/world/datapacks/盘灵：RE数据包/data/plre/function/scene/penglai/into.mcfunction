execute unless function plre:check/bless/qinglong run return fail
execute unless function plre:check/bless/zhuque run return fail
execute unless function plre:check/bless/baihu run return fail
execute unless function plre:check/bless/xuanwu run return fail

tellraw @s {"translate":"pl.info.tppenglai"}
effect give @s nausea 8 0
tp @s 418.0 36 -635

scoreboard players set @s locate_at 7