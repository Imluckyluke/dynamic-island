# Dynamic Island for Android

Prototype Android app that shows a floating Dynamic Island-style pill around the display cutout.

## MVP

- Floating island overlay with compact and expanded states
- Notification listener forwards alerts to the island
- Optional suppression of the original notification to reduce duplicates
- Media title/artist with play, pause, next, and previous controls
- Charging status and a simple countdown timer
- Permission setup for overlay, notification access, and battery optimization

## Important limitation

Android does not guarantee that the original heads-up notification can be hidden before it is shown. Suppression removes the original notification as quickly as possible, but system notifications, calls, alarms, and some OEM behavior may still appear briefly.
