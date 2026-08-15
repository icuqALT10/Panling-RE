#皇城
execute if score #5ticks_bgm_middle_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_middle_now time_trigger 1
execute unless score #5ticks_bgm_middle_now time_trigger matches 0.. run function plre:bgm/middle
#人族村庄
execute if score #5ticks_bgm_ren_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_ren_now time_trigger 1
execute unless score #5ticks_bgm_ren_now time_trigger matches 0.. run function plre:bgm/ren
#蜀山
execute if score #5ticks_bgm_xian_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_xian_now time_trigger 1
execute unless score #5ticks_bgm_xian_now time_trigger matches 0.. run function plre:bgm/xian
#昆仑
execute if score #5ticks_bgm_shen_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_shen_now time_trigger 1
execute unless score #5ticks_bgm_shen_now time_trigger matches 0.. run function plre:bgm/shen
#叶灵谷
execute if score #5ticks_bgm_yao_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_yao_now time_trigger 1
execute unless score #5ticks_bgm_yao_now time_trigger matches 0.. run function plre:bgm/yao
#战神驻地
execute if score #5ticks_bgm_zhan_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_zhan_now time_trigger 1
execute unless score #5ticks_bgm_zhan_now time_trigger matches 0.. run function plre:bgm/zhan
#蓬莱
execute if score #5ticks_bgm_penglai_now time_trigger matches 0.. run scoreboard players remove #5ticks_bgm_penglai_now time_trigger 1
execute unless score #5ticks_bgm_penglai_now time_trigger matches 0.. run function plre:bgm/penglai