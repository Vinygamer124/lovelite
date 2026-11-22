package dev.lovelite

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Milestones {
    val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Ngày thứ N tính cả ngày đầu tiên (ngày bắt đầu = ngày 1). */
    fun daysTogether(start: LocalDate, today: LocalDate = LocalDate.now()): Long =
        maxOf(0L, ChronoUnit.DAYS.between(start, today) + 1)

    /** Các mốc sắp tới: tròn trăm/nghìn ngày và kỷ niệm theo năm. */
    fun upcoming(c: Context, start: LocalDate, max: Int = 6): List<String> {
        val today = LocalDate.now()
        val items = ArrayList<Pair<LocalDate, String>>()
        var n = 100
        while (n <= 36500) {
            items += start.plusDays((n - 1).toLong()) to c.getString(R.string.m_days, n)
            n += if (n < 1000) 100 else 500
        }
        for (y in 1..100) items += start.plusYears(y.toLong()) to c.getString(R.string.m_years, y)
        return items.filter { !it.first.isBefore(today) }
            .sortedBy { it.first }
            .take(max)
            .map { (d, label) ->
                val left = ChronoUnit.DAYS.between(today, d)
                val `when` = if (left == 0L) c.getString(R.string.today) else c.getString(R.string.in_days, left.toInt())
                "♥ $label — ${d.format(fmt)} ($`when`)"
            }
    }
}
