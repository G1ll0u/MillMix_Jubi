
# Fork of Victimarius/millmix that fixes and tweak even more things in Millénaire 1.12.2 mod.
This is my first mod ! :D

## Description
It's a mod that patches Millenaire 8.x.x on 1.12.2 with mixins, making it more immersive. I forked it from victimarius/millmix.

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
| **Log fix**                   | Made the error `Could not find a villager type to create. Gender: 2` silent since it dont break the game | Implemented                                | It fixes a shitty error that flood millenaire.log with errors, preventing millenaire.log polluting hard drive space |   |


* Rework of farming feature that makes them wait for crops to get ripe 
 * **NEW FEATURE** : they will harvest even if some soil blocks are missing (so if some blocks get sucked out by a tornado, they still harvest what remains :D)
    - **FIXED** : they will now finish harvest crops even if farm field is smaller that 64 they won't leave any wheat normally.
- This mod includes **MillMix**
     - I took the Millmix mixin class and it compiled sooo it should work but please tell me if it does not work.
- Added random offset of 2 blocks to `genericvisit` goals (like `patrol` or `godrinkcider`) for target position, they will "cluster" less
    - you can adjust it in goal.txt : randomoffset=x (in blocks, default 2 even if not set)
- Allow more **(DANGEROUS)** settings directly ingame like villageradius up to 3000 (that's stupid, normally it's max 100. Above 100 you get a message like "be careful it will lag above 100")
    - But you can also set villager name distance to 0 in-game :). So you can totally hide villagers name
- lumberman will chop trees more agressively (they shouldn't miss trees tell me if you think it's immersion-breaking or if broke)

## Prerequisites :
- MixinBooter `curseforge.com/minecraft/mc-mods/mixin-booter/files/all?page=1&pageSize=20`  (you may already have it installed if you have some mods)
### if you previously installed my patch that includes bansoukou
- Remove Bansoukou and `/minecraft/bansoukou`   (if you don't use it for another things)
- Remove any old MillMix version (since it replaces it)

It won't break your save but please make backups

There still aren't other goal tweaks I talked about some times ago on Millénaire Discord such as guards patrolling or farmer children. At the moment, my mod just patches bits of game code, not culture related files like goals.txt.
Also, note that I didn't test all tweaks for the forge version so if you see that features are missing, report it here.
