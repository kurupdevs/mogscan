# MogScan — free PSL face rating

MogScan scans your face from three angles (front, left profile, right profile) and gives you
an honest PSL rating with a per-feature breakdown — jawline, cheekbones, eyes, symmetry,
skin and more — plus concrete softmaxxing tips to level up. No paywall, no subscription.

## How it works

- **CameraX** captures the three required angles with a guided overlay.
- **AnalysisViewModel** sends the three photos to Google's Gemini AI (`gemini-2.0-flash`)
  with a structured prompt and parses the JSON report: overall PSL (1–9), a /100 score,
  per-feature scores, strengths, and non-surgical improvement methods.
- You bring your own **free** Gemini API key (from [Google AI Studio](https://aistudio.google.com/app/apikey)).
  The key is stored only on your device.

## Build

Tagged pushes (`v*`) build a debug APK via GitHub Actions and attach it to the release.
Or build locally with Gradle 8.7 + JDK 17:

```
gradle :app:assembleDebug
```

## Notes

- Scores estimate photo-based traits under your lighting/angle — they are not a verdict on your worth.
- Improvement tips are softmaxxing only (skincare, hairstyle, posture, fitness, sleep, style).
  The app never recommends surgery or medical procedures.
