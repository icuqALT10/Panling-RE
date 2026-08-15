execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon wither_skeleton ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","west","boss","wither_skeleton"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.west.boss_wither_skeleton"}',\
DeathLootTable:"plre:monsters/west/boss/wither_skeleton",\
attributes:\
[\
{id:"generic.max_health",base:700},\
{id:"generic.movement_speed",base:0.3},\
{id:"generic.minecraft:attack_damage",base:8},\
{id:"generic.armor",base:0d},\
{id:"generic.follow_range",base:40d},\
],\
ArmorItems:\
[\
{},\
{},\
{},\
{id:"iron_helmet",components:{unbreakable:{}}}\
],\
HandItems:\
[\
{id:"golden_sword",count:1b,components:{unbreakable:{},enchantments:{knockback:3}}},\
{}\
],\
Team:"monster",PersistenceRequired:0b}