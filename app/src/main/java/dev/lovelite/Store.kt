package dev.lovelite

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

data class Entry(val day: Long, val text: String)

/** Lưu trữ cục bộ: SharedPreferences cho thông tin chung, một file JSON nhỏ cho nhật ký. */
object Store {
    private fun sp(c: Context) = c.getSharedPreferences("love", Context.MODE_PRIVATE)

    fun names(c: Context): Pair<String, String> =
        Pair(sp(c).getString("a", "") ?: "", sp(c).getString("b", "") ?: "")

    fun start(c: Context): LocalDate? {
        val v = sp(c).getLong("start", Long.MIN_VALUE)
        return if (v == Long.MIN_VALUE) null else LocalDate.ofEpochDay(v)
    }

    fun saveInfo(c: Context, a: String, b: String, d: LocalDate) {
        sp(c).edit().putString("a", a).putString("b", b).putLong("start", d.toEpochDay()).apply()
        Widgets.refresh(c)
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("lovelite:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun hasPin(c: Context) = sp(c).getString("pin", null) != null
    fun checkPin(c: Context, pin: String) = sp(c).getString("pin", null) == hash(pin)
    fun setPin(c: Context, pin: String) {
        val e = sp(c).edit()
        if (pin.isEmpty()) e.remove("pin") else e.putString("pin", hash(pin))
        e.apply()
    }

    private fun file(c: Context) = File(c.filesDir, "diary.json")
    private fun parse(a: JSONArray): MutableList<Entry> =
        MutableList(a.length()) { val o = a.getJSONObject(it); Entry(o.getLong("d"), o.getString("t")) }

    private fun toJson(l: List<Entry>): JSONArray {
        val a = JSONArray()
        for (e in l) a.put(JSONObject().put("d", e.day).put("t", e.text))
        return a
    }

    fun diary(c: Context): MutableList<Entry> =
        try { parse(JSONArray(file(c).readText())) } catch (e: Exception) { mutableListOf() }

    fun saveDiary(c: Context, l: List<Entry>) = file(c).writeText(toJson(l).toString())

    fun export(c: Context): String {
        val (a, b) = names(c)
        return JSONObject().put("v", 1).put("a", a).put("b", b)
            .put("start", start(c)?.toEpochDay() ?: JSONObject.NULL)
            .put("diary", toJson(diary(c))).toString()
    }

    fun import(c: Context, json: String): Boolean = try {
        val o = JSONObject(json)
        saveDiary(c, parse(o.getJSONArray("diary")))
        if (!o.isNull("start")) {
            saveInfo(c, o.optString("a"), o.optString("b"), LocalDate.ofEpochDay(o.getLong("start")))
        }
        true
    } catch (e: Exception) { false }
}
