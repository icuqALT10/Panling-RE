execute store result score @s temp run clear @s #plre:fish 0

execute if score @s temp matches 10.. run return 1

return fail