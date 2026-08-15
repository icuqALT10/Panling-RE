tp @e[type=villager,distance=..5,tag=buy_fish] ~ ~-1000 ~
summon villager 554 34 39 \
{Tags:["panling","buy_fish"],\
CustomNameVisible:true,CustomName:'{"translate":"plre.npc.name.buy_fish"}',\
 VillagerData:{profession:"minecraft:weaponsmith",level:26,type:"minecraft:plains"},HandItems:[{},{}],LastRestock:2147483648L,Xp:0,HandDropChances:[0.0f,0.0f],Inventory:[],Gossips:[],Invulnerable: 1b, PersistenceRequired: 1b,CanPickUpLoot: 0b, Age: 100000000,Brain: {memories:{"minecraft:job_site":{value:{pos:[I;47,65,199],dimension:"minecraft:overworld"}}}},Team:"normal",Rotation:[90f,0f]\
 ,can_interact:{active:true,command:"plre opengui villager @s east/buy_fish"},NoAI:1b\
}