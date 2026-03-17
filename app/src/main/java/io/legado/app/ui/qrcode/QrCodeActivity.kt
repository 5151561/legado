package io.legado.app.ui.qrcode

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MenuItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.google.zxing.Result
import io.legado.app.R
import io.legado.app.base.BaseComposeActivity
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.QRCodeUtils
import io.legado.app.utils.readBytes

class QrCodeActivity : BaseComposeActivity(), ScanResultCallback {

    private val selectQrImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.readBytes(this)?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            onScanResultCallback(QRCodeUtils.parseCodeResult(bitmap))
        }
    }

    @Composable
    override fun Content() {
        Scaffold(
            topBar = {
                LegadoSmallAppBar(
                    title = stringResource(id = R.string.scan_qr_code),
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = {
                            selectQrImage.launch {
                                mode = HandleFileContract.IMAGE
                            }
                        }) {
                            Icon(Icons.Default.PhotoAlbum, contentDescription = "从相册选择")
                        }
                    }
                )
            }
        ) { padding ->
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = R.id.fl_content
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                update = {
                    val fTag = "qrCodeFragment"
                    if (supportFragmentManager.findFragmentByTag(fTag) == null) {
                        val qrCodeFragment = QrCodeFragment()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fl_content, qrCodeFragment, fTag)
                            .commit()
                    }
                }
            )
        }
    }

    override fun onScanResultCallback(result: Result?) {
        val intent = Intent()
        intent.putExtra("result", result?.text)
        setResult(RESULT_OK, intent)
        finish()
    }

}