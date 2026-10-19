# Test-only reward: observe ordering and duplicate dispatch without granting real loot.
execute if entity @s[tag=party_reward_clean] run tag @s add party_reward_duplicate
execute unless entity @s[tag=panlingre.instance.active] run tag @s add party_reward_clean
