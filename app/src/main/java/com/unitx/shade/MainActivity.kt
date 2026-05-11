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
                                    multiSelect(10)
                                    copyToCache{
                                        enabled = true
                                        onProgress = { progressConfig->
                                            progressConfig as ProgressConfig.Copying
                                            Log.i("Copying", progressConfig.percent.toString())
                                        }
                                    }
                                    onResult { multiple->
                                        multiple as ShadeResult.Multiple
                                        Toast.makeText(context, "Images selected: ${multiple.items}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }

                        LaunchedEffect(Unit) {
                            shade.launch(ShadeAction.Image.Gallery)
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