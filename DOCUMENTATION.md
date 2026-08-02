# SpecScan — Application Documentation

## What it does

SpecScan is an Android app that lets you scan the printed label on a pair of
spectacles (frames) with your phone's camera, automatically reads the text on
that label using on-device OCR (optical character recognition), and saves it
as a new row in a CSV file — building up a running spreadsheet of every pair
you scan, in the order you scan them.

Typical use case: someone processing/cataloguing many pairs of glasses (e.g.
in a warehouse, optical shop, or donation-sorting center) who needs to log
the model/size/color info printed on each label without typing it by hand.

## Core features

| Feature | Description |
|---|---|
| Live camera scanning | Real-time camera preview so you can frame the label before capturing. |
| On-device OCR | Text recognition happens entirely on the phone (Google ML Kit) — no internet connection or API key required, and no images ever leave the device. |
| Editable result | Recognized text appears in an editable box before saving, so you can fix any misread characters. |
| Sequential CSV logging | Each saved scan becomes one row, numbered in order (1, 2, 3, …), with a timestamp. |
| Running counter | The screen shows how many entries have been saved so far. |
| Export/share | A "Share CSV" button lets you send the file to email, Google Drive, WhatsApp, etc., or save it elsewhere. |

## How a scan works, step by step

1. Open the app — it asks for camera permission the first time (required to take photos of labels).
2. Point the camera at the spectacles label so the printed text fills the frame.
3. Tap **Scan**. The app captures a photo and runs text recognition on it.
4. The recognized text appears in the editable box above the buttons.
5. Review it — correct anything OCR got wrong (small/curved label text can occasionally be misread).
6. Tap **Add to CSV**. The text is appended as the next row in the CSV file, and the on-screen counter updates.
7. Repeat for the next pair of glasses.
8. Anytime, tap **Share CSV** to export everything scanned so far.

## What gets stored, and where

Each scan produces one row with three columns:

| Column | Example | Notes |
|---|---|---|
| `SerialNo` | `1`, `2`, `3`, … | Sequential, auto-incremented, never reused. |
| `Timestamp` | `2026-08-01 14:32:07` | Date/time the entry was saved, from the phone's clock. |
| `ScannedText` | `MODEL: RB2140 SIZE: 54-18 COLOR: BLACK` | The (possibly edited) OCR result — whatever text was on the label, as one field. |

The file is named `spectacles_labels.csv` and lives in the app's private
storage folder on the device
(`Android/data/com.example.specscan/files/spectacles_labels.csv`). It persists
between app launches and keeps growing as you scan more labels — it is never
overwritten, only appended to. It does **not** sync anywhere automatically;
use **Share CSV** to get a copy off the phone.

## What it does NOT do (current limitations)

- **No structured field parsing.** It saves the label's raw text as one
  block, not split into separate Model/Size/Color columns. (Could be added —
  see "Possible enhancements" below.)
- **No cloud sync or backup.** Everything stays on the phone until you
  explicitly share/export it.
- **No duplicate detection.** Scanning the same label twice creates two rows.
- **No editing/deleting past rows from within the app.** The CSV can be
  edited in any spreadsheet app after exporting.
- **Latin-script OCR only** (current configuration). Labels in Chinese,
  Japanese, Korean, or Devanagari script would need a different ML Kit
  recognizer model swapped in.
- **Debug build only.** The current build isn't signed for the Play Store —
  it's meant for installing directly on your own device(s).

## Permissions used

- **Camera** — required to photograph the label for OCR. That's the only
  permission the app requests. It does not access contacts, location,
  internet, or other device data.

## Possible enhancements (not yet built)

If useful, these could be added later:
- Auto-parse the label text into separate columns (e.g. detect "MODEL:",
  "SIZE:", "COLOR:" patterns).
- Undo/delete the last saved entry from within the app.
- Save directly to a public "Downloads" folder instead of app-private storage.
- Barcode/QR scanning in addition to text, if labels include one.
- Auto-upload each row to Google Sheets instead of a local CSV.
