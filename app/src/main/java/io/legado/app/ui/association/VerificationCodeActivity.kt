package io.legado.app.ui.association

import android.os.Bundle
import androidx.compose.runtime.Composable
import io.legado.app.base.BaseComposeActivity
import io.legado.app.constant.SourceType
import io.legado.app.utils.showDialogFragment

/**
 * 验证码
 */
class VerificationCodeActivity : BaseComposeActivity() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            val sourceType = intent.getIntExtra("sourceType", SourceType.book)
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName, sourceType)
            )
        } ?: finish()
    }

    @Composable
    override fun Content() {
        // 纯弹窗容器，无需 UI
    }

}