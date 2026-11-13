# GainForge: offline muscle and weight-gain coach

**By STG Studios** · [License](LICENSE) · [Terms of Service](TERMS.md)

A native Android app (Kotlin + Jetpack Compose) with a matching **Galaxy Watch (Wear OS)** app. Everything works offline: data lives only on the phone, and the watch syncs over Bluetooth.

Download the official APKs from the **[Releases](../../releases)** page:

| File | Install on |
|---|---|
| `GainForge-phone.apk` | Android phone (Android 8.0+) |
| `GainForge-watch.apk` | Galaxy Watch4 or newer (Wear OS 3+) |

## What's inside
**First launch questionnaire (10 steps):** who it's for (you or a friend), body basics, lifestyle, training setup, a health check with injuries/conditions, goal physique (Lean / Athletic / Bodybuilder / Powerbuilder), diet and allergies, foods you like, and eating habits. It ends with your **system-set ideal goal**.

**How the ideal goal is set:** body fat (yours, or estimated from BMI) → lean mass → FFMI. Each physique has a target FFMI and body-fat %, so the ideal weight is target lean mass ÷ (1 − target body fat). Gain rate depends on training age and pace. Calories come from Mifflin-St Jeor × activity, plus training burn, plus a surplus. Protein, carbs, fat and water are set from there.

**Tabs:**
- **Today**: calorie ring, protein and water, quick-add food and water, today's workout, goal progress, next meals, and a daily tip.
- **Train**: an auto-built program (full body / upper-lower / PPL) matched to your goal, experience, equipment and session length. Exercises are swapped around knee, back and shoulder injuries. Live set logging (weight and reps), with a rest timer that starts automatically. Also history and a weekly streak.
- **Timers**: stopwatch with laps, countdown with presets, and intervals (Tabata / HIIT / EMOM / Boxing / custom). Each one beeps and vibrates. The rest timer floats over every tab.
- **Meals**: a daily meal plan built from the foods each person likes, with servings scaled to hit their calories, plus a shuffle button. A recipe book with 36 recipes, search, favorites, a match %, and full ingredients and steps. Recipes that conflict with the diet or allergies are never shown.
- **Goals**: the ideal-goal hero card, a physique picker, gain pace, an optional custom target, macros, body stats (BMI, BF%, FFMI), a weight log with chart, and coach notes and warnings.
- **Profile**: photo or emoji avatar, name, 9 accent colors, System/Light/Dark theme, imperial/metric units, multiple profiles (you plus friends) with switching, edit the questionnaire, and Galaxy Watch status and sync.

**Galaxy Watch app:** calorie and protein rings, today's workout with a one-tap "set done" that starts a rest countdown with haptics, live heart rate, rest timer, intervals, stopwatch, and water +250 ml. Workouts and water logged on the watch are sent back to the phone. The last synced plan is cached, so it still works away from the phone.

## Install on the phone
1. Copy `GainForge-phone.apk` to the phone and open it. Allow "Install unknown apps" for your file manager.
2. Open GainForge and answer the questionnaire.

## Install on the Galaxy Watch
The watch app has to be sideloaded once over Wi-Fi:
1. On the watch: **Settings → About watch → Software info** → tap **Software version** 5 times to turn on Developer options.
2. **Settings → Developer options** → turn on **ADB debugging** and **Wireless debugging**. Tap **Wireless debugging → Pair new device** to see a pairing code and IP:port. The watch and PC must be on the same Wi-Fi.
3. On the PC, using `adb` from the [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools):
   ```
   adb pair <watch-ip>:<pair-port>        (enter the 6-digit code)
   adb connect <watch-ip>:<debug-port>
   adb -s <watch-ip>:<debug-port> install GainForge-watch.apk
   ```
4. Open GainForge on the watch. Allow the heart-rate sensor when asked. With the phone nearby (paired through the Galaxy Wearable app), your plan syncs automatically. You can also tap **Profile → Sync to watch now**.

> Both APKs must keep the same package name (`com.gainforge.app`) and be signed with the same key, or the phone and watch can't talk to each other. Build them together as shown below.

## Build from source
For STG Studios and authorized contributors only. The [License](LICENSE) doesn't allow publishing or sharing your own builds.

Requirements: JDK 17 and the Android SDK (platform 36, build-tools 35). Create `local.properties` with your SDK location, for example `sdk.dir=/path/to/Android/Sdk`. Then run:
```
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleRelease
```
On Windows, use `set JAVA_HOME=C:\path\to\jdk-17` and `gradlew assembleRelease`.

The outputs are `app/build/outputs/apk/release/app-release.apk` and `wear/build/outputs/apk/release/wear-release.apk`.

## Code map
- `app/.../logic/GoalEngine.kt`: ideal goal and macro calculations
- `app/.../logic/Programs.kt`: workout program builder and injury swaps
- `app/.../logic/Recipes.kt`: recipe database and meal planner
- `app/.../logic/TimerEngine.kt`: timers and workout session
- `app/.../ui/*`: glass UI components and the six tabs plus the questionnaire
- `app/.../wear/WatchSync.kt`: phone ↔ watch Data Layer sync
- `wear/...`: Wear OS app

## License and Terms
GainForge is **proprietary software**. You may view the code and install the official release APKs for personal, non-commercial use. Copying, modifying, redistributing or rebranding it is not allowed without written permission. See [LICENSE](LICENSE).

Using the app means you agree to the [Terms of Service](TERMS.md). GainForge gives general fitness and nutrition guidance, **not medical advice**. Talk to a doctor before starting a new diet or exercise program.

© 2026 STG Studios. All rights reserved.
