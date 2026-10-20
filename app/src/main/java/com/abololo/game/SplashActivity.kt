package com.abololo.game

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView

/** Black screen showing res/drawable-nodpi/presplash.png for 3 seconds, then opens the game. */
class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        val img = ImageView(this)
        img.setImageResource(R.drawable.presplash)
        img.scaleType = ImageView.ScaleType.FIT_CENTER
        img.alpha = 0f
        root.addView(
            img,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)
        hideSystemBars()

        // fade in (0.6 s) ... hold ... fade out (0.4 s) => total 3 s
        img.animate().alpha(1f).setDuration(600).start()
        handler.postDelayed({ img.animate().alpha(0f).setDuration(380).start() }, 2600)
        handler.postDelayed({ openGame() }, 3000)
    }

    private fun openGame() {
        if (started || isFinishing) return
        started = true
        startActivity(Intent(this, GameActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
