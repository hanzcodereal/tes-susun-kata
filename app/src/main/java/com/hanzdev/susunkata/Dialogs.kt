package com.hanzdev.susunkata

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

const val DEV_NAME = "Hanz Dev Official"
const val DEV_EMAIL = "hanzzcodee@gmail.com"
const val DEV_SUPPORT_URL = "https://saweria.co/hanzreally"

val COLOR_GREEN: Int = Color.parseColor("#2BA616")
val COLOR_GREEN_DARK: Int = Color.parseColor("#1B7A0E")

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun roundBg(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

/** Tombol bulat bergaya game, dipakai di menu dan dialog. */
fun gameButton(ctx: Context, label: String, color: Int, onClick: () -> Unit): TextView =
    TextView(ctx).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        gravity = Gravity.CENTER
        setShadowLayer(3f, 0f, 2f, 0x66000000)
        background = roundBg(color, ctx.dp(28f).toFloat(), Color.argb(70, 0, 0, 0), ctx.dp(3f))
        setPadding(ctx.dp(20f), ctx.dp(14f), ctx.dp(20f), ctx.dp(14f))
        setOnClickListener { onClick() }
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }
            false
        }
    }

fun lp(ctx: Context, topDp: Float = 0f): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = ctx.dp(topDp)
    }

private fun cardDialog(ctx: Context, title: String, build: (LinearLayout, Dialog) -> Unit): Dialog {
    val d = Dialog(ctx)
    d.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val card = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(22f), ctx.dp(20f), ctx.dp(22f), ctx.dp(22f))
        background = roundBg(Color.parseColor("#F6FFF1"), ctx.dp(28f).toFloat(), COLOR_GREEN, ctx.dp(4f))
    }
    card.addView(TextView(ctx).apply {
        text = title
        gravity = Gravity.CENTER
        textSize = 24f
        setTextColor(COLOR_GREEN_DARK)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }, lp(ctx))
    build(card, d)
    d.setContentView(card)
    d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    return d
}

private fun Dialog.showWide(ctx: Context) {
    show()
    window?.setLayout((ctx.resources.displayMetrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
}

private fun label(ctx: Context, t: String, size: Float = 16f, color: Int = Color.parseColor("#2B2B2B")) =
    TextView(ctx).apply {
        text = t
        textSize = size
        setTextColor(color)
    }

private fun switchRow(ctx: Context, name: String, on: Boolean, onChange: (Boolean) -> Unit): LinearLayout =
    LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(label(ctx, name, 18f).apply { layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        addView(Switch(ctx).apply {
            isChecked = on
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
    }

fun showSettings(ctx: Context, onReset: () -> Unit) {
    cardDialog(ctx, "Pengaturan") { card, d ->
        card.addView(switchRow(ctx, "Suara", Settings.sound) { Settings.sound = it; Settings.save(ctx) }, lp(ctx, 18f))
        card.addView(switchRow(ctx, "Getar", Settings.vibrate) { Settings.vibrate = it; Settings.save(ctx) }, lp(ctx, 14f))
        card.addView(gameButton(ctx, "Tentang & Dukung Developer", Color.parseColor("#1E88E5")) {
            d.dismiss(); showAbout(ctx)
        }, lp(ctx, 20f))
        card.addView(gameButton(ctx, "Reset Progres", Color.parseColor("#D32F2F")) {
            AlertDialog.Builder(ctx)
                .setTitle("Reset progres?")
                .setMessage("Level dan koin akan kembali ke awal. Tindakan ini tidak bisa dibatalkan.")
                .setPositiveButton("Reset") { _, _ -> onReset(); d.dismiss() }
                .setNegativeButton("Batal", null)
                .show()
        }, lp(ctx, 10f))
        card.addView(gameButton(ctx, "Tutup", COLOR_GREEN) { d.dismiss() }, lp(ctx, 10f))
    }.showWide(ctx)
}

private fun openLink(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Tidak ada aplikasi untuk membuka ini", Toast.LENGTH_SHORT).show()
    }
}

fun showAbout(ctx: Context) {
    val version = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
    cardDialog(ctx, "Susun Kata") { card, d ->
        card.addView(label(ctx, "Versi $version", 14f, Color.parseColor("#6B6B6B")).apply { gravity = Gravity.CENTER }, lp(ctx, 2f))
        card.addView(label(ctx, "Geser huruf pada roda untuk menyusun kata dan mengisi kotak. Kata lain yang valid menjadi kata bonus dan memberi koin.", 15f), lp(ctx, 14f))
        card.addView(label(ctx, "Developer: $DEV_NAME", 17f).apply { setTypeface(typeface, Typeface.BOLD) }, lp(ctx, 16f))
        card.addView(label(ctx, "Kontak: $DEV_EMAIL", 15f), lp(ctx, 4f))
        card.addView(label(ctx, "Dukung: saweria.co/hanzreally", 15f), lp(ctx, 2f))
        card.addView(gameButton(ctx, "Dukung via Saweria", Color.parseColor("#F57C00")) {
            openLink(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(DEV_SUPPORT_URL)))
        }, lp(ctx, 18f))
        card.addView(gameButton(ctx, "Kirim Email", Color.parseColor("#1E88E5")) {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEV_EMAIL"))
            i.putExtra(Intent.EXTRA_SUBJECT, "Susun Kata - masukan")
            openLink(ctx, i)
        }, lp(ctx, 10f))
        card.addView(gameButton(ctx, "Tutup", COLOR_GREEN) { d.dismiss() }, lp(ctx, 10f))
    }.showWide(ctx)
}
