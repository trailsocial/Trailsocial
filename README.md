# TrailSocial

A RuneLite plugin for the TrailBlazer Social Club: a joke "Bankstanding" skill,
Clan Hall and bossing stat tracking, and trailsocial.net event notifications.

## How it works

- Every game tick, checks whether your player is within a configurable radius
  (default 12 tiles) of any bank object (bank booth, chest, counter, table, GE
  booth, etc. - detected dynamically by scanning nearby scene tiles, not a
  hardcoded location list, so it works everywhere including minigames/instances).
- If you're near a bank and haven't moved for a configurable idle threshold
  (default 30 seconds), you start slowly accruing "Bankstanding" XP.
- XP accrues at a configurable, deliberately slow rate (default 300 xp/hour -
  similar vibe to Agility, just for standing still).
- The skill caps at level 99 / 200,000 xp, using the same growth curve shape
  as a real OSRS skill, just rescaled to a smaller max.
- A side-panel button shows your current level, xp, progress bar, and status,
  plus a checkbox to toggle the in-game overlay box.
- An in-game overlay box (draggable, like any RuneLite overlay) shows the same
  info live, and can be toggled from the panel or the plugin's config.
- Progress is saved per RuneOScape account and persists across sessions.
- Chat message + tray notification on level up (toggleable in config).
- Fake XP drop popup (icon + "+1 Bankstanding", fades out) each time you earn
  a whole point of XP, mimicking a real skill's xp drop. Toggleable via the
  "Show XP drops" config option - the real xp-drop widget only knows about
  the 23 actual skills, so this is a custom overlay built to look the same.

## Project layout

Standard RuneLite plugin template (same shape as RuneLite's official
[example-plugin](https://github.com/runelite/example-plugin)):

```
src/main/java/com/bankstandingxp/
  BankstandingXPPlugin.java   - core logic (detection, xp/level tracking, persistence)
  BankstandingXPConfig.java   - config options (radius, idle time, xp rate, etc.)
  BankstandingXPOverlay.java  - in-game overlay box
  BankstandingXPPanel.java    - side-toolbar panel UI
  BankstandingXP.java         - xp <-> level table (99 levels, capped at 200,000 xp)
  BankstandingStatus.java     - status enum used by both panel and overlay
src/test/java/com/bankstandingxp/
  BankstandingXPPluginTest.java - dev entry point to sideload + launch the client
```

## Building / running locally

1. Open this folder in IntelliJ IDEA (File > Open, point at this directory).
   Let Gradle import the project (it pulls `net.runelite:client` from RuneLite's
   own Maven repo, declared in `build.gradle`).
2. Create a Run/Debug configuration:
   - Type: Application
   - Main class: `com.bankstandingxp.BankstandingXPPluginTest`
   - Use classpath of module: `bankstanding-xp.test`
3. Run it. This launches a full RuneLite client in developer mode with the
   Bankstanding XP plugin already sideloaded and enabled - log in and stand
   next to a bank for 30+ seconds to test.

Alternatively from the command line:

```bash
gradlew.bat build
```

(You'll need a Gradle wrapper - run `gradle wrapper` once if `gradlew.bat`
isn't present, or just open the project in IntelliJ which bundles Gradle.)
