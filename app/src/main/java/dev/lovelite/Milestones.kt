package dev.lovelite

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Upcoming(val date: LocalDate, val label: String, val whenText: String, val birthday: Boolean)

object Milestones {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Ngày thứ N tính cả ngày đầu tiên (ngày bắt đầu = ngày 1). */
    fun daysTogether(start: LocalDate, today: LocalDate = LocalDate.now()): Long =
        maxOf(0L, ChronoUnit.DAYS.between(start, today) + 1)

    /** Lần sinh nhật kế tiếp (hôm nay cũng tính). */
    fun nextBirthday(birth: LocalDate, today: LocalDate = LocalDate.now()): LocalDate {
        val d = birth.withYear(today.year)
        return if (d.isBefore(today)) birth.withYear(today.year + 1) else d
    }

    fun whenText(c: Context, d: LocalDate, today: LocalDate): String {
        val left = ChronoUnit.DAYS.between(today, d).toInt()
        return if (left == 0) c.getString(R.string.today)
        else c.resources.getQuantityString(R.plurals.in_days, left, left)
    }

    /** Các mốc sắp tới: tròn trăm/nghìn ngày, kỷ niệm theo năm và sinh nhật hai người. */
    fun upcoming(c: Context, start: LocalDate?, people: List<Person>, max: Int = 6): List<Upcoming> {
        val today = LocalDate.now()
        val items = ArrayList<Upcoming>()
        if (start != null) {
            var n = 100
            while (n <= 36500) {
                val d = start.plusDays((n - 1).toLong())
                if (!d.isBefore(today)) items += Upcoming(d, c.resources.getQuantityString(R.plurals.m_days, n, n), "", false)
                n += if (n < 1000) 100 else 500
            }
            for (y in 1..100) {
                val d = start.plusYears(y.toLong())
                if (!d.isBefore(today)) items += Upcoming(d, c.resources.getQuantityString(R.plurals.m_years, y, y), "", false)
            }
        }
        for ((i, p) in people.withIndex()) {
            val b = p.birth ?: continue
            val d = nextBirthday(b, today)
            items += Upcoming(d, c.getString(R.string.birthday_of, Info.ref(c, i, p), d.year - b.year), "", true)
        }
        return items.sortedBy { it.date }.take(max).map { it.copy(whenText = whenText(c, it.date, today)) }
    }
}
