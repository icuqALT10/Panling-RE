#队伍
team add normal
team modify normal color yellow
team modify normal friendlyFire false
team modify normal deathMessageVisibility always
team modify normal collisionRule never

team add monster ["怪物队伍"]
team modify monster color dark_red
team modify monster friendlyFire false
team modify monster collisionRule never

team add other_entity "其他无碰撞箱的实体"
team modify other_entity color yellow
team modify other_entity collisionRule never

#准则
setworldspawn 183 190 -1768 -90
gamerule commandBlockOutput false
gamerule logAdminCommands false
gamerule doFireTick false
gamerule doMobSpawning false
gamerule doTileDrops true
gamerule doWeatherCycle true
gamerule mobGriefing false
gamerule spawnRadius 0
gamerule keepInventory true
gamerule doPatrolSpawning false
gamerule doTraderSpawning false
gamerule doLimitedCrafting false
gamerule doImmediateRespawn true
gamerule maxCommandChainLength 10000000
gamerule doWardenSpawning false
gamerule doVinesSpread false
gamerule randomTickSpeed 0
gamerule snowAccumulationHeight 0
gamerule showDeathMessages true

difficulty hard