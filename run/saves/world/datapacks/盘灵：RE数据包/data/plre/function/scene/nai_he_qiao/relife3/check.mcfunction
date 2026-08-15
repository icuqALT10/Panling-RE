execute unless items entity @s container.* panlingre:respawn_stone run return run tellraw @s {"translate": "pl.info.relifeleft.fail"}

clear @s panlingre:respawn_stone 1

function plre:scene/nai_he_qiao/respawn


execute if function plre:check/curios/race/ren run return run function plre:scene/ren/spawn_location

execute if function plre:check/curios/race/shen run return run function plre:scene/shen/spawn_location

execute if function plre:check/curios/race/xian run return run function plre:scene/xian/spawn_location

execute if function plre:check/curios/race/yao run return run function plre:scene/yao/spawn_location

execute if function plre:check/curios/race/zhan run return run function plre:scene/zhan/spawn_location