execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon blaze ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","south","common","blaze"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.south.box2_blaze"}',\
DeathLootTable:"plre:monsters/south/elite/box2_blaze",\
attributes:\
[\
{id:"generic.max_health",base:60},\
{id:"generic.movement_speed",base:0.2},\
{id:"generic.minecraft:attack_damage",base:0},\
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