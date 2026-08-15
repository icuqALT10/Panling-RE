#收购员
tp @e[type=villager,distance=..5,tag=buyer2] ~ ~-1000 ~
summon villager 90 47 112 \
{Tags:["panling","buyer2"],\
CustomNameVisible:true,CustomName:'{"translate":"plre.npc.name.buyer2"}',\
 VillagerData:{profession:"minecraft:weaponsmith",level:26,type:"minecraft:plains"},HandItems:[{},{}],LastRestock:2147483648L,Xp:0,HandDropChances:[0.0f,0.0f],Inventory:[],Gossips:[],Invulnerable: 1b, PersistenceRequired: 1b,CanPickUpLoot: 0b, Age: 100000000,Brain: {memories:{"minecraft:job_site":{value:{pos:[I;47,65,199],dimension:"minecraft:overworld"}}}},Team:"normal",Rotation:[-90f,0f]\
 ,can_interact:{active:true,command:"plre opengui villager @s middle/buyer2"},NoAI:1b\
}