package com.hanzdev.susunkata

import android.content.Context
import org.json.JSONObject

/** Satu kata di papan: posisi awal, arah, dan daftar sel (baris * kolom + kolom). */
class Word(val text: String, val row: Int, val col: Int, val vertical: Boolean) {
    fun cells(cols: Int): IntArray = IntArray(text.length) { k ->
        if (vertical) (row + k) * cols + col else row * cols + col + k
    }
}

class Level(
    val letters: String,
    val rows: Int,
    val cols: Int,
    val words: List<Word>,
    val bonus: Set<String>
)

/** Membaca assets/levels.json (dibuat dan diverifikasi oleh tools/generate_levels.py). */
object LevelRepo {
    private var cache: List<Level>? = null

    private fun all(ctx: Context): List<Level> {
        cache?.let { return it }
        val text = ctx.applicationContext.assets.open("levels.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("levels")
        val list = ArrayList<Level>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ws = o.getJSONArray("words")
            val words = ArrayList<Word>(ws.length())
            for (j in 0 until ws.length()) {
                val w = ws.getJSONArray(j)
                words.add(Word(w.getString(0), w.getInt(1), w.getInt(2), w.getInt(3) == 1))
            }
            val bonus = HashSet<String>()
            val bs = o.getJSONArray("bonus")
            for (j in 0 until bs.length()) bonus.add(bs.getString(j))
            list.add(Level(o.getString("letters"), o.getInt("rows"), o.getInt("cols"), words, bonus))
        }
        cache = list
        return list
    }

    fun count(ctx: Context): Int = all(ctx).size

    /** Indeks di luar jumlah level berputar kembali ke awal. */
    fun get(ctx: Context, index: Int): Level {
        val l = all(ctx)
        return l[((index % l.size) + l.size) % l.size]
    }
}
