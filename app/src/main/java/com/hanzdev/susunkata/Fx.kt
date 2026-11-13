package com.hanzdev.susunkata

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.random.Random

/** Pengaturan pemain (suara dan getar). */
object Settings {
    var sound = true
    var vibrate = true

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences("susunkata_settings", Context.MODE_PRIVATE)
        sound = p.getBoolean("sound", true)
        vibrate = p.getBoolean("vibrate", true)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("susunkata_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("sound", sound).putBoolean("vibrate", vibrate).apply()
    }
}

/** Efek suara (SoundPool, variasi nada kecil agar tidak monoton) dan getar. */
class Fx(ctx: Context) {
    private val pool: SoundPool
    private val tapIds = IntArray(8)
    private val okId: Int
    private val bonusId: Int
    private val salahId: Int
    private val menangId: Int
    private val koinId: Int
    private val petunjukId: Int
    private val klikId: Int
    private val acakId: Int
    private val vib: Vibrator? = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        val taps = intArrayOf(
            R.raw.snd_tap0, R.raw.snd_tap1, R.raw.snd_tap2, R.raw.snd_tap3,
            R.raw.snd_tap4, R.raw.snd_tap5, R.raw.snd_tap6, R.raw.snd_tap7
        )
        for (i in taps.indices) tapIds[i] = pool.load(ctx, taps[i], 1)
        okId = pool.load(ctx, R.raw.snd_ok, 1)
        bonusId = pool.load(ctx, R.raw.snd_bonus, 1)
        salahId = pool.load(ctx, R.raw.snd_salah, 1)
        menangId = pool.load(ctx, R.raw.snd_menang, 1)
        koinId = pool.load(ctx, R.raw.snd_koin, 1)
        petunjukId = pool.load(ctx, R.raw.snd_petunjuk, 1)
        klikId = pool.load(ctx, R.raw.snd_klik, 1)
        acakId = pool.load(ctx, R.raw.snd_acak, 1)
    }

    private fun play(id: Int, vol: Float = 1f, rate: Float = 1f) {
        if (!Settings.sound || id == 0) return
        pool.play(id, vol, vol, 1, 0, rate)
    }

    private fun vary() = 0.97f + Random.nextFloat() * 0.06f

    fun tap(i: Int) = play(tapIds[i.coerceIn(0, tapIds.size - 1)], 0.8f)
    fun ok() = play(okId, 1f, vary())
    fun bonus() = play(bonusId)
    fun salah() = play(salahId, 0.8f)
    fun menang() = play(menangId)
    fun koin() = play(koinId)
    fun petunjuk() = play(petunjukId)
    fun klik() = play(klikId, 0.7f)
    fun acak() = play(acakId)

    fun vibrate(ms: Long) {
        if (!Settings.vibrate) return
        val v = vib ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    fun release() {
        pool.release()
    }
}
