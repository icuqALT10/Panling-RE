execute store result score @s temp run execute if items entity @s container.* orange_dye[custom_data~{id:"plre:te2"}]

execute if score @s temp matches 60.. run return 1

return fail
