execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon zombie ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","west","elite","box_zombie"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.north.box_zombie"}',\
DeathLootTable:"plre:monsters/north/elite/box_zombie",\
attributes:\
[\
{id:"generic.max_health",base:150},\
{id:"generic.movement_speed",base:0.2},\
{id:"generic.minecraft:attack_damage",base:13},\
{id:"generic.armor",base:0d},\
{id:"generic.follow_range",base:40d},\
{id:"generic.knockback_resistance",base:1d},\
],\
ArmorItems:\
[\
{},\
{},\
{},\
{id:"minecraft:leather_helmet",count:1b,components:{unbreakable:{}}}\
],\
HandItems:\
[\
{id:"iron_sword",count:1b,components:{unbreakable:{},enchantments:{knockback:1}}},\
{}\
],\
Team:"monster",PersistenceRequired:0b}