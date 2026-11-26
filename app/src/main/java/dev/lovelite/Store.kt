package dev.lovelite

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

data class Entry(val day: Long, val text: String)

/** gender: 0 = chưa chọn, 1 = nam, 2 = nữ, 3 = khác. */
data class Person(val name: String, val gender: Int, val birth: LocalDate?)

/** Lưu trữ cục bộ: SharedPreferences cho thông tin chung, một file JSON nhỏ cho nhật ký. */
object Store {
    private const val NONE = Long.MIN_VALUE
    private fun sp(c: Context) = c.getSharedPreferences("love", Context.MODE_PRIVATE)

    fun lang(c: Context): String = sp(c).getString("lang", "vi") ?: "vi"
    fun setLang(c: Context, l: String) { sp(c).edit().putString("lang", l).apply() }

    fun widgetPhoto(c: Context) = sp(c).getBoolean("wphoto", true)
    fun setWidgetPhoto(c: Context, v: Boolean) { sp(c).edit().putBoolean("wphoto", v).apply() }

    fun avatarMissing(c: Context, i: Int) = !Photo.avatarFile(c, i).exists()

    fun person(c: Context, i: Int): Person {
        val s = sp(c)
        val k = if (i == 0) "a" else "b"
        val b = s.getLong(if (i == 0) "ba" else "bb", NONE)
        return Person(
            s.getString(k, "") ?: "",
            s.getInt(if (i == 0) "ga" else "gb", 0),
            if (b == NONE) null else LocalDate.ofEpochDay(b),
        )
    }

    fun start(c: Context): LocalDate? {
        val v = sp(c).getLong("start", NONE)
        return if (v == NONE) null else LocalDate.ofEpochDay(v)
    }

    fun saveInfo(c: Context, p0: Person, p1: Person, d: LocalDate?) {
        val e = sp(c).edit()
        e.putString("a", p0.name).putString("b", p1.name).putInt("ga", p0.gender).putInt("gb", p1.gender)
        e.putLong("ba", p0.birth?.toEpochDay() ?: NONE).putLong("bb", p1.birth?.toEpochDay() ?: NONE)
        e.putLong("start", d?.toEpochDay() ?: NONE)
        e.apply()
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

    private fun b64(f: File): Any =
        if (f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else JSONObject.NULL

    fun export(c: Context): String {
        val p0 = person(c, 0)
        val p1 = person(c, 1)
        fun pj(p: Person) = JSONObject().put("name", p.name).put("gender", p.gender)
            .put("birth", p.birth?.toEpochDay() ?: JSONObject.NULL)
        return JSONObject().put("v", 2)
            .put("a", p0.name).put("b", p1.name)
            .put("p0", pj(p0)).put("p1", pj(p1))
            .put("start", start(c)?.toEpochDay() ?: JSONObject.NULL)
            .put("diary", toJson(diary(c)))
            .put("img", JSONObject().put("bg", b64(Photo.bgFile(c)))
                .put("a", b64(Photo.avatarFile(c, 0))).put("b", b64(Photo.avatarFile(c, 1))))
            .toString()
    }

    private fun restoreImg(img: JSONObject?, key: String, f: File) {
        if (img == null || img.isNull(key)) return
        f.writeBytes(Base64.decode(img.getString(key), Base64.NO_WRAP))
    }

    fun import(c: Context, json: String): Boolean = try {
        val o = JSONObject(json)
        saveDiary(c, parse(o.getJSONArray("diary")))
        fun pp(key: String, i: Int): Person {
            val j = o.optJSONObject(key)
            val old = person(c, i)
            val nm = j?.optString("name") ?: o.optString(if (i == 0) "a" else "b", old.name)
            val g = j?.optInt("gender", old.gender) ?: old.gender
            val b = if (j == null || !j.has("birth")) old.birth
                else if (j.isNull("birth")) null else LocalDate.ofEpochDay(j.getLong("birth"))
            return Person(nm, g, b)
        }
        val st = if (o.isNull("start")) start(c) else LocalDate.ofEpochDay(o.getLong("start"))
        val img = o.optJSONObject("img")
        restoreImg(img, "bg", Photo.bgFile(c))
        restoreImg(img, "a", Photo.avatarFile(c, 0))
        restoreImg(img, "b", Photo.avatarFile(c, 1))
        saveInfo(c, pp("p0", 0), pp("p1", 1), st)
        true
    } catch (e: Exception) { false }
}
