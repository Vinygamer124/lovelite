package dev.lovelite

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

/** Màn hình nhập thông tin hai người: ảnh đại diện, tên, giới tính, ngày sinh và ngày bắt đầu yêu. */
class InfoActivity : BaseActivity() {
    private inner class Form(val i: Int, val root: View) {
        val avatar: ImageView = root.findViewById(R.id.f_avatar)
        val name: EditText = root.findViewById(R.id.f_name)
        val birthView: TextView = root.findViewById(R.id.f_birth)
        val clear: View = root.findViewById(R.id.f_birth_clear)
        val hint: TextView = root.findViewById(R.id.f_hint)
        val seg = listOf(1 to R.id.g_male, 2 to R.id.g_female, 3 to R.id.g_other)
            .map { (g, id) -> g to root.findViewById<TextView>(id) }
        var gender = 0
        var birth: LocalDate? = null

        fun show() {
            val p = Person(name.text.toString(), gender, birth)
            avatar.setImageBitmap(Photo.circle(Photo.loadAvatar(this@InfoActivity, i), dp(96), Info.initial(p), gender))
            birthView.text = birth?.format(Milestones.fmt) ?: getString(R.string.pick_birth)
            birthView.setTextColor(getColor(if (birth == null) R.color.ink_soft else R.color.ink))
            clear.visibility = if (birth == null) View.GONE else View.VISIBLE
            val h = listOf(Info.meta(this@InfoActivity, p.copy(gender = 0)), Info.zodiac(this@InfoActivity, p))
                .filter { it.isNotEmpty() }.joinToString(" · ")
            hint.text = h
            hint.visibility = if (h.isEmpty()) View.GONE else View.VISIBLE
            for ((g, tv) in seg) {
                tv.isSelected = g == gender
                tv.setTextColor(if (g == gender) Color.WHITE else getColor(R.color.rose_dark))
            }
        }
    }

    private val forms = ArrayList<Form>()
    private var start: LocalDate? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_info)
        findViewById<TextView>(R.id.tb_title).setText(R.string.edit_info)
        findViewById<View>(R.id.tb_back).setOnClickListener { finish() }

        val host = findViewById<LinearLayout>(R.id.forms)
        for (i in 0..1) {
            val v = layoutInflater.inflate(R.layout.view_person_form, host, false)
            v.isSaveFromParentEnabled = false // hai form dùng chung id con
            host.addView(v)
            val f = Form(i, v)
            val p = Store.person(this, i)
            v.findViewById<TextView>(R.id.f_title).setText(if (i == 0) R.string.form_you else R.string.form_partner)
            f.name.setText(p.name)
            f.gender = p.gender
            f.birth = p.birth
            f.avatar.setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
                }, 10 + i)
            }
            f.name.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun afterTextChanged(e: android.text.Editable?) { if (Store.avatarMissing(this@InfoActivity, i)) f.show() }
            })
            f.birthView.setOnClickListener {
                pickDate(f.birth ?: LocalDate.of(2000, 1, 1), true) { f.birth = it; f.show() }
            }
            f.clear.setOnClickListener { f.birth = null; f.show() }
            for ((g, tv) in f.seg) tv.setOnClickListener { f.gender = if (f.gender == g) 0 else g; f.show() }
            f.show()
            forms += f
        }

        start = Store.start(this)
        val sv = findViewById<TextView>(R.id.f_start)
        fun showStart() {
            sv.text = start?.format(Milestones.fmt) ?: getString(R.string.pick_date)
            sv.setTextColor(getColor(if (start == null) R.color.ink_soft else R.color.ink))
        }
        showStart()
        sv.setOnClickListener { pickDate(start ?: LocalDate.now(), false) { start = it; showStart() } }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val ps = forms.map { Person(it.name.text.toString().trim(), it.gender, it.birth) }
            Store.saveInfo(this, ps[0], ps[1], start)
            toast(R.string.saved)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Guard.check(this)) return
    }

    override fun onPause() { super.onPause(); Guard.touch() }

    private fun pickDate(cur: LocalDate, past: Boolean, done: (LocalDate) -> Unit) {
        val d = DatePickerDialog(this, { _, y, m, day -> done(LocalDate.of(y, m + 1, day)) }, cur.year, cur.monthValue - 1, cur.dayOfMonth)
        if (past) d.datePicker.maxDate = System.currentTimeMillis()
        d.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (res != RESULT_OK || req !in 10..11) return
        val i = req - 10
        Thread {
            val ok = Photo.saveAvatar(this, i, uri)
            runOnUiThread { if (ok) { forms[i].show(); Widgets.refresh(this) } else toast(R.string.failed) }
        }.start()
    }
}
