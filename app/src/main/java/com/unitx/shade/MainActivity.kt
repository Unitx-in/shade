package com.unitx.shade

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.unitx.shade.ui.theme.ShadeTheme
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.extend.ProgressConfig
import com.unitx.shade_core.common.result.ShadeResult
import com.unitx.shade_core.common.okHttp.toMultipartPart
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

                        val context = LocalContext.current

                        val shade = rememberShade {
                            image {
                                camera {
//                                    durationLimit = 5
//                                    saveToExternalStorage {
//                                        enabled = true
//                                        path = File(
//                                            Environment.getExternalStoragePublicDirectory(
//                                                Environment.DIRECTORY_PICTURES
//                                            ), "Shade"
//                                        )
//                                    }

                                    compress {
                                        enabled = true
                                        videoBitrate = 2_000_000
                                        frameRate = 30
                                        maxWidth = 720
                                        keyFrameInterval = 2
                                        onProgress = { progressConfig ->
                                            progressConfig as ProgressConfig.Compressing
                                            Log.i("Compressing", "progress: ${progressConfig.percent}, file number ${progressConfig.fileNumber}")
                                        }
                                    }
                                    onResult { captured ->
                                        val fileName = captured.file.name
                                        val ext = if (fileName.contains(".")) fileName.substringAfterLast('.').lowercase() else "no extension"
                                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)

                                        Log.d("SHADE_DEBUG", "File path: ${captured.file.absolutePath}")
                                        Log.d("SHADE_DEBUG", "File name: $fileName")
                                        Log.d("SHADE_DEBUG", "ext=$ext | mime=$mime")
                                        Log.d("SHADE_DEBUG", "File size: ${captured.file.length()}")
                                    }
                                    onFailure { error ->
                                        Log.i("CameraResultError", error.toString())
                                        Toast.makeText(context, "$error", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                gallery {
                                    multiSelect {
                                        enabled = true
                                        maxItems = 10
                                    }
                                    copyToCache {
                                        enabled = true
                                        onProgress = { progressConfig ->
                                            progressConfig as ProgressConfig.Copying
                                        }
                                    }
                                    onResult { multiple ->
                                        multiple as ShadeResult.Multiple
                                        Toast.makeText(context, "Videos selected: ${multiple.items.map { it.file?.absolutePath }}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            document {
                                copyToCache {
                                    enabled = true
                                    onProgress = {
                                        it as ProgressConfig.Copying
                                        Log.i("Document", "progress: ${it.percent}")
                                    }
                                }
                                onResult { result ->
                                    result as ShadeResult.Single
                                    Log.i("Document", "${result.file?.absolutePath}")
                                }

                                onFailure {
                                    Toast.makeText(context, "$it", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
//                            shade.launch(ShadeAction.Document(listOf(DocumentMimeType.PDF)))
                            shade.launch(ShadeAction.Image.Camera)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShadeTheme {
        Greeting("Android")
    }
}