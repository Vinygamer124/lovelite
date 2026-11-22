package dev.lovelite

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import java.time.LocalDate

class DiaryActivity : Activity() {
    private var list = mutableListOf<Entry>()
    private lateinit var lv: ListView

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_diary)
        lv = findViewById(R.id.list)
        findViewById<Button>(R.id.btn_add).setOnClickListener { edit(-1) }
        lv.setOnItemClickListener { _, _, i, _ -> edit(i) }
    }

    override fun onStart() {
        super.onStart()
        if (Guard.check(this)) return
        list = Store.diary(this)
        val rows = list.map {
            mapOf("d" to LocalDate.ofEpochDay(it.day).format(Milestones.fmt), "t" to it.text)
        }
        lv.adapter = SimpleAdapter(
            this, rows, android.R.layout.simple_list_item_2,
            arrayOf("d", "t"), intArrayOf(android.R.id.text1, android.R.id.text2),
        )
    }

    override fun onPause() { super.onPause(); Guard.touch() }

    private fun edit(i: Int) {
        val et = EditText(this).apply {
            hint = getString(R.string.diary_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            if (i >= 0) setText(list[i].text)
        }
        val b = AlertDialog.Builder(this)
            .setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val t = et.text.toString().trim()
                if (t.isNotEmpty()) {
                    if (i >= 0) list[i] = list[i].copy(text = t)
                    else list.add(0, Entry(LocalDate.now().toEpochDay(), t))
                    Store.saveDiary(this, list)
                    onStart()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
        if (i >= 0) b.setNeutralButton(R.string.delete) { _, _ ->
            list.removeAt(i)
            Store.saveDiary(this, list)
            onStart()
        }
        b.show()
    }
}
