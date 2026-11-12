package com.aqualux.browser

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aqualux.browser.browser.BrowserViewModel
import com.aqualux.browser.ui.BrowserScreen

class MainActivity : ComponentActivity(), BrowserHost {
    private val viewModel: BrowserViewModel by viewModels()

    private lateinit var fullscreenContainer: FrameLayout
    private var fullscreenHidden: (() -> Unit)? = null

    private var permissionCallback: ((Map<String, Boolean>) -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val callback = permissionCallback
        permissionCallback = null
        callback?.invoke(result)
    }

    private var fileCallback: ((Array<Uri>?) -> Unit)? = null
    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        val uris = when {
            result.resultCode != RESULT_OK -> null
            result.data == null -> null
            result.data?.clipData != null -> {
                val clip = result.data!!.clipData!!
                Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
            }
            result.data?.data != null -> arrayOf(result.data!!.data!!)
            else -> null
        }
        callback?.invoke(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@MainActivity))
            setContent { BrowserScreen(viewModel) }
        }
        fullscreenContainer = FrameLayout(this).apply { visibility = View.GONE }
        val root = FrameLayout(this).apply {
            addView(composeView, FrameLayout.LayoutParams(MATCH, MATCH))
            addView(fullscreenContainer, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        setContentView(root)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.handleBack()) finish()
                }
            },
        )
        viewModel.attachHost(this)
        if (savedInstanceState == null) viewModel.consumeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.consumeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHostResume()
    }

    override fun onPause() {
        viewModel.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.detachHost(this)
        super.onDestroy()
    }

    override fun requestPermissions(permissions: Array<String>, onResult: (Map<String, Boolean>) -> Unit) {
        permissionCallback?.invoke(permissions.associateWith { false })
        permissionCallback = onResult
        permissionLauncher.launch(permissions)
    }

    override fun openFileChooser(intent: Intent, onResult: (Array<Uri>?) -> Unit) {
        fileCallback?.invoke(null)
        fileCallback = onResult
        try {
            fileLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            fileCallback = null
            onResult(null)
        }
    }

    override fun showFullscreen(view: View, onHidden: () -> Unit) {
        if (fullscreenContainer.childCount > 0) {
            onHidden()
            return
        }
        fullscreenHidden = onHidden
        fullscreenContainer.addView(
            view,
            FrameLayout.LayoutParams(MATCH, MATCH),
        )
        fullscreenContainer.visibility = View.VISIBLE
        insetsController().apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun hideFullscreen() {
        if (fullscreenContainer.visibility != View.VISIBLE && fullscreenHidden == null) return
        fullscreenContainer.removeAllViews()
        fullscreenContainer.visibility = View.GONE
        insetsController().show(WindowInsetsCompat.Type.systemBars())
        val callback = fullscreenHidden
        fullscreenHidden = null
        callback?.invoke()
    }

    override fun startActivitySafe(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override fun dispatchBack() {
        onBackPressedDispatcher.onBackPressed()
    }

    private fun insetsController(): WindowInsetsControllerCompat {
        return WindowCompat.getInsetsController(window, window.decorView)
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
