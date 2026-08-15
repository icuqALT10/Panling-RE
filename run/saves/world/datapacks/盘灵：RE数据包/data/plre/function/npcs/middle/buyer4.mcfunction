#收购员
tp @e[type=villager,distance=..5,tag=buyer4] ~ ~-1000 ~
summon villager 77 47 103 \
{Tags:["panling","buyer4"],\
CustomNameVisible:true,CustomName:'{"translate":"plre.npc.name.buyer4"}',\
 VillagerData:{profession:"minecraft:weaponsmith",level:26,type:"minecraft:taiga"},HandItems:[{},{}],LastRestock:2147483648L,Xp:0,HandDropChances:[0.0f,0.0f],Inventory:[],Gossips:[],Invulnerable: 1b, PersistenceRequired: 1b,CanPickUpLoot: 0b, Age: 100000000,Brain: {memories:{"minecraft:job_site":{value:{pos:[I;47,65,199],dimension:"minecraft:overworld"}}}},Team:"normal",Rotation:[-90f,0f]\
 ,can_interact:{active:true,command:"plre opengui villager @s middle/buyer4"},NoAI:1b\
}