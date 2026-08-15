execute unless score .system instance_model matches 1 run tellraw @a {"translate": "plre.instance.model_change.1"}
execute unless score .system instance_model matches 1 run return run scoreboard players set .system instance_model 1

scoreboard players reset .system instance_model
tellraw @a {"translate": "plre.instance.model_change.2"}