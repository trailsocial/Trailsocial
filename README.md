# TrailSocial

A RuneLite plugin for the most optimal gaming gamers

## What it has

- Bankstanding XP - a joke skill for idling near a bank
- Clan Hall stats - balloons burst, gilded chains picked up, wealth looted, time spent sitting
- Bossing/raid death counter (CoX, ToB, ToA, and real boss NPCs)
- Gnome kill counter, Trailblazer cane held-time tracker
- Live trailsocial.net event notifications - sidebar badge, in-game popup, login splash
- Much More, and More To Come


## Project layout

Standard RuneLite plugin template
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
