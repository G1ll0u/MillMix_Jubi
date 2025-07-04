
## First release of my fork of MillMix that replaces a patch that I posted on Millénaire discord. (bansoukou patch)
It is a fork of Victimarius/millmix.

Changelog
[1.0.5] - 2025-07-04
### fixes :
* Removed some log spam 
  * ERROR : Could not find a villager type to create
* Fixed pathfinding (cauldron fix)

[1.0.4] - 2025-07-04
### Features :
* Rework of farming feature that makes them wait for crops to get ripe [DESC TO BE REDONE DONT READ THAT SHIT]
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
- Remove any old MillMix 1.0.2 or 1.0.3 (since it replaces it)

It won't break your save.

There still aren't other goal tweaks I talked about some times ago on Millénaire Discord such as guards patrolling or farmer children. At the moment, my mod just patches bits of game code, not culture related files like goals.txt.
Also, note that I didn't test all tweaks for the forge version so if you see that features are missing, report it here.

## What is millmix-1.0.4-**CRL**.jar ?
- This is the current version of the mod that only works with cleanroom loader (will probably be merged in one jar in the future)
# what is cleanroom loader ?

- The forge 1.12.2 continuation that runs forge 1.12.2 on recent Java21+ and lwjgl3, keeping 99% mods compatibility.
It makes loading times really faster, it allows modders to make more "modern ?" mods or at least use modern dev environment to dev.
So if you don't know how to install cleanroom :
you should first read : `github.com/CleanroomMC/Cleanroom`

java 21+ :
adoptium.net/temurin/releases/?arch=x64&package=jre
Server start script recommended for Cleanroom :
`github.com/jchung01/Cleanroom-ServerStart-Scripts/`

You'll probably encounter problems if you're using old/weird mods while migrating your modpack to Cleanroom. You have to ensure you're using mods that are updated. Some old mods have been forked for being compatible for Cleanroom like Had Enough Items (that replaces Just Enough Items).
(had enough items also replaces Just Enough Items in forge 1.12.2 iirc)
Check your logs :D

If you're stuck while Cleanroom migration there is a Cleanroom discord where you can ask help.

## TemplateDevEnv
_For Kotlin see [TemplateDevEnvKt](https://github.com/CleanroomMC/TemplateDevEnvKt)_

Template workspace for modding Minecraft 1.12.2. Licensed under MIT, it is made for public use.

This template runs on Java 21! Currently utilizies **Gradle 8.12** + **[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 1.4.1** + **Forge 14.23.5.2847**.

With **coremod and mixin support** that is easy to configure.

### Instructions:

1. Click `use this template` at the top.
2. Clone the repository that you have created with this template to your local machine.
3. Make sure IDEA is using Java 21 for Gradle before you sync the project. Verify this by going to IDEA's `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM`.
4. Open the project folder in IDEA. When prompted, click "Load Gradle Project" as it detects the `build.gradle`, if you weren't prompted, right-click the project's `build.gradle` in IDEA, select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Run gradle tasks such as `runClient` and `runServer` in the IDEA gradle tab, or use the auto-imported run configurations like `1. Run Client`.

### Notes:
- Dependencies script in [gradle/scripts/dependencies.gradle](gradle/scripts/dependencies.gradle), explanations are commented in the file.
- Publishing script in [gradle/scripts/publishing.gradle](gradle/scripts/publishing.gradle).
- When writing Mixins on IntelliJ, it is advisable to use latest [MinecraftDev Fork for RetroFuturaGradle](https://github.com/eigenraven/MinecraftDev/releases).
