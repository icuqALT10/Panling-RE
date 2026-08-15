#玩家个人计分板 
scoreboard objectives add level level
scoreboard objectives add hp health ["生命值"]
scoreboard objectives add player_online minecraft.custom:minecraft.leave_game ["玩家退出次数"]
scoreboard objectives add player_death deathCount "玩家是否死亡"

#倒计时触发器
scoreboard objectives add time_trigger dummy
    #人族村庄bgm时间倒数
scoreboard players set #5ticks_bgm_ren_all time_trigger 580
    #叶灵谷bgm时间倒数
scoreboard players set #5ticks_bgm_yao_all time_trigger 360
    #蜀山bgm时间倒数
scoreboard players set #5ticks_bgm_xian_all time_trigger 268
    #战神驻地bgm时间倒数
scoreboard players set #5ticks_bgm_zhan_all time_trigger 468
    #昆仑bgm时间倒数
scoreboard players set #5ticks_bgm_shen_all time_trigger 272
    #皇城bgm时间倒数
scoreboard players set #5ticks_bgm_middle_all time_trigger 452
    #蓬莱bgm时间倒数
scoreboard players set #5ticks_bgm_penglai_all time_trigger 476


#强加载
forceload add -184 -832
forceload add 1814 -772 1868 -829
forceload add 3119 -2237