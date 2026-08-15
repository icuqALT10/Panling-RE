stopsound @a[x=354,y=128,z=-689,distance=..160] * minecraft:bgm_penglai
playsound minecraft:bgm_penglai record @a[x=354,y=128,z=-689,distance=..160] 354 128 -689 9 1 0

#5ticks 重置
scoreboard players operation #5ticks_bgm_penglai_now time_trigger = #5ticks_bgm_penglai_all time_trigger