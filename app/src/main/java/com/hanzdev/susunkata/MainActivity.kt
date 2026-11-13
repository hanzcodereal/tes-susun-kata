package com.hanzdev.susunkata

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var playButton: TextView
    private val tiles = ArrayList<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.load(this)

        val root = FrameLayout(this)
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.bg_1)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(View(this).apply { setBackgroundColor(0x55000000) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32f), dp(40f), dp(32f), dp(24f))
        }
        root.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // logo: ubin huruf
        val logo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        for (ch in "KATA") {
            val t = TextView(this).apply {
                text = ch.toString()
                setTextColor(Color.WHITE)
                textSize = 40f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                setShadowLayer(4f, 0f, 3f, 0x88000000.toInt())
                background = roundBg(COLOR_GREEN, dp(16f).toFloat(), Color.WHITE, dp(3f))
            }
            tiles.add(t)
            logo.addView(t, LinearLayout.LayoutParams(dp(68f), dp(68f)).apply { setMargins(dp(4f), 0, dp(4f), 0) })
        }
        col.addView(TextView(this).apply {
            text = "SUSUN"
            setTextColor(Color.WHITE)
            textSize = 46f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setShadowLayer(8f, 0f, 4f, 0xAA000000.toInt())
        }, lp(this, 70f))
        col.addView(logo, lp(this, 10f))
        col.addView(TextView(this).apply {
            text = "Game susun kata bahasa Indonesia"
            setTextColor(0xFFEFF7E9.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
        }, lp(this, 14f))

        col.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))

        playButton = gameButton(this, "MAIN", COLOR_GREEN) {
            startActivity(Intent(this, GameActivity::class.java))
        }.apply { textSize = 26f }
        col.addView(playButton, lp(this))
        col.addView(gameButton(this, "Pengaturan", Color.parseColor("#1E88E5")) {
            showSettings(this) {
                val st = GameState(this); st.resetAll(); refreshPlay()
            }
        }, lp(this, 12f))
        col.addView(gameButton(this, "Tentang & Dukung", Color.parseColor("#F57C00")) { showAbout(this) }, lp(this, 12f))
        col.addView(TextView(this).apply {
            text = "© Developer: $DEV_NAME"
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }, lp(this, 22f))

        setContentView(root)
    }

    private fun refreshPlay() {
        val st = GameState(this)
        st.load()
        playButton.text = if (st.levelIndex > 0) "LANJUT  •  Tingkat ${st.levelIndex + 1}" else "MAIN"
    }

    override fun onResume() {
        super.onResume()
        refreshPlay()
        tiles.forEachIndexed { i, t ->
            t.scaleX = 0f; t.scaleY = 0f
            ObjectAnimator.ofFloat(t, View.SCALE_X, 0f, 1f).apply { startDelay = i * 90L; duration = 380; interpolator = OvershootInterpolator(2.2f); start() }
            ObjectAnimator.ofFloat(t, View.SCALE_Y, 0f, 1f).apply { startDelay = i * 90L; duration = 380; interpolator = OvershootInterpolator(2.2f); start() }
        }
    }
}
