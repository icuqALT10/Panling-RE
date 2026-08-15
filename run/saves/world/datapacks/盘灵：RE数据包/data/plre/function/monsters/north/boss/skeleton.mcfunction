execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon skeleton ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","north","boss","skeleton"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.north.bossskeleton"}',\
DeathLootTable:"plre:monsters/north/boss/skeleton",\
attributes:\
[\
{id:"generic.max_health",base:1000},\
{id:"generic.movement_speed",base:0.3},\
{id:"generic.minecraft:attack_damage",base:0},\
{id:"generic.armor",base:0d},\
{id:"generic.follow_range",base:50d},\
{id:"panlingre:arrow_damage",base:12},\
],\
ArmorItems:\
[\
{},\
{},\
{},\
{id:"minecraft:golden_helmet",count:1b,components:{unbreakable:{},enchantments:{thorns:1}}}\
],\
HandItems:\
[\
{id:"minecraft:bow",components:{unbreakable:{},enchantments:{punch:2}}},\
{}\
],\
Team:"monster",PersistenceRequired:0b}