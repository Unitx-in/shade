package com.unitx.shade

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unitx.shade.ui.theme.ShadeTheme
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.extend.ProgressConfig
import com.unitx.shade_core.common.result.ShadeResult
import com.unitx.shade_core.compose.rememberShade
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ShadeTestScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun ShadeTestScreen() {
    val context = LocalContext.current
    var previewUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var lastResultText by remember { mutableStateOf("No result yet") }

    val shade = rememberShade {
        image {
            camera {
                compress {
                    enabled = true
                    quality = 80
                    maxWidth = 1024
                    maxHeight = 1024
                    onProgress = { progress ->
                        progress as ProgressConfig.Compressing
                        Log.i("ImageCamera", "compress ${progress.percent}% file#${progress.fileNumber}")
                    }
                }
                saveToExternalStorage {
                    enabled = true
                    path = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "Shade"
                    )
                }
                onResult { captured ->
                    val fileName = captured.file.name
                    val ext = fileName.substringAfterLast('.', "no extension").lowercase()
                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                    Log.d("ImageCamera", "path=${captured.file.absolutePath} mime=$mime size=${captured.file.length()}")
                    previewUri = captured.uri
                    lastResultText = "Image camera: ${captured.file.name}"
                }
                onFailure { error ->
                    Log.e("ImageCamera", error.toString())
                    Toast.makeText(context, "$error", Toast.LENGTH_SHORT).show()
                    lastResultText = "Image camera failed: $error"
                }
            }

            gallery {
                multiSelect {
                    enabled = true
                    maxItems = 5
                }
                copyToCache {
                    enabled = true
                    onProgress = { progress ->
                        progress as ProgressConfig.Copying
                        Log.i("ImageGallery", "copy ${progress.percent}%")
                    }
                }
                onResult { result ->
                    when (result) {
                        is ShadeResult.Single -> {
                            previewUri = result.uri
                            lastResultText = "Image gallery single: ${result.file?.name}"
                        }
                        is ShadeResult.Multiple -> {
                            lastResultText = "Image gallery multiple: ${result.items.size} items"
                        }

                        is ShadeResult.Captured -> "Image captured ${result.file.absolutePath}"
                    }
                }
                onFailure { error ->
                    Log.e("ImageGallery", error.toString())
                    lastResultText = "Image gallery failed: $error"
                }
            }
        }

        video {
            camera {
                durationLimit = 30
                compress {
                    enabled = true
                    videoBitrate = 1_500_000
                    frameRate = 30
                }
                onResult { captured ->
                    Log.d("VideoCamera", "path=${captured.file.absolutePath}")
                    lastResultText = "Video camera: ${captured.file.name}"
                }
                onFailure { error ->
                    Log.e("VideoCamera", error.toString())
                    lastResultText = "Video camera failed: $error"
                }
            }

            gallery {
                onResult { result ->
                    lastResultText = "Video gallery result: $result"
                }
                onFailure { error ->
                    lastResultText = "Video gallery failed: $error"
                }
            }
        }

        document {
            copyToCache {
                enabled = true
                onProgress = { progress ->
                    progress as ProgressConfig.Copying
                    Log.i("Document", "copy ${progress.percent}%")
                }
            }
            onResult { result ->
                result as ShadeResult.Single
                Log.i("Document", "${result.file?.absolutePath}")
                lastResultText = "Document: ${result.file?.name}"
            }
            onFailure { error ->
                Toast.makeText(context, "$error", Toast.LENGTH_SHORT).show()
                lastResultText = "Document failed: $error"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { shade.launch(ShadeAction.Image.Camera) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Image Camera") }

        Button(
            onClick = { shade.launch(ShadeAction.Image.Gallery) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Image Gallery") }

        Button(
            onClick = { shade.launch(ShadeAction.Video.Camera) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Video Camera") }

        Button(
            onClick = { shade.launch(ShadeAction.Video.Gallery) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Video Gallery") }

        Button(
            onClick = { shade.launch(ShadeAction.Document(DocumentMimeType.ALL_ENTRY_LIST)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Document Picker") }

        Text(text = lastResultText)

        previewUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}