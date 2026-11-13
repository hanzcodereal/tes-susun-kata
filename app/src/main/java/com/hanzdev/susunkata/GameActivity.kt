package com.hanzdev.susunkata

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

class GameActivity : Activity(), GameHost {
    private lateinit var state: GameState
    private lateinit var fx: Fx
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Settings.load(this)
        state = GameState(this)
        state.load()
        fx = Fx(this)
        view = GameView(this, state, fx, this)
        setContentView(view)
    }

    override fun exitToMenu() {
        finish()
    }

    override fun openSettings() {
        showSettings(this) {
            state.resetAll()
            view.reload()
        }
    }

    override fun onPause() {
        super.onPause()
        state.save()
    }

    override fun onDestroy() {
        super.onDestroy()
        fx.release()
    }
}
