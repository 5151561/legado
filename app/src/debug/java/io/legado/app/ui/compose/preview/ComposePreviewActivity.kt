package io.legado.app.ui.compose.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import io.legado.app.ui.compose.theme.LegadoComposeTheme

class ComposePreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegadoComposeTheme {
                Surface {
                    ComposePreviewScreen()
                }
            }
        }
    }
}
