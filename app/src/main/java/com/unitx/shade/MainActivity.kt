package com.unitx.shade

import android.os.Bundle
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
                                    onResult { captured->
                                        Toast.makeText(context, "Image captured: ${captured.file.absolutePath}", Toast.LENGTH_SHORT).show()
                                    }
                                    onFailure { error->

                                    }
                                }
                                gallery {

                                }
                            }
                        }

                        LaunchedEffect(Unit) {
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