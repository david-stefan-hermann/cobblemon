execute store result score #pokemon ct if entity @e[type=cobblemon:pokemon]
tellraw @a ["CT-COUNT pokemon entities: ",{"score":{"name":"#pokemon","objective":"ct"}}]
say CT-DONE
