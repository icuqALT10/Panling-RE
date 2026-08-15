function plre:loads/betaload
function plre:loads/gamerule
function plre:scene/load
function plre:instances/load

scoreboard objectives add 5ticks dummy "延迟tick触发倒计时"
scoreboard objectives add 20ticks dummy "延迟tick触发倒计时"

scoreboard objectives add temp dummy
scoreboard objectives add temp2 dummy
scoreboard objectives add temp3 dummy

scoreboard objectives add monster_count dummy

scoreboard objectives add player_id dummy "玩家id"
scoreboard objectives add entity_id dummy "其他实体id"

# 0奈何桥 1大陆 2-6人神仙妖战 7蓬莱 8圣山 9青龙神庙 10朱雀神庙 11白虎神庙 12玄武神庙
scoreboard objectives add locate_at dummy "当前位置"

# ======== 主线 ========
scoreboard objectives add misson_ren dummy "人族-主线进度"
scoreboard objectives add misson_shen dummy "神族-主线进度"
scoreboard objectives add misson_xian dummy "仙族-主线进度"
scoreboard objectives add misson_yao dummy "妖族-主线进度"
scoreboard objectives add misson_zhan dummy "战神族-主线进度"

scoreboard objectives add misson_south dummy "南方主线进度"
scoreboard objectives add misson_west dummy "西方主线进度"
scoreboard objectives add misson_north dummy "北方主线进度"

scoreboard objectives add qinglong_qualification dummy "青龙进入资格"
scoreboard objectives add zhuque_qualification dummy "朱雀进入资格"
scoreboard objectives add baihu_qualification dummy "白虎进入资格"
scoreboard objectives add xuanwu_qualification dummy "玄武进入资格"