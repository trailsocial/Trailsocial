# TrailSocial

A RuneLite plugin for the most optimal gaming gamers

## What it has

- Bankstanding XP - a joke skill for idling near a bank
- Clan Hall stats - balloons burst, gilded chains picked up, wealth looted, time spent sitting
- Bossing/raid death counter (CoX, ToB, ToA, and real boss NPCs)
- Gnome kill counter, Trailblazer cane held-time tracker
- Time spent inside vs. outside the Grand Exchange
- Optional leaderboard sync - rank your Bankstanding XP and stats against the clan on trailsocial.net
- Live trailsocial.net event notifications - sidebar badge, in-game popup, login splash
- Much More, and More To Come


## Leaderboard sync (optional, off by default)

The plugin can send your Bankstanding XP and stats to the leaderboard at
[trailsocial.net/leaderboard](https://trailsocial.net/leaderboard). Nothing is sent until you opt in:

1. Log in at trailsocial.net, open your profile, and find **Leaderboard Sync**.
2. Add your RuneScape account (clan members only, listed by their Wise Old Man name) and generate a link code.
3. Paste the code into the plugin settings under **Leaderboard > Leaderboard link code**.

While a code is set, the plugin sends this to `https://trailsocial.net/.netlify/functions/bankstanding` about every
5 minutes and when you log out: the link code, your in-game name, your Bankstanding XP, and the stats shown in the
plugin's sidebar (bankstanding time, Clan Hall wealth/balloons/chains/sitting time, boss deaths, gnomes killed,
cane held time, time in and out of the Grand Exchange). It sends no account hash, no other game data, and no
inventory, bank or chat contents. Clear the code to stop; you can also revoke it from your profile.

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
  LeaderboardSync.java        - opt-in leaderboard reporting (payload, HTTP, status messages)
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
