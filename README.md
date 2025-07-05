
# Fork of [Victimarius/millmix](https://github.com/Victimarius/millmix) that fixes and tweaks even more things in Millénaire 1.12.2 mod making it more immersive.
This is my first mod ! :D

## Description
It's a mod that patches [Millenaire 8.x.x on 1.12.2](https://www.curseforge.com/minecraft/mc-mods/millenaire) with mixins, making it more immersive. I forked it from victimarius/millmix.

### Current features :
| Target                        | Feature                                                                                                  | Status                                     | Note                                                                                                                |   |
|-------------------------------|----------------------------------------------------------------------------------------------------------|--------------------------------------------|---------------------------------------------------------------------------------------------------------------------|---|
| **Harvest goal**              | Makes villagers wait for crops to be ripe                                                                | Implemented                                | prevent them to rush on the first ripe crop                                                                         |   |
|                               | Make villagers ignore missing soil                                                                       | Implemented                                | if some soil blocks are missing, it won't prevent villagers to harvest the remaining                                |   |
|                               | Finish small farms                                                                                       | Implemented                                | If the farm is small enough, they'll totally finish harvest it                                                      |   |
| **millvillager**              | Include all MillMix fixes :                                                                              | Imoplemented                               |                                                                                                                     |   |
|                               | Makes villager toggledoor compatible with MalisisDoors                                                   | Implemented but to be tested (should work) |                                                                                                                     |   |
|                               | Prevent millagers warping to the player if attacked                                                      | Implemented but to be tested (should work) |                                                                                                                     |   |
|                               | Prevent player toggling seller ui if right clicking on millvillager while fighting                       | Implemented but to be tested (should work) |                                                                                                                     |   |
| **Lumberman Chop trees goal** | Makes chop trees radius bigger                                                                           | Implemented but need some user feedback    |                                                                                                                     |   |
| **GenericVisit goal**         | Adds a random offset where the villager goes for visit goals like patrol or godrinkcider                 | Implemented                                | Makes villagers cluster less so it's more immersive                                                                 |   |
| **Adjust config values**      | Allows higher [STUPIDLY DANGEROUS] config settings values for "M" in-game menu                           | Implemented                                | **DON'T MESS WITH VILLAGE RADIUS, 100 IS OK LOL** (up to 2000 with my mod iirc)                                     |   |
|                               | Allow to set villager names distance to 0                                                                | Implemented                                | Allows to totally hide villagers directly in-game (avoids to edit config-custom.txt and restart)                    |   |
| **Pathfinding**               | Fixed `Error in onUpdate(). Check millenaire.log` that was in fact a pathfinding issue                   | Implemented                                | It fixes a shitty error that was breaking immersion                                                                 |   |
| **Log fix**                   | Made the error `Could not find a villager type to create. Gender: 2` silent since it dont break the game | Implemented                                | It fixes a shitty error that flood millenaire.log with errors, preventing millenaire.log polluting hard drive space and health |   |

## Prerequisites
- MixinBooter `curseforge.com/minecraft/mc-mods/mixin-booter/files/all?page=1&pageSize=20`  (you may already have it installed if you have some mods)
### if you previously installed my bansoukou patch
- Remove Bansoukou and `/minecraft/bansoukou`   (if you don't use it for another things)
- Remove any old MillMix version (since it replaces it)

It won't break your save but please **make backups**

There still aren't other goal tweaks I talked about some times ago on Millénaire Discord such as guards actually patrolling or farmer children. At the moment, my mod just patches bits of game code, not culture related files like goals.txt. I will do, but I want to do it cleanly.
Also, note that I didn't test all tweaks. Please report it here on issues or in the millenaire discord #jubitus.

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


Have a nice day
