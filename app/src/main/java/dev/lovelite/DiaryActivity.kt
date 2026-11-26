package dev.lovelite

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.time.LocalDate

class DiaryActivity : BaseActivity() {
    private var list = mutableListOf<Entry>()
    private lateinit var lv: ListView

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = list.size
        override fun getItem(p: Int) = list[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, v: View?, parent: ViewGroup): View {
            val row = v ?: LayoutInflater.from(this@DiaryActivity).inflate(R.layout.item_diary, parent, false)
            row.findViewById<TextView>(R.id.d_date).text = LocalDate.ofEpochDay(list[p].day).format(Milestones.fmt)
            row.findViewById<TextView>(R.id.d_text).text = list[p].text
            return row
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_diary)
        findViewById<TextView>(R.id.tb_title).setText(R.string.diary_title)
        findViewById<View>(R.id.tb_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tb_action).apply {
            setText(R.string.add); visibility = View.VISIBLE
            setOnClickListener { edit(-1) }
        }
        lv = findViewById(R.id.list)
        lv.adapter = Adapter()
        lv.setOnItemClickListener { _, _, i, _ -> edit(i) }
    }

    override fun onStart() {
        super.onStart()
        if (Guard.check(this)) return
        list = Store.diary(this)
        refresh()
    }

    private fun refresh() {
        (lv.adapter as BaseAdapter).notifyDataSetChanged()
        findViewById<View>(R.id.empty).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onPause() { super.onPause(); Guard.touch() }

    private fun edit(i: Int) {
        val et = EditText(this).apply {
            hint = getString(R.string.diary_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            gravity = android.view.Gravity.TOP
            if (i >= 0) setText(list[i].text)
        }
        val box = LinearLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(et, LinearLayout.LayoutParams(-1, -2)) }
        val b = AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val t = et.text.toString().trim()
                if (t.isNotEmpty()) {
                    if (i >= 0) list[i] = list[i].copy(text = t)
                    else list.add(0, Entry(LocalDate.now().toEpochDay(), t))
                    Store.saveDiary(this, list)
                    refresh()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
        if (i >= 0) b.setNeutralButton(R.string.delete) { _, _ ->
            list.removeAt(i)
            Store.saveDiary(this, list)
            refresh()
        }
        b.show()
    }
}
