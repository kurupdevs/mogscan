# MogScan — free PSL face rating

MogScan scans your face from three angles (front, left profile, right profile) and gives you
an honest PSL rating with a per-feature breakdown — jawline, cheekbones, eyes, symmetry,
facial thirds, nose, lips, chin — plus concrete softmaxxing tips to level up.

**100% free, 100% on-device.** No paywall, no subscription, no API key, no account.
Your photos never leave your phone.

## How it works

- **CameraX** captures the three required angles with a guided overlay.
- **AnalysisViewModel** runs the three photos through ML Kit's on-device face detection
  (bundled model, works offline) and feeds the landmarks + contours into an original
  geometric scoring engine (`FaceAnalyzer`): symmetry, jaw/cheek ratios, facial thirds,
  eye spacing and canthal tilt, nose/mouth and lip proportions — mapped onto the
  1.0–9.0 PSL scale with a /100 score.
- Weakest features get matched against a built-in softmaxxing advice database
  (mewing, chewing, posture, sleep, skincare basics, hairstyle, grooming). Non-surgical only.

## Build

Tagged pushes (`v*`) build a debug APK via GitHub Actions and attach it to the release.
Or build locally with Gradle 8.7 + JDK 17:

```
gradle :app:assembleDebug
```

## Notes

- Scores come from facial geometry measured in your photos — they estimate traits, not worth.
  Same lighting, same angle = comparable results.
- Improvement tips are softmaxxing only. The app never recommends surgery or medical procedures.
