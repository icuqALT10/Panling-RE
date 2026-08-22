execute as @a[x=-184,y=98,z=-821,distance=..40] run dialog show instance_pangu_1
execute as @a[x=-184,y=98,z=-821,distance=..40] run scoreboard players set @s in_instance 1
schedule function plre:instances/pangu/ready/2 3s replace