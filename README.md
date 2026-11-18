# KeySafe — launcher icon

This package contains Android launcher icon resources generated from the supplied
`keysafe-icon.png` artwork.

## Files

Copy the contents of `app/src/main/res/` into the same `app/src/main/res/`
directory in your Android project. Merge folders if they already exist; do not
delete unrelated resources.

The package includes:
- Legacy launcher PNGs for mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi.
- Adaptive launcher icon XML for Android 8.0 (API 26) and newer.
- A foreground image and matching dark-purple adaptive background.
- Both `ic_launcher` and `ic_launcher_round` resource names.

## AndroidManifest.xml

Open `app/src/main/AndroidManifest.xml` and add these attributes to the
existing `<application>` element:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher_round"
```

Do not replace the whole manifest. Keep its existing activity declarations,
permissions, backup settings and other attributes.

## Verify

1. Sync Gradle.
2. Build and install the app.
3. Check the launcher icon on the device/emulator.
4. If the old icon remains, uninstall the development build or clear the
   launcher cache, then install again.

`play_store_icon_512.png` is an optional 512x512 image for store/listing use.
It is not automatically configured as the launcher icon.

The original artwork is used as supplied; no changes were made to the logo.
