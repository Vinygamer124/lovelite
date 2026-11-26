package dev.lovelite

import android.content.Context
import java.time.LocalDate
import java.time.Period

/** Tuổi, cung hoàng đạo, giới tính: các chuỗi hiển thị dùng chung cho app và widget. */
object Info {
    // Ngày bắt đầu cung kế tiếp của từng tháng (T1..T12); danh sách cung bắt đầu từ Ma Kết.
    private val cut = intArrayOf(20, 19, 21, 20, 21, 21, 23, 23, 23, 23, 22, 22)

    fun age(b: LocalDate, today: LocalDate = LocalDate.now()): Int =
        Period.between(b, today).years.coerceAtLeast(0)

    fun zodiacIndex(b: LocalDate): Int {
        val m = b.monthValue
        return if (b.dayOfMonth < cut[m - 1]) m - 1 else m % 12
    }

    fun zodiacName(c: Context, b: LocalDate): String = c.resources.getStringArray(R.array.zodiac)[zodiacIndex(b)]

    fun name(c: Context, i: Int, p: Person): String =
        p.name.ifBlank { c.getString(if (i == 0) R.string.you_default else R.string.partner_default) }

    /** Tên dùng trong câu văn (viết thường nếu chưa đặt tên). */
    fun ref(c: Context, i: Int, p: Person): String =
        p.name.ifBlank { c.getString(if (i == 0) R.string.you_lower else R.string.partner_lower) }

    fun initial(p: Person): String = p.name.trim().take(1).uppercase().ifEmpty { "♥" }

    fun genderText(c: Context, g: Int): String = when (g) {
        1 -> "♂ " + c.getString(R.string.gender_male)
        2 -> "♀ " + c.getString(R.string.gender_female)
        3 -> c.getString(R.string.gender_other)
        else -> ""
    }

    /** "♂ Nam · 24 tuổi" */
    fun meta(c: Context, p: Person): String {
        val parts = ArrayList<String>()
        genderText(c, p.gender).takeIf { it.isNotEmpty() }?.let { parts += it }
        p.birth?.let { parts += c.resources.getQuantityString(R.plurals.age_years, age(it), age(it)) }
        return parts.joinToString(" · ")
    }

    /** "Cung Sư Tử" */
    fun zodiac(c: Context, p: Person): String =
        p.birth?.let { c.getString(R.string.zodiac_fmt, zodiacName(c, it)) } ?: ""

    /** Dòng gọn cho widget: "♂ 24 · Sư Tử" */
    fun short(c: Context, p: Person): String {
        val parts = ArrayList<String>()
        val g = when (p.gender) { 1 -> "♂"; 2 -> "♀"; else -> "" }
        val b = p.birth
        val left = listOf(g, if (b != null) age(b).toString() else "").filter { it.isNotEmpty() }.joinToString(" ")
        if (left.isNotEmpty()) parts += left
        if (b != null) parts += zodiacName(c, b)
        return parts.joinToString(" · ")
    }
}
