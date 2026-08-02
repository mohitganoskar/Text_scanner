# SpecScan — Spectacle Label Scanner

Scans the text printed on a spectacles label using the phone's camera (on-device OCR,
no internet/API key needed) and appends it as a new row to a CSV file, one row per scan.

## How it works
- **CameraX** shows a live preview and captures a still photo of the label.
- **ML Kit Text Recognition** (on-device, bundled model — works offline) extracts the text.
- The extracted text appears in an editable box so you can fix any OCR mistakes before saving.
- **Add to CSV** appends a row `SerialNo, Timestamp, ScannedText` to
  `spectacles_labels.csv`, stored in the app's private external storage
  (`Android/data/com.example.specscan/files/spectacles_labels.csv`) — no storage
  permission needed.
- **Share CSV** opens the share sheet so you can send the file to email, Drive, etc.,
  or save it wherever you like.

## Get an installable APK without installing Android Studio (recommended)

This project includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that builds the APK in the cloud automatically every time you push code.

1. Create a new **GitHub repository** (public or private — private works fine, it's free).
2. Upload the contents of this `SpecScan` folder to it. Easiest way, in a terminal:
   ```
   cd SpecScan
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
   (No terminal handy? GitHub's web UI also lets you drag-and-drop the whole folder
   via "Add file > Upload files" on the repo page.)
3. On GitHub, go to the **Actions** tab of your repo. You'll see a "Build APK" run
   start automatically (takes ~2–3 minutes).
4. When it finishes, click into the run, scroll to **Artifacts**, and download
   **SpecScan-debug-apk** — it's a zip containing `app-debug.apk`.
5. Transfer that `.apk` to your phone (email it to yourself, upload to Drive, use a
   USB cable — any way you like) and tap it to install. You'll need to allow
   "install unknown apps" for whichever app you used to open it (Android will
   prompt you the first time).

That's it — no Android Studio, no SDK setup on your end. Every time you push a
change to the repo, a fresh APK is built automatically.

## Opening the project
1. Open **Android Studio** (Hedgehog or newer recommended).
2. `File > Open` and select the `SpecScan` folder.
3. Let Gradle sync — Android Studio will download the wrapper jar automatically the
   first time (an internet connection is needed for this one-time sync).
4. Connect a device or start an emulator (a real device with a camera works best for
   scanning labels).
5. Click **Run**.

## Usage
1. Grant the camera permission when prompted.
2. Point the camera at a spectacles label so the text fills the frame.
3. Tap **Scan**.
4. Review/edit the recognized text in the box.
5. Tap **Add to CSV** — it's saved as the next sequential row.
6. Repeat for each pair of spectacles.
7. Tap **Share CSV** anytime to export what you've collected so far.

## Customizing
- **CSV columns**: edit `CsvStore.kt` — the `appendEntry` function builds each row.
- **Where the file is saved**: `CsvStore.kt` constructor (currently app-private
  external storage; you can switch to `MediaStore` to save into the public
  `Downloads` folder if you want it visible in a file manager without sharing).
- **Recognized-language script**: the app uses `TextRecognizerOptions.DEFAULT_OPTIONS`
  (Latin script). ML Kit also has separate recognizers for Chinese, Devanagari,
  Japanese, and Korean if labels use those scripts.

## Project structure
```
app/src/main/java/com/example/specscan/
  MainActivity.kt   — camera, OCR trigger, UI wiring
  CsvStore.kt        — sequential CSV append logic
app/src/main/res/layout/activity_main.xml — camera preview + result card UI
app/src/main/AndroidManifest.xml          — camera permission, FileProvider
```
