execute store result score @s temp run clear @s orange_dye[custom_data~{id:"plre:te2"}] 0

execute if score @s temp matches 60.. run return 1

return fail
