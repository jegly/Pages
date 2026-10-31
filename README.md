<p align="center">
  <img src="screenshots/Pages_Banner.png" alt="Pages"/>
</p>

**A beautiful, privacy-first ebook reader for Android — PDF, EPUB, MOBI, TXT, CBZ & CBR**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-cba6f7.svg?logo=kotlin&logoColor=cdd6f4&labelColor=1e1e2e)](https://kotlinlang.org) [![Android](https://img.shields.io/badge/Android-14%2B-a6e3a1.svg?logo=android&logoColor=cdd6f4&labelColor=1e1e2e)](https://developer.android.com) [![Version](https://img.shields.io/badge/Version-1.0.0-89b4fa.svg?labelColor=1e1e2e)](https://github.com/jegly/pages/releases) [![License](https://img.shields.io/badge/License-Apache%202.0-fab387.svg?labelColor=1e1e2e)](LICENSE) [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-b4befe.svg?logo=jetpackcompose&logoColor=cdd6f4&labelColor=1e1e2e)](https://developer.android.com/jetpack/compose)

[![Download APK](https://img.shields.io/badge/Download_APK-94e2d5?style=for-the-badge&logo=android&logoColor=1e1e2e)](https://github.com/jegly/pages/releases/latest)

If this project helped you, please ⭐️ star it. **Also try [OfflineLLM](https://github.com/jegly/OfflineLLM)** and **[Box](https://github.com/jegly/Box)** — on-device AI apps built on the same privacy-first philosophy.

---

**📱 Screenshots**

<table>
  <tr>
    <td><img src="screenshots/01_library_list.png" width="320"/></td>
    <td><img src="screenshots/02_library_grid.png" width="320"/></td>
    <td><img src="screenshots/03_library_add_menu.png" width="320"/></td>
  </tr>
  <tr>
    <td><img src="screenshots/04_reader.png" width="320"/></td>
    <td><img src="screenshots/05_reader_tts.png" width="320"/></td>
    <td><img src="screenshots/06_settings_appearance.png" width="320"/></td>
  </tr>
  <tr>
    <td><img src="screenshots/07_settings_reading.png" width="320"/></td>
    <td><img src="screenshots/08_settings_security_about.png" width="320"/></td>
  </tr>
</table>

---

## Features

- **Six formats** — PDF, EPUB, MOBI, TXT, CBZ & CBR all read natively
- **Beautiful reader** — swipe or scroll page turn, pinch-to-zoom, fullscreen mode, progress slider with configurable timeout, go-to-page, on-screen nav buttons
- **PDF tools** — fit page / fill width / fill screen modes, margin crop, high contrast (grayscale)
- **Text customisation** — font size (70–250%), font family, line spacing, side margins, 6 reading backgrounds (System / Light / Dark / Sepia / Green / Night), brightness control, blue light filter
- **Neural TTS** — fully offline text-to-speech via Sherpa-ONNX with 4 bundled Piper voices (Lessac, Ryan, Alan, Amy); sentence-level highlight while reading; auto-advances pages; speed & pitch control
- **Table of Contents** — chapter navigation for EPUB
- **In-book search** — full-text search across EPUB, MOBI & TXT
- **Highlights & bookmarks** — highlight text in four colours, bookmark any page
- **Auto-scroll** — hands-free reading at adjustable speed
- **Foldable support** — dual-page spread and tabletop modes for foldable devices
- **E-ink mode** — white/black only, no animations, optimised for e-paper displays
- **Material You** — dynamic colour from your wallpaper, or choose from 6 accent colours
- **Collections** — organise your library into groups
- **Folder scan** — import an entire folder of books in one tap
- **No READ_EXTERNAL_STORAGE** — file access via Storage Access Framework only

## Security

- **Encrypted files** — imported books stored as AES-256-GCM encrypted copies in private app storage when biometric lock is enabled; originals can be safely deleted
- **Encrypted database** — SQLCipher AES-256 for all metadata, bookmarks and highlights
- **Biometric lock** — fingerprint or PIN required to open the app
- **PDF auth gate** — require biometric or PIN before opening any PDF
- **Inactivity lock** — auto-locks after 5 minutes of inactivity
- **Screenshot protection** — `FLAG_SECURE` blocks screen capture and app-switcher previews
- **`allowBackup=false`** — no cloud backup of your library or encrypted files
- **Privacy Sandbox blocked** — all ad/tracking services explicitly denied in the manifest
- **Auto-clear cache** — delete temporary render data on every app close

## Install

1. Download the APK from [Releases](https://github.com/jegly/pages/releases/latest)
2. **Settings → Apps → Install unknown apps** → allow your file manager
3. Open the APK and tap Install

Requires Android 14+.

## Release signing

Release APKs use hybrid signing (APK Signature Scheme v3.2): the original app key plus an EC P-384 key and an ML-DSA-87 post-quantum key. Android 17 and newer verify the hybrid block, where the ML-DSA-87 key is the signer; earlier Android versions verify the original key as before.

| File | SHA-256 |
| --- | --- |
| `Pages-1.0.3-PQC-signed.apk` | `4c0fa65aa4288642119419b614650fd82101699559e0a019233b7861e203cd42` |

Verify a download:

```bash
sha256sum Pages-1.0.3-PQC-signed.apk
apksigner verify -v --print-certs Pages-1.0.3-PQC-signed.apk   # build-tools 37.0.0 on JDK 25 shows all three keys
```

Signing certificate SHA-256:

- ML-DSA-87 (post-quantum): `5f99de5b3195753d4fe8ddabebd2fb27356b2109d6fbb2471fcbbdf21a9e76a6`
- EC P-384 (hybrid classical): `fa8f4a07e6a4f2b7dc6e5ab31fc05592b1414ab9436d1e2e39b35ce3bb2e33a2`
- Original key: `98d324d4106a368c62729a0a24d9ac9a6b47f8ac4c6585348531f0ee4eb6a04c`

---

**[www.jegly.xyz](https://www.jegly.xyz)**

[![Buy Me A Coffee](https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png)](https://www.buymeacoffee.com/jegly)

