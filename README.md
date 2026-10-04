# Planet Compass 🌌

An Android/Kotlin app that displays the approximate live positions of the major planets on a compass-style sky view.

## Features

- Live phone compass using the rotation-vector sensor.
- GPS location for the observer.
- Current-time planetary calculations.
- Azimuth and altitude for Mercury, Venus, Mars, Jupiter, Saturn, Uranus and Neptune.
- Works without a separate astronomy API.
- Kotlin + Jetpack Compose.
- Designed as a simple foundation for GitHub / Android Studio.

## Accuracy

The included planetary engine uses simplified orbital elements. It is suitable for general sky orientation and educational use, **not precision telescope pointing**. For higher accuracy, replace `PlanetCalculator` with a precision astronomy library.

## Build

1. Open this repository in Android Studio.
2. Let Gradle sync.
3. Connect an Android phone or start an emulator.
4. Build and run.
5. Grant location permission and calibrate the phone compass if needed.

## GitHub

```bash
git init
git add .
git commit -m "Initial Planet Compass app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/PlanetCompass.git
git push -u origin main
```

## License

MIT License.
