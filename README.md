# Moggr — Looksmaxx Face Rating

**Free PSL face rating. Three angles. Zero paywall. No account. No BS.**

Moggr measures your facial structure from three photos (front + both profiles) right on your phone and rates it on the real **PSL 1–8 scale** — LTN, MTN, HTN, Chadlite, Chad. Then it tells you exactly what to fix first. No sugarcoating, no "you're perfect the way you are" energy. Just measurements and a softmaxxing roadmap.

## What it actually does

1. **Watch the intro, answer 5 quick questions** (name, language, height, DOB, goal).
2. **Capture 3 angles** — front, left profile, right profile — with guided on-screen positioning.
3. **On-device analysis** — ML Kit face detection (bundled model, no network) measures 15 features from your landmarks and contours:
   - Symmetry, Facial thirds, Facial fifths, Midface ratio, **Eye spacing (ESR)**, **FWHR**, Eyes, Nose, Lips, Jawline, Chin, Brows, Cheekbones, **Jaw angle**, **Side profile**
4. **Your PSL report** — overall score on the 1–8 scale with your community tier, strongest features, weakest features, and concrete softmaxxing improvements ranked by impact.
5. **Moggr Coach** — a separate in-app coach tab. Ask anything ("what's dragging my score?", "best haircut for my face shape?") or send a fresh photo and it breaks it down: verdict → why → top 3 fixes → what not to worry about. Blunt older-brother energy, softmaxxing only.

## The PSL scale (no bluepill)

The community rates faces 1–8. Moggr uses the real tiers:

| Score | Tier |
|---|---|
| 7.75+ | Gigachad — near-mythical |
| 7.0–7.74 | Chad |
| 6.0–6.99 | Chadlite |
| 5.0–5.99 | HTN — High Tier Normie |
| 3.0–4.99 | MTN — Mid Tier Normie |
| 1.4–2.99 | LTN — Low Tier Normie |
| < 1.4 | Sub-5 — maximum ascension potential |

These are community conventions from looksmaxxing forums, not medical science. Moggr reports them straight because that's what you asked for — a real number, not a participation trophy.

## What makes it different

- **Actually free.** The full report, all 15 features, the coach — no unlock screen, no subscription, no "premium rating."
- **Three angles, not one selfie.** Most rating apps score a single front photo. Moggr reads front + both profiles, and your side profile genuinely affects the score (chin projection / forward growth estimate).
- **Real measurements, not vibes.** Every score comes from landmark geometry — distances and ratios computed from your face. Nothing is random, nothing is made up.
- **Your face stays on your phone.** The core scan is 100% on-device. No account, no upload, no cloud.

## Privacy — read this

Two separate systems, two separate privacy models:

- **Face scan:** 100% on-device. Your photos are processed by the bundled ML Kit model on your phone and never uploaded anywhere. No account exists to tie them to.
- **Moggr Coach (chat):** runs on a free keyless chat API, so **your typed questions are sent to that API** to generate replies. If you attach a photo in the coach tab, the photo is **scanned on your phone** and only the *measurements as text* (e.g. "jawline 6.2, symmetry 91%") are sent — the image itself never leaves your device. Conversations are sent with `private=true` (kept off the public feed), but don't share anything you wouldn't want a stranger's server to see.

## Install

1. Download the APK from [Releases](../../releases) (`Moggr-vX.X.apk`).
2. Open it on your phone. If Android asks, allow **"Install unknown apps"** for your browser/files app.
3. Done. No signup.

> **Play Protect note:** sideloaded APKs can trigger a Play Protect warning even when clean. If you see one, check the checksum below matches the release before installing.

## Requirements

- Android 8.0 (API 26) or newer
- A working front or rear camera
- ~60 MB free space
- Internet only needed for the Moggr Coach chat tab (the scan works fully offline)

## Permissions

| Permission | Why |
|---|---|
| Camera | Capturing your three scan angles. Nothing else. |
| Internet | Only the Moggr Coach chat tab. The face scan never uses the network. |

## Limitations — be real with yourself

- **Ratings are photo-dependent estimates.** Lighting, pose, lens distortion, facial expression and camera angle all move the number. Same face, different photo, different score. Treat it as a directional read, not destiny.
- **Community ratios aren't science.** FWHR, ESR, "ideal" thirds — these come from forum consensus, not peer-reviewed research. Moggr reports them because the culture runs on them, but they're conventions, not medical truth.
- **The coach is a free-tier chatbot.** It can be slow or temporarily down, and it's guidance, not professional advice. It will never recommend surgery or diagnose anything — and you should never take medical advice from an app.
- **Moggr is not a medical device.** It's a self-improvement tool. If you're struggling with how you look, talking to a real person you trust beats any rating.

## Scores are estimates, worth isn't

A number from your camera doesn't decide your value, your dating life, or your future. Use it as a starting point for the stuff you can control — skin, hair, fitness, style, posture — and log off when it stops being useful.

## Verify the download

```
SHA-256: b494e6c5ae536817eadd92417246fdd495ef0b54500811b383afa385491d85df
```

Compare with: `sha256sum Moggr-v1.1.apk` (Linux) or any checksum tool on your phone/PC.

## Built with

- Kotlin + Jetpack Compose (Material 3)
- CameraX for capture
- Google ML Kit face detection (bundled model, on-device)
- OkHttp for the coach chat

## License

MIT — do what you want, just don't blame us for your PSL score.

---

*Moggr is an independent project. Not affiliated with looksmax.org or any forum. Maintained by [kurupdevs](https://github.com/kurupdevs).*
