execute store result score @s monster_count run execute if entity @e[tag=monster,distance=..10]

execute if score @s monster_count matches 9.. run return 0

summon skeleton ~ ~ ~ {CanPickUpLoot:false,ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f],Health:10000000000.0f,\
Tags:["monster","north","common","skeleton"],\
CustomNameVisible:1b,CustomName:'{"translate":"plre.monster.north.skeleton"}',\
DeathLootTable:"plre:monsters/north/common/skeleton",\
attributes:\
[\
{id:"generic.max_health",base:90},\
{id:"generic.movement_speed",base:0.2},\
{id:"generic.minecraft:attack_damage",base:0},\
{id:"generic.armor",base:0d},\
{id:"generic.follow_range",base:40d},\
{id:"panlingre:arrow_damage",base:8},\
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
{id:"minecraft:bow",components:{unbreakable:{},enchantments:{punch:1}}},\
{}\
],\
Team:"monster",PersistenceRequired:0b}