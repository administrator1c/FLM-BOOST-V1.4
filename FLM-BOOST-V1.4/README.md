# FLM BOOST V1.4

Android 10+ (API 29), Java, Shizuku 13.1.5.

## Build APK with GitHub Actions
1. Upload the **contents** of this folder to the root of a GitHub repository (including `.github/workflows/build.yml`).
2. Open Actions > Build FLM BOOST APK > Run workflow.
3. Download `FLM-BOOST-V1.4-APK` from the workflow's Artifacts section.

## Usage
1. Install Shizuku and start its service (wireless debugging or ADB).
2. Open FLM BOOST, grant Shizuku permission.
3. Choose only apps whose work you have saved and are safe to stop.
4. Tap BOOST + MỞ FL STUDIO.

## Limits
No root overclocking, no guaranteed 60 FPS, no permanent background optimization. Android may restart background apps. `am force-stop` can terminate ongoing work and notifications. On Android 11+ package visibility permissions may be restricted for Play Store distribution. The app is a prototype and has not been compiled/tested in this environment.
