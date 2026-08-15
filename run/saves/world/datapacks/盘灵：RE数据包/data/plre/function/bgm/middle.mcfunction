stopsound @a[x=175,y=128,z=-63,distance=..320] * minecraft:bgm_middle
playsound minecraft:bgm_middle record @a[x=175,y=128,z=-63,distance=..320] 175 128 -63 19 1 0
#5ticks 重置
scoreboard players operation #5ticks_bgm_middle_now time_trigger = #5ticks_bgm_middle_all time_trigger