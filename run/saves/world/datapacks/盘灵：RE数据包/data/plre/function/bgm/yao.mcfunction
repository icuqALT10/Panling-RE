stopsound @a[x=2735,y=128,z=927,distance=..208] * minecraft:bgm_yao
playsound minecraft:bgm_yao record @a[x=2735,y=128,z=927,distance=..208] 2735 128 927 13 1 0

#5ticks 重置
scoreboard players operation #5ticks_bgm_yao_now time_trigger = #5ticks_bgm_yao_all time_trigger
