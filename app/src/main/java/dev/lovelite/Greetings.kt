package dev.lovelite

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class Greeting(val title: String, val body: String, val special: Boolean)

/** Lời chúc từ app: ngày đặc biệt (sinh nhật, kỷ niệm, mốc ngày) hoặc lời nhắn thay đổi mỗi ngày. */
object Greetings {
    fun get(c: Context, today: LocalDate = LocalDate.now(), hour: Int = LocalTime.now().hour): Greeting {
        val p0 = Store.person(c, 0)
        val p1 = Store.person(c, 1)
        val start = Store.start(c)
        val n0 = Info.ref(c, 0, p0)
        val n1 = Info.ref(c, 1, p1)

        fun special(body: String) = Greeting(c.getString(R.string.sp_title), body, true)

        val b0 = p0.birth?.let { Milestones.nextBirthday(it, today) }
        val b1 = p1.birth?.let { Milestones.nextBirthday(it, today) }
        if (b0 == today) return special(c.getString(R.string.sp_bday_self, n0, today.year - p0.birth!!.year))
        if (b1 == today) return special(c.getString(R.string.sp_bday_partner, n1, today.year - p1.birth!!.year))

        if (start != null && !start.isAfter(today)) {
            val m = ChronoUnit.MONTHS.between(start, today)
            if (m > 0 && start.plusMonths(m) == today) {
                return special(
                    if (m % 12 == 0L) c.getString(R.string.sp_year, (m / 12).toInt())
                    else c.getString(R.string.sp_month, m.toInt())
                )
            }
            val n = Milestones.daysTogether(start, today)
            if (n > 0 && n % 100 == 0L) return special(c.getString(R.string.sp_days, n.toInt()))
        }

        // Sinh nhật trong vòng 7 ngày tới (ưu tiên người sắp tới sớm hơn).
        val soon = listOfNotNull(
            b0?.let { Triple(ChronoUnit.DAYS.between(today, it).toInt(), n0, 0) },
            b1?.let { Triple(ChronoUnit.DAYS.between(today, it).toInt(), n1, 1) },
        ).filter { it.first in 1..7 }.minByOrNull { it.first }
        if (soon != null) {
            return Greeting(
                c.getString(R.string.sp_soon_title),
                c.resources.getQuantityString(R.plurals.sp_bday_soon, soon.first, soon.second, soon.first),
                true,
            )
        }

        val titleRes = when (hour) {
            in 5..10 -> R.string.greet_morning
            in 11..12 -> R.string.greet_noon
            in 13..17 -> R.string.greet_afternoon
            in 18..21 -> R.string.greet_evening
            else -> R.string.greet_night
        }
        val msgs = c.resources.getStringArray(R.array.daily_msgs)
        val body = msgs[Math.floorMod(today.toEpochDay().toInt(), msgs.size)].format(n1)
        return Greeting(c.getString(titleRes, n0), body, false)
    }
}
