# Later — schedule WhatsApp messages on Android

A personal-use Android app that lets you write a WhatsApp message now and have it go
out at a time you pick. Not published anywhere; built as a debug APK you sideload
onto your own phone.

## How it works

WhatsApp has no API for personal accounts, so nothing can send on your behalf from a
server. Later works entirely on your phone:

1. You compose the message in Later. It is stored in a local database — nothing leaves
   the device.
2. An exact alarm (`AlarmManager.setExactAndAllowWhileIdle`) wakes the app at the
   scheduled moment.
3. A short-lived foreground service opens the right WhatsApp chat with the text already
   in the composer.
4. An accessibility service — which you grant once in Settings — checks that the text on
   screen matches what you wrote, presses send, and backs out.

**If anything about step 3 or 4 is not right, the message is not lost.** Later posts a
high-priority notification instead: one tap opens WhatsApp with everything filled in,
and if the accessibility service is running it still presses send for you. Every
attempt, automatic or not, is recorded in the History screen.

### What it can and cannot do

- Individual chats only, addressed by phone number (with country code). Groups are not
  supported — WhatsApp's `wa.me` links only address numbers.
- Repeats: once, daily, weekly, monthly, yearly. Recurrence is computed forward in local
  wall-clock time, so a daily 9am message stays at 9am across daylight-saving changes.
- **Your phone must be unlocked at the scheduled moment for auto-send.** We cannot see
  or drive WhatsApp's UI behind a lockscreen — locked sends become tap-to-send
  notifications.
- Automating WhatsApp is against its Terms of Service. For a handful of personal
  messages a day the practical risk is low, but it is not zero. Your call.
- The accessibility service matches WhatsApp's view IDs. A WhatsApp redesign can break
  auto-send; the tap-to-send fallback keeps working regardless.

## Getting the APK

Every push builds one in CI:

1. Go to the repo's **Actions** tab → the latest **Build APK** run.
2. Download the `later-debug-apk` artifact and unzip it.
3. Copy `app-debug.apk` to your phone and open it. Allow "install unknown apps" when
   prompted.

Or build it yourself with Android Studio (Ladybug or newer): open the project, let it
generate the Gradle wrapper, then **Build → Build APK(s)**.

## First-run setup

Open Later → the gear icon. Work down the checklist:

| Step | Required | Why |
| --- | --- | --- |
| WhatsApp installed | yes | everything depends on it |
| Exact alarms | yes | otherwise Android may hold a send back by minutes |
| Notifications | yes | carries the tap-to-send fallback |
| Auto-send (accessibility) | for automation | presses the send button |
| Display over other apps | for automation | the reliable way to let Later open WhatsApp in the background; Later tries without it too |
| Unrestricted battery use | recommended | stops Android sleeping through a send |

Skip the last three and Later still works — every message just waits for one tap.

## If a permission will not turn on

Android 13 and newer put accessibility, and often the overlay and battery settings,
behind **restricted settings** for any app installed outside an app store. The toggle
looks available but silently refuses, or is greyed out.

To lift it: **Settings → Apps → Later → ⋮ (top right) → Allow restricted settings**.
Then grant the permissions as normal.

If that menu item is missing or greyed out — common on Xiaomi, Samsung and Oppo skins —
install over ADB instead, which is not subject to the restriction at all:

```
adb install -r app-debug.apk
```

Or grant everything directly from your computer:

```
adb shell appops set com.anik.later ACCESS_RESTRICTED_SETTINGS allow
adb shell appops set com.anik.later SYSTEM_ALERT_WINDOW allow
adb shell settings put secure enabled_accessibility_services \
  com.anik.later/com.anik.later.send.WhatsAppAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell dumpsys deviceidle whitelist +com.anik.later
```

## Project layout

```
app/src/main/java/com/anik/later/
├── data/          Room entities, DAOs, database
├── scheduling/    AlarmManager wiring, recurrence, boot re-arming
├── send/          delivery service, accessibility service, permissions, notifications
└── ui/            Compose screens (list, editor, setup, history)
```
