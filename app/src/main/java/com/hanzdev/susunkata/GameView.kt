package com.hanzdev.susunkata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

interface GameHost {
    fun exitToMenu()
    fun openSettings()
}

private val PI_F = Math.PI.toFloat()

private const val B_SETTINGS = 1
private const val B_BACK = 2
private const val B_TARGET = 3
private const val B_ONE = 4
private const val B_MULTI = 5
private const val B_GIFT = 6
private const val B_SHUFFLE = 7
private const val B_BONUS = 8

private class Btn(val id: Int, val hit: RectF, val cx: Float, val cy: Float, val r: Float)

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var life: Float, val maxLife: Float, val color: Int, val size: Float
)

/**
 * Seluruh layar permainan digambar di satu View: latar, papan kata, pratinjau, roda huruf,
 * tombol, partikel dan layar selesai level. Semua perubahan datang dari [GameState].
 */
class GameView(
    context: Context,
    private val state: GameState,
    private val fx: Fx,
    private val host: GameHost
) : View(context) {

    // ---- warna ----
    private val green = Color.parseColor("#2BA616")
    private val greenDark = Color.parseColor("#1B7A0E")
    private val red = Color.parseColor("#D32F2F")
    private val gold = Color.parseColor("#F9A825")
    private val blue = Color.parseColor("#1E88E5")

    private val bgIds = intArrayOf(
        R.drawable.bg_1, R.drawable.bg_2, R.drawable.bg_3,
        R.drawable.bg_4, R.drawable.bg_5, R.drawable.bg_6
    )
    private var bgBitmap: Bitmap? = null
    private var bgTheme = -1
    private val bgMatrix = Matrix()
    private var topShade: Paint? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    // ---- level & papan ----
    private var level: Level = state.level
    private var cellChar = CharArray(0)
    private var s = 1f
    private var cell = 0f
    private var gridX = 0f
    private var gridY = 0f

    // ---- roda ----
    private var letters = CharArray(0)
    private var slotOf = IntArray(0)
    private var prevSlot = IntArray(0)
    private var shuffleStart = -10000L
    private var wheelCx = 0f
    private var wheelCy = 0f
    private var wheelR = 0f
    private var ringR = 0f
    private var letterR = 0f
    private var previewY = 0f

    // ---- pilihan ----
    private val sel = ArrayList<Int>()
    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f

    // ---- pratinjau / pesan ----
    private var previewText = ""
    private var previewKind: SubmitResult? = null
    private var previewStart = -10000L
    private var msg = ""
    private var msgStart = -10000L

    // ---- animasi ----
    private val revealAnim = HashMap<Int, Long>()
    private var introStart = 0L
    private var coinPulse = -10000L
    private var shownCoins = state.coins.toFloat()
    private var completeStart = 0L
    private var lastFrame = 0L
    private val particles = ArrayList<Particle>()
    private var targetMode = false

    // ---- tombol ----
    private val buttons = ArrayList<Btn>()
    private var pressed: Btn? = null
    private val nextRect = RectF()
    private val coinRect = RectF()

    private val fm = FloatArray(2)
    private val icons = Icons()

    init {
        resetLevel()
    }

    fun reload() {
        resetLevel()
        invalidate()
    }

    // =====================================================================================
    // Level & tata letak
    // =====================================================================================

    private fun now() = SystemClock.uptimeMillis()

    private fun resetLevel() {
        level = state.level
        cellChar = CharArray(level.rows * level.cols)
        for (w in level.words) {
            val cells = w.cells(level.cols)
            for (k in cells.indices) cellChar[cells[k]] = w.text[k]
        }
        letters = level.letters.toCharArray()
        val n = letters.size
        slotOf = IntArray(n) { it }
        val rnd = Random(state.levelIndex * 7919L + 13)
        var guard = 0
        do {
            slotOf.shuffle(rnd)
            guard++
        } while (guard < 20 && slotOf.indices.all { slotOf[it] == it })
        prevSlot = slotOf.copyOf()
        shuffleStart = -10000L
        sel.clear()
        dragging = false
        targetMode = false
        previewStart = -10000L
        msgStart = -10000L
        revealAnim.clear()
        particles.clear()
        introStart = now()
        shownCoins = state.coins.toFloat()
        completeStart = 0L
        val theme = (state.levelIndex / 3) % bgIds.size
        if (theme != bgTheme) {
            bgTheme = theme
            bgBitmap = BitmapFactory.decodeResource(resources, bgIds[theme])
        }
        layoutAll()
        if (state.completed) completeStart = now()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutAll()
    }

    private fun layoutAll() {
        if (width == 0 || height == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        s = w / 720f

        wheelR = min(0.31f * w, h * 0.19f)
        wheelCx = w / 2f
        wheelCy = h - wheelR - 84f * s
        ringR = wheelR * 0.66f
        letterR = wheelR * 0.22f
        previewY = wheelCy - wheelR - 56f * s

        val top = 150f * s
        val bottom = previewY - 54f * s
        val availW = w - 56f * s
        val availH = bottom - top
        cell = min(min(availW / level.cols, availH / level.rows), 118f * s)
        val gw = cell * level.cols
        val gh = cell * level.rows
        gridX = (w - gw) / 2f
        gridY = top + (availH - gh) / 2f

        buttons.clear()
        val br = 34f * s
        fun circle(id: Int, cx: Float, cy: Float) =
            buttons.add(Btn(id, RectF(cx - br - 8 * s, cy - br - 8 * s, cx + br + 8 * s, cy + br + 8 * s), cx, cy, br))
        circle(B_SETTINGS, 52f * s, 58f * s)
        circle(B_BACK, 134f * s, 58f * s)
        val lx = 64f * s
        val rx = w - 64f * s
        val y1 = wheelCy - wheelR * 0.83f
        val y2 = wheelCy - wheelR * 0.2f
        val y3 = wheelCy + wheelR * 0.83f
        circle(B_TARGET, lx, y1)
        circle(B_MULTI, lx, y2)
        circle(B_ONE, rx, y1)
        circle(B_GIFT, rx, y2)
        circle(B_SHUFFLE, rx, y3)
        val bw = 150f * s
        val bh = 66f * s
        buttons.add(Btn(B_BONUS, RectF(14f * s, y3 - bh / 2, 14f * s + bw, y3 + bh / 2), 14f * s + bw / 2, y3, bh / 2))

        coinRect.set(w - 230f * s, 58f * s - 28f * s, w - 16f * s, 58f * s + 28f * s)
        val cw = 400f * s
        nextRect.set(w / 2 - cw / 2, h / 2 + 120f * s, w / 2 + cw / 2, h / 2 + 120f * s + 92f * s)

        topShade = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h * 0.3f, 0x88000000.toInt(), 0, Shader.TileMode.CLAMP)
        }
    }

    // =====================================================================================
    // Sentuhan
    // =====================================================================================

    private fun buttonAt(x: Float, y: Float): Btn? = buttons.firstOrNull { it.hit.contains(x, y) }

    private fun letterXY(i: Int, out: FloatArray) {
        val n = letters.size
        val a1 = slotAngle(prevSlot[i], n)
        val a2 = slotAngle(slotOf[i], n)
        var d = a2 - a1
        while (d > PI_F) d -= 2 * PI_F
        while (d < -PI_F) d += 2 * PI_F
        val t = shuffleT()
        val a = a1 + d * t
        val rr = ringR * (1f - 0.14f * sin(PI_F * t))
        out[0] = wheelCx + cos(a) * rr
        out[1] = wheelCy + sin(a) * rr
    }

    private fun slotAngle(slot: Int, n: Int) = -PI_F / 2f + 2f * PI_F * slot / n

    private fun shuffleT(): Float {
        val q = ((now() - shuffleStart) / 450f).coerceIn(0f, 1f)
        return q * q * (3f - 2f * q)
    }

    private fun letterAt(x: Float, y: Float, radius: Float): Int {
        var best = -1
        var bestD = radius
        for (i in letters.indices) {
            letterXY(i, fm)
            val d = hypot(x - fm[0], y - fm[1])
            if (d <= bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    private fun cellAt(x: Float, y: Float): Int {
        val c = ((x - gridX) / cell).toInt()
        val r = ((y - gridY) / cell).toInt()
        if (x < gridX || y < gridY || c !in 0 until level.cols || r !in 0 until level.rows) return -1
        val idx = r * level.cols + c
        return if (cellChar[idx] != '\u0000') idx else -1
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (state.completed) {
                    if (nextRect.contains(x, y) && now() - completeStart > 700) {
                        fx.klik()
                        state.advance()
                        resetLevel()
                        invalidate()
                    }
                    return true
                }
                val b = buttonAt(x, y)
                if (b != null) {
                    pressed = b
                    invalidate()
                    return true
                }
                if (targetMode) {
                    val c = cellAt(x, y)
                    if (c >= 0 && c !in state.revealed) doTarget(c) else targetMode = false
                    invalidate()
                    return true
                }
                val li = letterAt(x, y, letterR * 1.3f)
                if (li >= 0) {
                    sel.clear()
                    sel.add(li)
                    dragging = true
                    dragX = x
                    dragY = y
                    fx.tap(0)
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressed != null) {
                    if (buttonAt(x, y) !== pressed) pressed = null
                    invalidate()
                } else if (dragging) {
                    dragX = x
                    dragY = y
                    val li = letterAt(x, y, letterR * 1.0f)
                    if (li >= 0) {
                        if (sel.size >= 2 && li == sel[sel.size - 2]) {
                            sel.removeAt(sel.size - 1)
                            fx.tap(sel.size - 1)
                        } else if (!sel.contains(li)) {
                            sel.add(li)
                            fx.tap(sel.size - 1)
                            fx.vibrate(6)
                        }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val b = pressed
                pressed = null
                if (b != null && buttonAt(x, y) === b) onButton(b.id)
                if (dragging) endSelection()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = null
                dragging = false
                sel.clear()
                invalidate()
            }
        }
        return true
    }

    // =====================================================================================
    // Aksi permainan
    // =====================================================================================

    private fun selText(): String {
        val sb = StringBuilder()
        for (i in sel) sb.append(letters[i])
        return sb.toString()
    }

    private fun endSelection() {
        dragging = false
        val text = selText()
        sel.clear()
        val res = state.submit(text)
        if (res == SubmitResult.TOO_SHORT) return
        previewText = text
        previewKind = res
        previewStart = now()
        when (res) {
            SubmitResult.NEW -> {
                fx.ok()
                fx.vibrate(18)
                animateReveal(state.lastNew)
                afterChange(true)
            }
            SubmitResult.BONUS -> {
                fx.bonus()
                fx.vibrate(12)
                coinPulse = now()
                showMsg("Kata bonus! +${GameState.BONUS_COINS} koin")
                burst(width / 2f, previewY, 14, intArrayOf(blue, gold, Color.WHITE))
            }
            SubmitResult.ALREADY -> {
                fx.klik()
                showMsg("Sudah ditemukan")
            }
            SubmitResult.INVALID -> {
                fx.salah()
                fx.vibrate(30)
            }
            SubmitResult.TOO_SHORT -> {}
        }
    }

    private fun animateReveal(cells: List<Int>) {
        val t = now()
        for ((k, c) in cells.withIndex()) {
            revealAnim[c] = t + k * 45L
            val r = c / level.cols
            val col = c % level.cols
            burst(gridX + (col + 0.5f) * cell, gridY + (r + 0.5f) * cell, 7, intArrayOf(Color.WHITE, gold, Color.parseColor("#9CFF6A")))
        }
    }

    /** Dipanggil setelah papan berubah: memulai layar selesai bila level tuntas. */
    private fun afterChange(fromWord: Boolean) {
        coinPulse = if (state.coins.toFloat() != shownCoins) now() else coinPulse
        if (state.completed && completeStart == 0L) {
            completeStart = now() + 450L
            fx.menang()
            fx.vibrate(40)
            confetti()
        }
    }

    private fun onButton(id: Int) {
        when (id) {
            B_BACK -> {
                fx.klik()
                host.exitToMenu()
            }
            B_SETTINGS -> {
                fx.klik()
                host.openSettings()
            }
            B_SHUFFLE -> {
                if (sel.isEmpty()) {
                    fx.acak()
                    prevSlot = slotOf.copyOf()
                    var guard = 0
                    val before = slotOf.copyOf()
                    do {
                        slotOf.shuffle()
                        guard++
                    } while (guard < 20 && slotOf.contentEquals(before))
                    shuffleStart = now()
                }
            }
            B_TARGET -> {
                fx.klik()
                if (targetMode) {
                    targetMode = false
                } else if (state.hiddenCount() == 0) {
                    // tidak ada yang bisa dibuka
                } else if (state.coins < GameState.COST_TARGET) {
                    noCoins()
                } else {
                    targetMode = true
                    showMsg("Ketuk kotak kosong yang ingin dibuka")
                }
            }
            B_ONE -> useHint(1, GameState.COST_ONE)
            B_MULTI -> useHint(GameState.MULTI_COUNT, GameState.COST_MULTI)
            B_GIFT -> {
                if (state.claimGift()) {
                    fx.koin()
                    coinPulse = now()
                    showMsg("Hadiah harian +${GameState.GIFT_COINS} koin!")
                    burst(width - 64f * s, wheelCy - wheelR * 0.2f, 18, intArrayOf(gold, Color.WHITE))
                } else {
                    fx.klik()
                    showMsg("Hadiah berikutnya tersedia besok")
                }
            }
            B_BONUS -> {
                fx.klik()
                val found = state.bonusFound
                val txt = if (found.isEmpty()) "Belum ada kata bonus. Cari kata lain dari huruf ini!"
                else "Kata bonus: " + found.joinToString(", ")
                Toast.makeText(context, txt, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun noCoins() {
        fx.salah()
        coinPulse = now()
        showMsg("Koin tidak cukup")
    }

    private fun useHint(count: Int, cost: Int) {
        if (state.completed || state.hiddenCount() == 0) return
        if (!state.spend(cost)) {
            noCoins()
            return
        }
        fx.petunjuk()
        val cells = state.revealRandom(count)
        animateReveal(cells)
        afterChange(false)
    }

    private fun doTarget(c: Int) {
        targetMode = false
        if (!state.isGridCell(c) || c in state.revealed) return
        if (!state.spend(GameState.COST_TARGET)) {
            noCoins()
            return
        }
        fx.petunjuk()
        state.revealCell(c)
        animateReveal(state.lastNew)
        afterChange(false)
    }

    private fun showMsg(m: String) {
        msg = m
        msgStart = now()
    }

    private fun burst(x: Float, y: Float, n: Int, colors: IntArray) {
        for (i in 0 until n) {
            val a = Random.nextFloat() * 2f * PI_F
            val v = (140f + Random.nextFloat() * 300f) * s
            particles.add(
                Particle(x, y, cos(a) * v, sin(a) * v - 120f * s, 0.6f + Random.nextFloat() * 0.5f, 1.1f,
                    colors[Random.nextInt(colors.size)], (4f + Random.nextFloat() * 6f) * s)
            )
        }
    }

    private fun confetti() {
        val cols = intArrayOf(gold, green, blue, red, Color.WHITE, Color.parseColor("#E040FB"))
        for (i in 0 until 110) {
            val life = 2.2f + Random.nextFloat() * 1.4f
            particles.add(
                Particle(Random.nextFloat() * width, -20f * s - Random.nextFloat() * 300f * s,
                    (Random.nextFloat() - 0.5f) * 220f * s, (200f + Random.nextFloat() * 380f) * s,
                    life, life, cols[Random.nextInt(cols.size)], (6f + Random.nextFloat() * 8f) * s)
            )
        }
    }

    // =====================================================================================
    // Menggambar
    // =====================================================================================

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    private fun text(c: Canvas, t: String, x: Float, y: Float, size: Float, color: Int, shadow: Boolean = false) {
        tp.textSize = size
        tp.color = color
        if (shadow) tp.setShadowLayer(size * 0.07f, 0f, size * 0.07f, 0x99000000.toInt()) else tp.clearShadowLayer()
        val m = tp.fontMetrics
        c.drawText(t, x, y - (m.ascent + m.descent) / 2f, tp)
        tp.clearShadowLayer()
    }

    override fun onDraw(c: Canvas) {
        val t = now()
        val dt = if (lastFrame == 0L) 0.016f else ((t - lastFrame) / 1000f).coerceIn(0f, 0.05f)
        lastFrame = t

        drawBackground(c)
        drawTopBar(c, t)
        drawGrid(c, t)
        drawPreview(c, t)
        drawWheel(c)
        drawButtons(c, t)
        drawMessage(c, t)
        drawParticles(c, dt)
        if (state.completed && completeStart != 0L) drawComplete(c, t)

        revealAnim.entries.removeAll { t - it.value > 700L }
        if (animating(t)) postInvalidateOnAnimation() else lastFrame = 0L
    }

    private fun animating(t: Long): Boolean =
        particles.isNotEmpty() || t - shuffleStart < 500 || t - previewStart < 650 || t - msgStart < 1900 ||
            t - coinPulse < 500 || t - introStart < 1500 || revealAnim.isNotEmpty() || targetMode ||
            shownCoins != state.coins.toFloat() || (state.completed && t - completeStart < 2500)

    private fun drawBackground(c: Canvas) {
        val bmp = bgBitmap
        if (bmp != null) {
            val sc = max(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
            bgMatrix.reset()
            bgMatrix.postScale(sc, sc)
            bgMatrix.postTranslate((width - bmp.width * sc) / 2f, (height - bmp.height * sc) / 2f)
            p.reset()
            p.isAntiAlias = true
            p.isFilterBitmap = true
            c.drawBitmap(bmp, bgMatrix, p)
        } else {
            c.drawColor(Color.parseColor("#2E7D32"))
        }
        c.drawColor(0x22000000)
        topShade?.let { c.drawRect(0f, 0f, width.toFloat(), height * 0.3f, it) }
    }

    private fun drawCoin(c: Canvas, cx: Float, cy: Float, r: Float) {
        p.reset(); p.isAntiAlias = true
        p.color = Color.parseColor("#E08A00"); c.drawCircle(cx, cy, r, p)
        p.color = Color.parseColor("#FFC928"); c.drawCircle(cx, cy, r * 0.86f, p)
        p.color = Color.parseColor("#FFE27A"); c.drawCircle(cx - r * 0.18f, cy - r * 0.2f, r * 0.45f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.14f; p.color = Color.parseColor("#E08A00")
        c.drawCircle(cx, cy, r * 0.58f, p)
    }

    private fun ringBtn(c: Canvas, b: Btn) {
        val sc = if (pressed === b) 0.92f else 1f
        c.save()
        c.scale(sc, sc, b.cx, b.cy)
        p.reset(); p.isAntiAlias = true
        p.color = 0x44000000; c.drawCircle(b.cx, b.cy + 3 * s, b.r, p)
        p.color = 0x33000000; c.drawCircle(b.cx, b.cy, b.r, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f * s; p.color = Color.WHITE
        c.drawCircle(b.cx, b.cy, b.r, p)
    }

    private fun drawTopBar(c: Canvas, t: Long) {
        for (b in buttons) {
            if (b.id == B_SETTINGS) {
                ringBtn(c, b); icons.drawGear(c, b.cx, b.cy, b.r * 0.62f); c.restore()
            } else if (b.id == B_BACK) {
                ringBtn(c, b); icons.drawBackArrow(c, b.cx, b.cy, b.r * 0.62f); c.restore()
            }
        }
        text(c, "TINGKAT ${state.levelIndex + 1}", width / 2f + 40f * s, 58f * s, 40f * s, Color.WHITE, true)

        // koin
        if (shownCoins != state.coins.toFloat()) {
            val d = state.coins - shownCoins
            shownCoins = if (abs(d) < 1f) state.coins.toFloat() else shownCoins + d * 0.18f
        }
        val pulse = 1f + 0.12f * max(0f, sin(PI_F * ((t - coinPulse) / 450f).coerceIn(0f, 1f)))
        c.save()
        c.scale(pulse, pulse, coinRect.centerX(), coinRect.centerY())
        p.reset(); p.isAntiAlias = true
        p.color = 0x55000000
        c.drawRoundRect(coinRect, coinRect.height() / 2, coinRect.height() / 2, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f * s; p.color = Color.WHITE
        c.drawRoundRect(coinRect, coinRect.height() / 2, coinRect.height() / 2, p)
        drawCoin(c, coinRect.left + coinRect.height() / 2, coinRect.centerY(), coinRect.height() * 0.4f)
        text(c, shownCoins.toInt().toString(), coinRect.centerX() + 22f * s, coinRect.centerY(), 36f * s, Color.WHITE, true)
        c.restore()
    }

    private fun drawGrid(c: Canvas, t: Long) {
        val cols = level.cols
        val gap = 6f * s
        val rad = cell * 0.14f
        for (r in 0 until level.rows) {
            for (col in 0 until cols) {
                val idx = r * cols + col
                val ch = cellChar[idx]
                if (ch == '\u0000') continue
                val ccx = gridX + (col + 0.5f) * cell
                val ccy = gridY + (r + 0.5f) * cell
                // animasi masuk level
                val intro = ((t - introStart - idx * 30L) / 280f).coerceIn(0f, 1f)
                var sc = easeOutBack(intro)
                val st = revealAnim[idx]
                val isOpen = idx in state.revealed && (st == null || t >= st)
                if (isOpen && st != null) {
                    sc = easeOutBack(((t - st) / 340f).coerceIn(0f, 1f))
                }
                if (sc <= 0.01f) continue
                val half = (cell - gap) / 2f
                c.save()
                c.scale(sc, sc, ccx, ccy)
                rect.set(ccx - half, ccy - half, ccx + half, ccy + half)
                p.reset(); p.isAntiAlias = true
                p.color = 0x55000000
                rect.offset(0f, 4f * s); c.drawRoundRect(rect, rad, rad, p); rect.offset(0f, -4f * s)
                if (isOpen) {
                    p.color = green; c.drawRoundRect(rect, rad, rad, p)
                    p.color = 0x33FFFFFF
                    rect.set(ccx - half, ccy - half, ccx + half, ccy - half * 0.1f)
                    c.drawRoundRect(rect, rad, rad, p)
                    text(c, ch.toString(), ccx, ccy + 2 * s, cell * 0.62f, Color.WHITE, true)
                } else {
                    p.color = 0xCCEFF3F8.toInt(); c.drawRoundRect(rect, rad, rad, p)
                    if (targetMode) {
                        p.style = Paint.Style.STROKE
                        p.strokeWidth = 4f * s
                        p.color = Color.argb((140 + 100 * sin(t / 160f)).toInt().coerceIn(0, 255), 255, 200, 40)
                        c.drawRoundRect(rect, rad, rad, p)
                    }
                }
                c.restore()
            }
        }
    }

    private fun drawPreview(c: Canvas, t: Long) {
        var txt = ""
        var color = green
        var alpha = 255
        var shake = 0f
        var pop = 1f
        if (dragging && sel.isNotEmpty()) {
            txt = selText()
        } else if (t - previewStart < 520 && previewText.isNotEmpty()) {
            val k = (t - previewStart) / 520f
            txt = previewText
            when (previewKind) {
                SubmitResult.INVALID -> { color = red; shake = sin(k * 38f) * (1f - k) * 14f * s }
                SubmitResult.ALREADY -> { color = gold; shake = sin(k * 30f) * (1f - k) * 8f * s }
                SubmitResult.BONUS -> color = blue
                else -> color = green
            }
            if (k > 0.7f) alpha = (255 * (1f - (k - 0.7f) / 0.3f)).toInt().coerceIn(0, 255)
            if (previewKind == SubmitResult.NEW || previewKind == SubmitResult.BONUS) pop = 1f + 0.12f * sin(PI_F * min(1f, k * 2.5f))
        }
        if (txt.isEmpty()) return
        val n = txt.length
        val gapT = 6f * s
        val ts = min(64f * s, (width - 80f * s) / n - gapT)
        val total = n * (ts + gapT) - gapT
        var x = (width - total) / 2f + shake
        c.save()
        c.scale(pop, pop, width / 2f, previewY)
        for (i in 0 until n) {
            rect.set(x, previewY - ts / 2, x + ts, previewY + ts / 2)
            p.reset(); p.isAntiAlias = true
            p.color = Color.argb((alpha * 0.3f).toInt(), 0, 0, 0)
            rect.offset(0f, 3 * s); c.drawRoundRect(rect, ts * 0.16f, ts * 0.16f, p); rect.offset(0f, -3 * s)
            p.color = (color and 0x00FFFFFF) or (alpha shl 24)
            c.drawRoundRect(rect, ts * 0.16f, ts * 0.16f, p)
            text(c, txt[i].toString(), rect.centerX(), rect.centerY() + 1 * s, ts * 0.68f, Color.argb(alpha, 255, 255, 255))
            x += ts + gapT
        }
        c.restore()
    }

    private fun drawWheel(c: Canvas) {
        p.reset(); p.isAntiAlias = true
        p.color = 0x33000000; c.drawCircle(wheelCx, wheelCy + 5 * s, wheelR, p)
        p.color = 0x99FFFFFF.toInt(); c.drawCircle(wheelCx, wheelCy, wheelR, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f * s; p.color = 0xCCFFFFFF.toInt()
        c.drawCircle(wheelCx, wheelCy, wheelR, p)

        // garis penghubung
        if (sel.isNotEmpty()) {
            val path = Path()
            for ((k, i) in sel.withIndex()) {
                letterXY(i, fm)
                if (k == 0) path.moveTo(fm[0], fm[1]) else path.lineTo(fm[0], fm[1])
            }
            if (dragging) path.lineTo(dragX, dragY)
            p.reset(); p.isAntiAlias = true
            p.style = Paint.Style.STROKE
            p.strokeWidth = letterR * 0.5f
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.color = Color.argb(215, 43, 166, 22)
            c.drawPath(path, p)
        }
        for (i in letters.indices) {
            letterXY(i, fm)
            val on = sel.contains(i)
            if (on) {
                p.reset(); p.isAntiAlias = true
                p.color = green; c.drawCircle(fm[0], fm[1], letterR * 1.05f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f * s; p.color = Color.WHITE
                c.drawCircle(fm[0], fm[1], letterR * 1.05f, p)
            }
            text(c, letters[i].toString(), fm[0], fm[1] + 3 * s, letterR * 2.0f, if (on) Color.WHITE else Color.parseColor("#1B1B1B"), !on)
        }
    }

    private fun costPill(c: Canvas, cx: Float, cy: Float, label: String, canAfford: Boolean) {
        val w = 104f * s
        val h = 32f * s
        rect.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        p.reset(); p.isAntiAlias = true
        p.color = 0xEEFFFFFF.toInt()
        c.drawRoundRect(rect, h / 2, h / 2, p)
        drawCoin(c, rect.left + h * 0.5f, cy, h * 0.36f)
        text(c, label, rect.centerX() + 12f * s, cy, 24f * s, if (canAfford) Color.parseColor("#3A3A3A") else red)
    }

    private fun drawButtons(c: Canvas, t: Long) {
        for (b in buttons) {
            when (b.id) {
                B_TARGET -> {
                    ringBtn(c, b)
                    if (targetMode) {
                        p.reset(); p.isAntiAlias = true; p.color = 0x66F9A825; c.drawCircle(b.cx, b.cy, b.r, p)
                    }
                    icons.drawTarget(c, b.cx, b.cy, b.r * 0.62f); c.restore()
                    costPill(c, b.cx, b.cy + b.r + 26f * s, GameState.COST_TARGET.toString(), state.coins >= GameState.COST_TARGET)
                }
                B_MULTI -> {
                    ringBtn(c, b); icons.drawMultiBulb(c, b.cx, b.cy, b.r * 0.66f); c.restore()
                    costPill(c, b.cx, b.cy + b.r + 26f * s, GameState.COST_MULTI.toString(), state.coins >= GameState.COST_MULTI)
                }
                B_ONE -> {
                    ringBtn(c, b); icons.drawBulb(c, b.cx, b.cy, b.r * 0.62f); c.restore()
                    costPill(c, b.cx, b.cy + b.r + 26f * s, GameState.COST_ONE.toString(), state.coins >= GameState.COST_ONE)
                }
                B_GIFT -> {
                    ringBtn(c, b); icons.drawGift(c, b.cx, b.cy, b.r * 0.62f); c.restore()
                    if (state.canClaimGift()) {
                        costPill(c, b.cx, b.cy + b.r + 26f * s, "+${GameState.GIFT_COINS}", true)
                    } else {
                        rect.set(b.cx - 52f * s, b.cy + b.r + 10f * s, b.cx + 52f * s, b.cy + b.r + 42f * s)
                        p.reset(); p.isAntiAlias = true; p.color = 0xAAFFFFFF.toInt()
                        c.drawRoundRect(rect, 16f * s, 16f * s, p)
                        text(c, "Besok", rect.centerX(), rect.centerY(), 22f * s, Color.parseColor("#555555"))
                    }
                }
                B_SHUFFLE -> {
                    ringBtn(c, b); icons.drawShuffle(c, b.cx, b.cy, b.r * 0.62f); c.restore()
                }
                B_BONUS -> {
                    val sc = if (pressed === b) 0.94f else 1f
                    c.save(); c.scale(sc, sc, b.cx, b.cy)
                    p.reset(); p.isAntiAlias = true; p.color = 0x33000000
                    c.drawRoundRect(b.hit, b.r, b.r, p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = 4f * s; p.color = Color.WHITE
                    c.drawRoundRect(b.hit, b.r, b.r, p)
                    text(c, state.bonusFound.size.toString(), b.hit.left + 38f * s, b.cy, 38f * s, Color.WHITE, true)
                    text(c, "Kata", b.hit.left + 100f * s, b.cy - 14f * s, 22f * s, Color.WHITE, true)
                    text(c, "Bonus", b.hit.left + 100f * s, b.cy + 12f * s, 22f * s, Color.WHITE, true)
                    c.restore()
                }
            }
        }
    }

    private fun drawMessage(c: Canvas, t: Long) {
        val age = t - msgStart
        if (age > 1800 || msg.isEmpty()) return
        val a = when {
            age < 150 -> age / 150f
            age > 1400 -> 1f - (age - 1400) / 400f
            else -> 1f
        }.coerceIn(0f, 1f)
        tp.textSize = 28f * s
        val w = tp.measureText(msg) + 56f * s
        val h = 56f * s
        val cy = 112f * s
        rect.set((width - w) / 2f, cy - h / 2, (width + w) / 2f, cy + h / 2)
        p.reset(); p.isAntiAlias = true
        p.color = Color.argb((190 * a).toInt(), 20, 20, 20)
        c.drawRoundRect(rect, h / 2, h / 2, p)
        text(c, msg, width / 2f, cy, 28f * s, Color.argb((255 * a).toInt(), 255, 255, 255))
    }

    private fun drawParticles(c: Canvas, dt: Float) {
        if (particles.isEmpty()) return
        val it = particles.iterator()
        while (it.hasNext()) {
            val q = it.next()
            q.life -= dt
            if (q.life <= 0f || q.y > height + 40f) {
                it.remove()
                continue
            }
            q.vy += 900f * s * dt
            q.x += q.vx * dt
            q.y += q.vy * dt
            val a = (255 * min(1f, q.life / (q.maxLife * 0.4f))).toInt().coerceIn(0, 255)
            p.reset(); p.isAntiAlias = true
            p.color = (q.color and 0x00FFFFFF) or (a shl 24)
            c.drawCircle(q.x, q.y, q.size * 0.5f, p)
        }
    }

    private fun starPath(cx: Float, cy: Float, r: Float): Path {
        val path = Path()
        for (i in 0 until 10) {
            val ang = -PI_F / 2f + i * PI_F / 5f
            val rr = if (i % 2 == 0) r else r * 0.46f
            val x = cx + cos(ang) * rr
            val y = cy + sin(ang) * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun drawComplete(c: Canvas, t: Long) {
        val age = (t - completeStart).toFloat()
        if (age < 0f) return
        val dim = (age / 300f).coerceIn(0f, 1f)
        c.drawColor(Color.argb((150 * dim).toInt(), 0, 0, 0))
        val q = (age / 420f).coerceIn(0f, 1f)
        val sc = 0.6f + 0.4f * easeOutBack(q)
        val cx = width / 2f
        val cy = height / 2f
        val cw = 580f * s
        val ch = 560f * s
        c.save()
        c.scale(sc, sc, cx, cy)
        p.reset(); p.isAntiAlias = true
        p.color = 0x55000000
        rect.set(cx - cw / 2, cy - ch / 2 + 8 * s, cx + cw / 2, cy + ch / 2 + 8 * s)
        c.drawRoundRect(rect, 44f * s, 44f * s, p)
        p.color = 0xFFF6FFF1.toInt()
        rect.set(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2)
        c.drawRoundRect(rect, 44f * s, 44f * s, p)
        p.color = green
        rect.set(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy - ch / 2 + 130f * s)
        c.drawRoundRect(rect, 44f * s, 44f * s, p)
        c.drawRect(cx - cw / 2, cy - ch / 2 + 80f * s, cx + cw / 2, cy - ch / 2 + 130f * s, p)
        text(c, "HEBAT!", cx, cy - ch / 2 + 66f * s, 68f * s, Color.WHITE, true)

        for (k in 0 until 3) {
            val sq = ((age - 250f - k * 160f) / 360f).coerceIn(0f, 1f)
            val r = (k == 1).let { if (it) 64f else 50f } * s * easeOutBack(sq)
            val sx = cx + (k - 1) * 128f * s
            val sy = cy - ch / 2 + 220f * s - (if (k == 1) 14f * s else 0f)
            p.reset(); p.isAntiAlias = true
            p.color = Color.parseColor("#E08A00")
            c.drawPath(starPath(sx, sy + 4 * s, max(r, 0.1f)), p)
            p.color = Color.parseColor("#FFC928")
            c.drawPath(starPath(sx, sy, max(r, 0.1f)), p)
        }
        text(c, "Tingkat ${state.levelIndex + 1} selesai", cx, cy + 10f * s, 40f * s, Color.parseColor("#1B5E20"))
        drawCoin(c, cx - 70f * s, cy + 78f * s, 28f * s)
        text(c, "+${state.levelReward()}", cx + 22f * s, cy + 78f * s, 52f * s, Color.parseColor("#B26A00"))

        // tombol lanjut (di dalam kartu)
        rect.set(nextRect)
        p.reset(); p.isAntiAlias = true
        p.color = greenDark
        rect.offset(0f, 5 * s); c.drawRoundRect(rect, 46f * s, 46f * s, p); rect.offset(0f, -5 * s)
        p.color = green
        c.drawRoundRect(rect, 46f * s, 46f * s, p)
        text(c, "LANJUT", rect.centerX(), rect.centerY(), 46f * s, Color.WHITE, true)
        c.restore()
    }
}
