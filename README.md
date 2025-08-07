
# Fork of [Victimarius/millmix](https://github.com/Victimarius/millmix) that fixes and tweaks even more things in Millénaire 1.12.2 mod making it more immersive.
This is my first mod ! :D

## Description
It's a mod that patches [Millenaire 8.x.x on 1.12.2](https://www.curseforge.com/minecraft/mc-mods/millenaire) with mixins, making it more immersive. I forked it from victimarius/millmix.
It fixes some log spam, tries to prevent building spawning and breaking landscape, improves some "goals" to make Millagers feel even more organic.
It also adds some values to "M" menu, you can now set villager names distance to 0 and totally hide names.
It's also useful for culture creators because it fixes millagers not planting if the farm building has no chests (only custom addons have farms without chests) 

### Current features :
| Target                        | Feature                                                                                  | Status      | Note                                                                                                                                                                           |
|-------------------------------|------------------------------------------------------------------------------------------|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Harvest goal**              | Makes villagers wait for crops to be ripe                                                | Implemented | prevent them to rush on the first ripe crop                                                                                                                                    |
|                               | Make villagers ignore missing soil                                                       | Implemented | if some soil blocks are missing, it won't prevent villagers to harvest the remaining. They can also harvest unfinished farm. (since 1.0.8)                                     |
|                               | Make villagers finish small farm field                                                   | Implemented | If the farm is small enough, they'll totally finish harvest it.                                                                                                                |
|                               | Make villagers harvest slower                                                            |             |                                                                                                                                                                                |
| **planting goal**             | Planting Seeds Fix in farms without inventory (custom content)                           | Implemented | Villagers now use seeds properly from their own inventory, the field storage, or their **house**. So now villagers handles farms that miss chests natively.                    |
| **world generation**          | Smoother Building Placement Near mountains (applies to walls since 1.1.2)                | Implemented | Stops buildings from popping up right on the edge of mountains or uneven ground by making sure the land around them is flat enough. Adjustable config : maxTerrainHeightDiff=8 |
|                               | Ability to disable walls generation in config (since 1.1.2)                              |             | Adjustable config : maxWallTerrainHeightDiff=8                                                                                                                                 |
| **villager behaviour**        | Include all MillMix fixes                                                                | Implemented | toggleable config : disableWalls=false                                                                                                                                         |
|                               | Makes villager toggledoor compatible with MalisisDoors                                   | Implemented |                                                                                                                                                                                |
|                               | Prevent millagers warping to the player if attacked                                      | Implemented |                                                                                                                                                                                |
|                               | Prevent player toggling seller ui if right clicking on millvillager while fighting       | Implemented |                                                                                                                                                                                |
| **Lumberman Chop trees goal** | Makes chop trees radius bigger                                                           | Implemented |                                                                                                                                                                                |
| **Visit goal**                | Adds a random offset where the villager goes for visit goals like patrol or godrinkcider | Implemented | Makes villagers cluster less so it's more immersive                                                                                                                            |
| **config menu "M"**           | Allows higher [STUPIDLY DANGEROUS] config settings values for "M" in-game menu           | Implemented | **DON'T MESS WITH VILLAGE RADIUS, 100 IS OK LOL** (up to 2000 with my mod iirc)                                                                                                |
|                               | Allow to set villager names distance to 0                                                | Implemented | Allows to totally hide villagers directly in-game (avoids to edit config-custom.txt and restart)                                                                               |
| **Pathfinding**               | Fixed `Error in onUpdate(). Check millenaire.log` that was in fact a pathfinding issue   | Implemented | It fixes a shitty error that was breaking immersion                                                                                                                            |
| **Log spam fix**              | Mute error `Could not find a villager type to create. Gender: 2`                         | Implemented | It fixes a shitty error that flood millenaire.log with errors, preventing millenaire.log polluting hard drive space and health                                                 |

## Prerequisites
- MixinBooter **Update it if my mod don't work**
### if you previously installed my bansoukou patch
- Remove Bansoukou and `/minecraft/bansoukou`   (if you don't use it for another things)
- Remove any old MillMix version (since it replaces it)

My mod should not break your save but please **make backups**

There still aren't other goal tweaks I talked about some times ago on Millénaire Discord such as guards actually patrolling or farmer children. At the moment, my mod just patches bits of game code, not culture related files like goals.txt. I will do, but I want to do it cleanly.
Also, note that I didn't carefully test all tweaks. If you have problems, report it here on issues tab or in the millenaire discord #jubitus.

### What's a goal ?
A goal is simply the target of a villager. For example, the farmer goals will be harvestWheat, plantWheat, getBackResourceHome among others.

### Millenaire website
https://millenaire.org/
### Millenaire discord 
https://discord.gg/nCXjd4s

### Thanks to
Victimarius
mouse0w0
Victimarius
mchorse
TudbuT
for [MillMix](https://github.com/Victimarius/millmix)

I used [TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) from CleanroomMC repo


Have a nice day
