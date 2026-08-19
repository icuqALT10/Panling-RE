#==================================世界tick==================================#
scoreboard players add .system 5ticks 1
execute if score .system 5ticks matches 6 run scoreboard players set .system 5ticks 1
scoreboard players add .system 20ticks 1
execute if score .system 20ticks matches 21 run scoreboard players set .system 20ticks 1

#bgm
execute if score .system 5ticks matches 5 run function plre:bgm/bgm_tick

#==================================玩家tick=======================================#
execute as @a at @s run function plre:ticks/player

#==================================非玩家实体tick=================================#
execute as @e[type=!player,type=!item_frame,type=!glow_item_frame,type=!villager,type=!painting] at @s run function plre:ticks/entity