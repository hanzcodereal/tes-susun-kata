package com.hanzdev.susunkata

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

private val PI_F = Math.PI.toFloat()

/** Ikon tombol, digambar dengan kanvas (tanpa gambar eksternal). */
class Icons {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private fun stroke(width: Float): Paint {
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.STROKE
        p.strokeWidth = width
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = Color.WHITE
        return p
    }

    fun drawGear(c: Canvas, cx: Float, cy: Float, r: Float) {
        stroke(r * 0.3f)
        c.drawCircle(cx, cy, r * 0.62f, p)
        p.style = Paint.Style.FILL
        for (k in 0 until 8) {
            c.save()
            c.rotate(k * 45f, cx, cy)
            rect.set(cx - r * 0.16f, cy - r * 1.02f, cx + r * 0.16f, cy - r * 0.62f)
            c.drawRoundRect(rect, r * 0.06f, r * 0.06f, p)
            c.restore()
        }
    }

    fun drawBackArrow(c: Canvas, cx: Float, cy: Float, r: Float) {
        stroke(r * 0.24f)
        val path = Path()
        path.moveTo(cx + r * 0.7f, cy)
        path.lineTo(cx - r * 0.7f, cy)
        path.moveTo(cx - r * 0.05f, cy - r * 0.65f)
        path.lineTo(cx - r * 0.7f, cy)
        path.lineTo(cx - r * 0.05f, cy + r * 0.65f)
        c.drawPath(path, p)
    }

    fun drawTarget(c: Canvas, cx: Float, cy: Float, r: Float) {
        stroke(r * 0.2f)
        c.drawCircle(cx, cy, r * 0.62f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(cx, cy, r * 0.16f, p)
        p.style = Paint.Style.STROKE
        c.drawLine(cx - r * 1.05f, cy, cx - r * 0.35f, cy, p)
        c.drawLine(cx + r * 0.35f, cy, cx + r * 1.05f, cy, p)
        c.drawLine(cx, cy - r * 1.05f, cx, cy - r * 0.35f, p)
        c.drawLine(cx, cy + r * 0.35f, cx, cy + r * 1.05f, p)
    }

    fun drawBulb(c: Canvas, cx: Float, cy: Float, r: Float) {
        val hy = cy - r * 0.15f
        p.reset(); p.isAntiAlias = true
        p.color = Color.parseColor("#FFF59D")
        c.drawCircle(cx, hy, r * 0.5f, p)
        p.color = Color.WHITE
        rect.set(cx - r * 0.26f, hy + r * 0.38f, cx + r * 0.26f, hy + r * 0.8f)
        c.drawRoundRect(rect, r * 0.1f, r * 0.1f, p)
        stroke(r * 0.14f)
        for (ang in intArrayOf(-90, -50, -130, -10, -170)) {
            val a = Math.toRadians(ang.toDouble()).toFloat()
            c.drawLine(cx + cos(a) * r * 0.72f, hy + sin(a) * r * 0.72f, cx + cos(a) * r * 0.98f, hy + sin(a) * r * 0.98f, p)
        }
    }

    fun drawMultiBulb(c: Canvas, cx: Float, cy: Float, r: Float) {
        drawBulb(c, cx - r * 0.55f, cy + r * 0.25f, r * 0.62f)
        drawBulb(c, cx + r * 0.55f, cy + r * 0.25f, r * 0.62f)
        drawBulb(c, cx, cy - r * 0.2f, r * 0.72f)
    }

    fun drawGift(c: Canvas, cx: Float, cy: Float, r: Float) {
        p.reset(); p.isAntiAlias = true
        p.color = Color.parseColor("#FF5252")
        rect.set(cx - r * 0.78f, cy - r * 0.12f, cx + r * 0.78f, cy + r * 0.85f)
        c.drawRoundRect(rect, r * 0.12f, r * 0.12f, p)
        rect.set(cx - r * 0.9f, cy - r * 0.48f, cx + r * 0.9f, cy - r * 0.02f)
        c.drawRoundRect(rect, r * 0.12f, r * 0.12f, p)
        p.color = Color.WHITE
        c.drawRect(cx - r * 0.12f, cy - r * 0.48f, cx + r * 0.12f, cy + r * 0.85f, p)
        stroke(r * 0.16f)
        c.drawOval(cx - r * 0.5f, cy - r * 0.95f, cx, cy - r * 0.45f, p)
        c.drawOval(cx, cy - r * 0.95f, cx + r * 0.5f, cy - r * 0.45f, p)
    }

    private fun arrow(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, head: Float) {
        c.drawLine(x1, y1, x2, y2, p)
        val a = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()).toFloat()
        val l = a + PI_F * 0.8f
        val rr = a - PI_F * 0.8f
        c.drawLine(x2, y2, x2 + cos(l) * head, y2 + sin(l) * head, p)
        c.drawLine(x2, y2, x2 + cos(rr) * head, y2 + sin(rr) * head, p)
    }

    fun drawShuffle(c: Canvas, cx: Float, cy: Float, r: Float) {
        stroke(r * 0.2f)
        arrow(c, cx - r * 0.9f, cy - r * 0.5f, cx + r * 0.85f, cy + r * 0.5f, r * 0.5f)
        arrow(c, cx - r * 0.9f, cy + r * 0.5f, cx + r * 0.85f, cy - r * 0.5f, r * 0.5f)
    }
}
