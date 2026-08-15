setblock -184 99 -832 minecraft:stone_button
kill @e[x=-184,y=98,z=-821,distance=..40,type=armor_stand,limit=1]

effect clear @a[x=-184,y=98,z=-821,distance=..40] blindness
execute as @a[x=-184,y=98,z=-821,distance=..40] run function plre:instances/pangu/begin

playsound entity.wither.ambient ambient @a[x=2992,y=0,z=-2000,dx=271,dy=255,dz=303] 3139 175 -1840 10 1 1
execute as @a[x=2992,y=0,z=-2000,dx=271,dy=255,dz=303] run dialog show instance_pangu_6

summon armor_stand 3169 141 -1847 {ArmorItems:[{id:"minecraft:chainmail_boots",count:1},{id:"minecraft:chainmail_leggings",count:1},{id:"minecraft:chainmail_chestplate",count:1},{id:"minecraft:player_head",count:1,components:{profile:{properties:[{name:"textures",value:"eyJ0aW1lc3RhbXAiOjE0MTEyODYxMjYzMjIsInByb2ZpbGVJZCI6IjViYjE5ZjBjNDBjNjQzMmZhMGY0NTQyZDAzY2YzZGNjIiwicHJvZmlsZU5hbWUiOiJBdWRpYWNlMDgwOSIsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMTlmYzlhZTM3YmZkMTNlY2I5ZmNkMmJlNWYxOWRhZjkzOWI2OTA2ZjU5NjU3NmFlYWU1M2QxYjE1NGRlYiJ9fX0="}]}}}],CustomNameVisible:0b,Invulnerable:1b,DisabledSlots:2039552,Invisible:0b,NoBasePlate:1b,NoGravity:1b,ShowArms:1b,Small:0b,Pose:{Body:[0.0F,0.0F,0.0F],LeftArm:[20F,0.0F,30F],RightArm:[20F,0.0F,330F],LeftLeg:[0.0F,0.0F,0.0F],RightLeg:[0.0F,0.0F,0.0F],Head:[-25.0F,0.0F,0.0F]},Rotation:[80F,0.0F]}


schedule function plre:instances/pangu/ready/12 3.5s replace