say CT-SPAWN
execute as @a[tag=ct_started] at @s run function cobbletest:spawn_at {species:"pidgey"}
schedule function cobbletest:battle 60t
