tag @s add ct_started
scoreboard objectives add ct dummy
say CT-START
give @s cobblemon:poke_ball 32
give @s cobblemon:great_ball 16
give @s cobblemon:ultra_ball 16
give @s cobblemon:potion 8
# macro lines are parsed when called - plain lines are parsed at datapack load, before Cobblemon has species
function cobbletest:give {species:"charmander"}
say CT-GIVEN
schedule function cobbletest:spawn 100t
