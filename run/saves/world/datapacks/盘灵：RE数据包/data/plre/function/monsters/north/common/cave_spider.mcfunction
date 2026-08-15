execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon cave_spider ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","north","common","cave_spider"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.north.cave_spider"}',\
DeathLootTable:"plre:monsters/north/common/cave_spider",\
attributes:\
[\
{id:"generic.max_health",base:90},\
{id:"generic.movement_speed",base:0.3},\
{id:"generic.minecraft:attack_damage",base:12},\
{id:"generic.armor",base:0d},\
{id:"generic.follow_range",base:40d},\
],\
ArmorItems:\
[\
{},\
{},\
{},\
{}\
],\
HandItems:\
[\
{},\
{}\
],\
Team:"monster",PersistenceRequired:0b}