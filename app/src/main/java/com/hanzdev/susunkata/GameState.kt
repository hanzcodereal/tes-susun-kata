package com.hanzdev.susunkata

import android.content.Context
import java.util.Calendar

enum class SubmitResult { NEW, BONUS, ALREADY, INVALID, TOO_SHORT }

/**
 * Model permainan (satu-satunya sumber kebenaran). Papan = himpunan sel yang sudah terbuka.
 * Tampilan hanya membaca data ini dan menganimasikan hasilnya.
 */
class GameState(private val ctx: Context) {
    companion object {
        const val SAVE_VERSION = 1
        const val START_COINS = 100
        const val COST_TARGET = 50
        const val COST_ONE = 25
        const val COST_MULTI = 100
        const val MULTI_COUNT = 5
        const val GIFT_COINS = 50
        const val BONUS_COINS = 5
        const val LEVEL_COINS = 15
    }

    private val prefs = ctx.getSharedPreferences("susunkata_save", Context.MODE_PRIVATE)

    var coins = START_COINS
        private set
    var levelIndex = 0
        private set
    var completed = false
        private set
    private var giftDay = -1

    private val _revealed = HashSet<Int>()
    val revealed: Set<Int> get() = _revealed
    private val _bonusFound = LinkedHashSet<String>()
    val bonusFound: Set<String> get() = _bonusFound

    lateinit var level: Level
        private set
    private var cellSet: Set<Int> = emptySet()

    /** Sel yang baru terbuka oleh submit() terakhir (untuk animasi). */
    var lastNew: List<Int> = emptyList()
        private set

    fun load() {
        coins = prefs.getInt("coins", START_COINS)
        giftDay = prefs.getInt("gift", -1)
        setLevel(prefs.getInt("level", 0).coerceAtLeast(0))
        prefs.getString("rev", "")!!.split(',').mapNotNull { it.toIntOrNull() }
            .filter { it in cellSet }.forEach { _revealed.add(it) }
        prefs.getString("bonus", "")!!.split('|')
            .filter { it.isNotEmpty() && it in level.bonus }.forEach { _bonusFound.add(it) }
        completed = level.words.all { isFound(it) }
    }

    fun save() {
        prefs.edit()
            .putInt("v", SAVE_VERSION)
            .putInt("coins", coins)
            .putInt("level", levelIndex)
            .putInt("gift", giftDay)
            .putString("rev", _revealed.joinToString(","))
            .putString("bonus", _bonusFound.joinToString("|"))
            .apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        coins = START_COINS
        giftDay = -1
        completed = false
        setLevel(0)
        save()
    }

    private fun setLevel(i: Int) {
        levelIndex = i
        level = LevelRepo.get(ctx, i)
        _revealed.clear()
        _bonusFound.clear()
        completed = false
        val s = HashSet<Int>()
        for (w in level.words) for (c in w.cells(level.cols)) s.add(c)
        cellSet = s
    }

    fun levelReward(): Int = LEVEL_COINS + level.words.size

    fun isFound(w: Word): Boolean = w.cells(level.cols).all { it in _revealed }

    fun hiddenCount(): Int = cellSet.count { it !in _revealed }

    fun isGridCell(c: Int): Boolean = c in cellSet

    fun submit(text: String): SubmitResult {
        lastNew = emptyList()
        if (completed) return SubmitResult.INVALID
        if (text.length < 2) return SubmitResult.TOO_SHORT
        val w = level.words.firstOrNull { it.text == text }
        if (w != null) {
            if (isFound(w)) return SubmitResult.ALREADY
            lastNew = w.cells(level.cols).filter { _revealed.add(it) }
            checkComplete()
            save()
            return SubmitResult.NEW
        }
        if (text in level.bonus) {
            if (!_bonusFound.add(text)) return SubmitResult.ALREADY
            coins += BONUS_COINS
            save()
            return SubmitResult.BONUS
        }
        return SubmitResult.INVALID
    }

    fun spend(cost: Int): Boolean {
        if (coins < cost) return false
        coins -= cost
        save()
        return true
    }

    /** Buka sel tertentu (petunjuk "target"). */
    fun revealCell(c: Int): Boolean {
        if (c !in cellSet || !_revealed.add(c)) return false
        lastNew = listOf(c)
        checkComplete()
        save()
        return true
    }

    /** Buka sel acak (petunjuk bohlam). */
    fun revealRandom(n: Int): List<Int> {
        val hidden = cellSet.filter { it !in _revealed }.shuffled()
        val pick = hidden.take(n)
        _revealed.addAll(pick)
        lastNew = pick
        checkComplete()
        save()
        return pick
    }

    private fun checkComplete() {
        if (!completed && level.words.all { isFound(it) }) {
            completed = true
            coins += levelReward()
        }
    }

    fun advance() {
        setLevel(levelIndex + 1)
        save()
    }

    private fun today(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun canClaimGift(): Boolean = giftDay != today()

    fun claimGift(): Boolean {
        if (!canClaimGift()) return false
        giftDay = today()
        coins += GIFT_COINS
        save()
        return true
    }
}
