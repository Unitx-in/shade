# Shade

A lightweight Android media library for capturing and picking images, videos, and documents — with built-in compression, cache copying, and progress reporting.

Supports both **Compose** and **XML (Activity/Fragment)** setups.

---

## Why Shade?

| Feature                                    | Shade |
|--------------------------------------------|-------|
| Camera capture (image & video)             | ✅     |
| Gallery picking (single & multi)           | ✅     |
| Document picking (single & multi)          | ✅     |
| Built-in image compression                 | ✅     |
| Built-in video compression                 | ✅     |
| Copy to cache with stable `File` reference | ✅     |
| Per-file progress reporting                | ✅     |
| Automatic permission handling              | ✅     |
| Compose support                            | ✅     |
| XML / Activity / Fragment support          | ✅     |
| No boilerplate launcher registration       | ✅     |
| Type-safe DSL configuration                | ✅     |

## 📦 Installation

### Step 1: Add JitPack repository

Add it in your root `settings.gradle` or `build.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

### Step 2: Add the dependency

```gradle
dependencies {
	        implementation("com.github.Unitx-in:shade-core:v1.0.0-alpha01")
	}
```

### Step 3: Add FileProvider (required for camera capture)

Add to your `AndroidManifest.xml` inside application block:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/shade_file_paths" />
</provider>
```

Create `res/xml/shade_file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="shade_cache" path="." />
</paths>
```

---

## Setup

### Compose

```kotlin
val shade = rememberShade {
    image {
        camera { onResult { } }
        gallery { onResult { } }
    }
}
```

### Fragment

```kotlin
private val shade by lazy {
    Shade.with(fragment = this) {
        image {
            camera { onResult { } }
        }
    }
}
```

### Activity

```kotlin
// Must be a class-level property — NOT inside onCreate or by lazy
private val shade = Shade.with(activity = this) {
    image {
        camera { onResult { } }
    }
}
```

---

## Launching

Use `shade.launch(ShadeAction)` to trigger any configured flow:

```kotlin
shade.launch(ShadeAction.Image.Camera)
shade.launch(ShadeAction.Image.Gallery)
shade.launch(ShadeAction.Video.Camera)
shade.launch(ShadeAction.Video.Gallery)
shade.launch(ShadeAction.Document())
shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF)))
```

---

## Results

Every action delivers a result in `onResult { }`:

| Type                   | When                                              |
|------------------------|---------------------------------------------------|
| `ShadeResult.Captured` | Camera capture — `file` and `uri` always non-null |
| `ShadeResult.Single`   | Single gallery or document pick                   |
| `ShadeResult.Multiple` | Multi-select gallery or document pick             |

```kotlin
// Camera — always Captured
camera {
    onResult { result ->
        Log.d("Shade", result.file.absolutePath)
        Log.d("Shade", result.uri.toString())
    }
}

// Gallery single — always Single
gallery {
    onResult { result ->
        val single = result as ShadeResult.Single
        Log.d("Shade", single.uri.toString())
        Log.d("Shade", single.file?.absolutePath ?: "no file")
    }
}

// Gallery multi — always Multiple
gallery {
    multiSelect { enabled = true }
    onResult { result ->
        val multiple = result as ShadeResult.Multiple
        multiple.items.forEach { item ->
            Log.d("Shade", item.uri.toString())
        }
    }
}

// Document single — always Single
document {
    onResult { result ->
        val single = result as ShadeResult.Single
        Log.d("Shade", single.file?.absolutePath ?: single.uri.toString())
    }
}

// Document multi — always Multiple
document {
    multiSelect { enabled = true }
    onResult { result ->
        val multiple = result as ShadeResult.Multiple
        multiple.items.forEach { item ->
            Log.d("Shade", item.uri.toString())
        }
    }
}
```

> `file` is non-null on `Single` and `Multiple` items only when `copyToCache` or `compress` is enabled.

---

## Errors

Handle failures in `onFailure { }`:

```kotlin
onFailure { error ->
    when (error) {
        ShadeError.PermissionDenied            -> showRationale()
        ShadeError.PermissionPermanentlyDenied -> openAppSettings()
        ShadeError.PickCancelled               -> Unit
        ShadeError.CompressionFailed           -> showError("Compression failed")
        ShadeError.FileSaveFailed              -> showError("Could not save file")
        else                                   -> showError("Something went wrong")
    }
}
```

---

## Features

### Multi-select

```kotlin
gallery {
    multiSelect {
        enabled  = true
        maxItems = 5
    }
    onResult { result ->
        val multiple = result as ShadeResult.Multiple
        multiple.items.forEach { item ->
            Log.d("Shade", item.uri.toString())
        }
    }
}
```

> `maxItems` is enforced for image/video gallery. For document picking, the system picker does not support enforcing a maximum.

### Copy to Cache

Copies picked files to the app's cache directory and provides a stable `File` reference:

```kotlin
gallery {
    copyToCache {
        enabled    = true
        onProgress = { config ->
            config as ProgressConfig.Copying
            Log.d("Shade", "File ${config.fileNumber}: ${config.percent}%")
        }
    }
    onResult { result ->
        val single = result as ShadeResult.Single
        Log.d("Shade", single.file?.absolutePath ?: "no file")
    }
}
```

### Compression

#### Image

```kotlin
camera {
    compress {
        enabled   = true
        quality   = 80          // JPEG quality, 0–100
        maxWidth  = 1024        // preserves aspect ratio
        maxHeight = 1024
        format    = CompressFormat.JPEG
        onProgress = { config ->
            config as ProgressConfig.Compressing
            Log.d("Shade", "File ${config.fileNumber}: ${config.percent}%")
        }
    }
    onResult { result ->
        Log.d("Shade", result.file.absolutePath)
    }
}
```

#### Video

```kotlin
camera {
    compress {
        enabled          = true
        videoBitrate     = 2_000_000   // 2 Mbps
        frameRate        = 30
        maxWidth         = 720
        keyFrameInterval = 2
        onProgress = { config ->
            config as ProgressConfig.Compressing
            Log.d("Shade", "File ${config.fileNumber}: ${config.percent}%")
        }
    }
    onResult { result ->
        Log.d("Shade", result.file.absolutePath)
    }
}
```

> `compress` and `copyToCache` are mutually exclusive — when compression is enabled it takes precedence.

---

## Document Picking

MIME types are specified at launch, not in the config block:

```kotlin
document {
    copyToCache {
        enabled    = true
        onProgress = { config ->
            config as ProgressConfig.Copying
            Log.d("Shade", "${config.percent}%")
        }
    }
    multiSelect {
        enabled = true
    }
    onResult { result ->
        val multiple = result as ShadeResult.Multiple
        multiple.items.forEach { item ->
            Log.d("Shade", item.file?.absolutePath ?: item.uri.toString())
        }
    }
    onFailure { error -> }
}

// Launch with specific MIME types
shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF, DocumentMimeType.DOCX)))

// Or accept all supported types
shade.launch(ShadeAction.Document())
```

### Supported MIME types

| Constant                | Format             |
|-------------------------|--------------------|
| `DocumentMimeType.PDF`  | PDF                |
| `DocumentMimeType.DOC`  | Word 97–2003       |
| `DocumentMimeType.DOCX` | Word               |
| `DocumentMimeType.XLS`  | Excel 97–2003      |
| `DocumentMimeType.XLSX` | Excel              |
| `DocumentMimeType.PPT`  | PowerPoint 97–2003 |
| `DocumentMimeType.PPTX` | PowerPoint         |
| `DocumentMimeType.TXT`  | Plain text         |
| `DocumentMimeType.CSV`  | CSV                |
| `DocumentMimeType.RTF`  | Rich Text          |

---

## Full Example (Compose)

```kotlin
val shade = rememberShade {
    image {
        camera {
            compress {
                enabled   = true
                quality   = 80
                maxWidth  = 1024
                maxHeight = 1024
            }
            onResult { result ->
                Toast.makeText(context, result.file.absolutePath, Toast.LENGTH_SHORT).show()
            }
            onFailure { error ->
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        gallery {
            multiSelect {
                enabled  = true
                maxItems = 10
            }
            copyToCache {
                enabled    = true
                onProgress = { config ->
                    config as ProgressConfig.Copying
                    Log.i("Shade", "File ${config.fileNumber}: ${config.percent}%")
                }
            }
            onResult { result ->
                val multiple = result as ShadeResult.Multiple
                Toast.makeText(context, "${multiple.items.size} images selected", Toast.LENGTH_SHORT).show()
            }
            onFailure { error ->
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    video {
        gallery {
            copyToCache { enabled = true }
            onResult { result ->
                val single = result as ShadeResult.Single
                Log.d("Shade", single.uri.toString())
            }
            onFailure { }
        }
    }
    document {
        copyToCache { enabled = true }
        onResult { result ->
            val single = result as ShadeResult.Single
            Log.d("Shade", single.file?.absolutePath ?: single.uri.toString())
        }
        onFailure { }
    }
}

Row {
    Button(onClick = { shade.launch(ShadeAction.Image.Camera) })  { Text("Camera") }
    Button(onClick = { shade.launch(ShadeAction.Image.Gallery) }) { Text("Gallery") }
    Button(onClick = { shade.launch(ShadeAction.Video.Gallery) }) { Text("Video") }
    Button(onClick = { shade.launch(ShadeAction.Document()) })    { Text("Document") }
}
```

---

## Permissions

Shade requests permissions automatically. You do not need to declare or request them manually.

| Action                  | Permission               |
|-------------------------|--------------------------|
| Image/Video Camera      | `CAMERA`                 |
| Video Gallery (API 33+) | `READ_MEDIA_VIDEO`       |
| Image Gallery           | None (uses Photo Picker) |
| Document Picker         | None                     |

---

## Requirements

- **Min SDK:** 24
- **Compile SDK:** 35
- **Kotlin:** 1.9+

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Support

- Create an [Issue](https://github.com/Unitx-in/Slate/issues)
- Email: developer@unitx.in
- You can contact me on the above email directly, if you have any problem using the library.

## Show your support

Give a ⭐️ if this project helped you!

---

Made with ❤️ by [Navneet/Unitx] (https://github.com/navneetLawania)
