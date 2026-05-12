package com.unitx.shade

import android.os.Bundle
import android.util.Log
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
import com.unitx.shade_core.common.DocumentMimeType
import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.extend.ProgressConfig
import com.unitx.shade_core.common.result.ShadeResult
import com.unitx.shade_core.compose.rememberShade

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
                                    compress {
                                        enabled = true
                                        quality = 80
                                        maxWidth = 1024
                                        maxHeight = 1024
                                    }
                                    onResult { captured->
                                        Toast.makeText(context, "Image captured: ${captured.file.absolutePath}", Toast.LENGTH_SHORT).show()
                                    }
                                    onFailure { error->
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
                                    onResult { multiple->
                                        multiple as ShadeResult.Multiple
                                        Toast.makeText(context, "Images selected: ${multiple.items.map { it.file?.absolutePath }}", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                document {
//                                    multiSelect {
//                                        enabled = true
//                                        maxItems = 2
//                                    }
                                    copyToCache {
                                        enabled = true
                                        onProgress = {
                                            it as ProgressConfig.Copying
                                            Log.i("Document", "progress: ${it.percent}")
                                        }
                                    }
                                    onResult { result->
                                        result as ShadeResult.Multiple
                                        Log.i("Document", "${result.items.map { it.file?.absolutePath }}")
                                    }
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