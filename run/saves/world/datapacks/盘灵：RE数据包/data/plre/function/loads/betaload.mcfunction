#玩家个人计分板 
scoreboard objectives add level level
scoreboard objectives add hp health ["生命值"]
scoreboard objectives add player_online minecraft.custom:minecraft.leave_game ["玩家退出次数"]
scoreboard objectives add player_death deathCount "玩家是否死亡"

#倒计时触发器


#强加载
forceload add -184 -832
forceload add 1814 -772 1868 -829
forceload add 3119 -2237
