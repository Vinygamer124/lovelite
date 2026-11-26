package dev.lovelite

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import java.time.LocalDate

class MainActivity : BaseActivity() {
    private companion object {
        const val REQ_BG = 1
        const val REQ_EXPORT = 2
        const val REQ_IMPORT = 3
        const val REQ_AV = 4 // 4 = bạn, 5 = người ấy
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)

        // Ảnh nền tràn lên dưới thanh trạng thái, header bo tròn hai góc dưới.
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        val radius = dp(28).toFloat()
        findViewById<View>(R.id.header).apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, -radius.toInt(), v.width, v.height, radius) }
            }
            clipToOutline = true
        }
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { _, ins ->
            @Suppress("DEPRECATION")
            val top = ins.systemWindowInsetTop
            findViewById<View>(R.id.status_space).apply {
                layoutParams = (layoutParams as ViewGroup.LayoutParams).apply { height = top }
            }
            ins
        }

        click(R.id.btn_info) { startActivity(Intent(this, InfoActivity::class.java)) }
        click(R.id.btn_photo) { pickImage(REQ_BG) }
        click(R.id.btn_diary) { startActivity(Intent(this, DiaryActivity::class.java)) }
        findViewById<View>(R.id.col_a).setOnClickListener { pickImage(REQ_AV) }
        findViewById<View>(R.id.col_b).setOnClickListener { pickImage(REQ_AV + 1) }
        findViewById<View>(R.id.row_lang).setOnClickListener { chooseLang() }
        findViewById<View>(R.id.row_widget).setOnClickListener {
            val sw = findViewById<Switch>(R.id.sw_widget)
            sw.isChecked = !sw.isChecked
            Store.setWidgetPhoto(this, sw.isChecked)
            Widgets.refresh(this)
        }
        findViewById<View>(R.id.row_pin).setOnClickListener { editPin() }
        findViewById<View>(R.id.row_export).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "lovelite-backup.json")
            }, REQ_EXPORT)
        }
        findViewById<View>(R.id.row_import).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }, REQ_IMPORT)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!Guard.check(this)) render()
    }

    override fun onPause() { super.onPause(); Guard.touch() }

    private fun click(id: Int, f: () -> Unit) = findViewById<Button>(id).setOnClickListener { f() }

    private fun pickImage(req: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
        }, req)
    }

    private fun render() {
        val start = Store.start(this)
        val people = listOf(Store.person(this, 0), Store.person(this, 1))

        findViewById<TextView>(R.id.days).text = start?.let { Milestones.daysTogether(it).toString() } ?: "♥"
        findViewById<TextView>(R.id.days_label).text =
            if (start == null) getString(R.string.tap_setup) else getString(R.string.days_together)
        findViewById<TextView>(R.id.since).text =
            start?.let { getString(R.string.since, it.format(Milestones.fmt)) } ?: ""
        findViewById<ImageView>(R.id.bg).setImageBitmap(Photo.loadBg(this))

        bind(0, people[0], R.id.av_a, R.id.name_a, R.id.meta_a, R.id.zodiac_a)
        bind(1, people[1], R.id.av_b, R.id.name_b, R.id.meta_b, R.id.zodiac_b)

        val g = Greetings.get(this)
        findViewById<TextView>(R.id.greet_title).text = g.title
        findViewById<TextView>(R.id.greet_msg).text = g.body

        val list = findViewById<LinearLayout>(R.id.list_upcoming)
        list.removeAllViews()
        val items = Milestones.upcoming(this, start, people)
        if (items.isEmpty()) {
            list.addView(TextView(this).apply {
                text = getString(R.string.upcoming_empty)
                setTextColor(getColor(R.color.ink_soft)); textSize = 14f; setPadding(dp(16), dp(14), dp(16), dp(14))
            })
        }
        for ((i, u) in items.withIndex()) {
            val row = layoutInflater.inflate(R.layout.item_upcoming, list, false)
            row.findViewById<TextView>(R.id.u_icon).text = if (u.birthday) "🎂" else "♥"
            row.findViewById<TextView>(R.id.u_label).text = u.label
            row.findViewById<TextView>(R.id.u_date).text = u.date.format(Milestones.fmt)
            row.findViewById<TextView>(R.id.u_when).text = u.whenText
            list.addView(row)
            if (i < items.size - 1) list.addView(View(this).apply {
                setBackgroundColor(getColor(R.color.line))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply { leftMargin = dp(16); rightMargin = dp(16) }
            })
        }

        findViewById<TextView>(R.id.val_lang).text =
            getString(if (Store.lang(this) == "en") R.string.lang_en else R.string.lang_vi)
        findViewById<Switch>(R.id.sw_widget).isChecked = Store.widgetPhoto(this)
        findViewById<TextView>(R.id.val_pin).text = getString(if (Store.hasPin(this)) R.string.pin_on else R.string.pin_off)
    }

    private fun bind(i: Int, p: Person, av: Int, name: Int, meta: Int, zod: Int) {
        findViewById<ImageView>(av).setImageBitmap(
            Photo.circle(Photo.loadAvatar(this, i), dp(92), Info.initial(p), p.gender)
        )
        findViewById<TextView>(name).text = Info.name(this, i, p)
        fun set(id: Int, t: String) = findViewById<TextView>(id).apply {
            text = t
            visibility = if (t.isEmpty()) View.GONE else View.VISIBLE
        }
        set(meta, Info.meta(this, p))
        set(zod, Info.zodiac(this, p))
    }

    private fun chooseLang() {
        val cur = if (Store.lang(this) == "en") 1 else 0
        AlertDialog.Builder(this).setTitle(R.string.lang_title)
            .setSingleChoiceItems(arrayOf(getString(R.string.lang_vi), getString(R.string.lang_en)), cur) { d, which ->
                d.dismiss()
                if (which != cur) {
                    Store.setLang(this, if (which == 1) "en" else "vi")
                    Widgets.refresh(this)
                    recreate()
                }
            }.show()
    }

    private fun editPin() {
        val et = EditText(this).apply {
            hint = getString(R.string.pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val box = LinearLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(et, LinearLayout.LayoutParams(-1, -2)) }
        AlertDialog.Builder(this).setTitle(R.string.btn_pin).setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val p = et.text.toString()
                if (p.isEmpty() || p.length in 4..8) { Store.setPin(this, p); Guard.touch(); toast(R.string.saved); render() }
                else toast(R.string.failed)
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        when (req) {
            REQ_BG -> Thread {
                val ok = Photo.saveBg(this, uri)
                runOnUiThread { if (ok) { render(); Widgets.refresh(this) } else toast(R.string.failed) }
            }.start()
            REQ_AV, REQ_AV + 1 -> Thread {
                val ok = Photo.saveAvatar(this, req - REQ_AV, uri)
                runOnUiThread { if (ok) { render(); Widgets.refresh(this) } else toast(R.string.failed) }
            }.start()
            REQ_EXPORT -> try {
                contentResolver.openOutputStream(uri)?.use { it.write(Store.export(this).toByteArray()) }
                toast(R.string.saved)
            } catch (e: Exception) { toast(R.string.failed) }
            REQ_IMPORT -> try {
                val json = contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: ""
                if (Store.import(this, json)) { render(); toast(R.string.saved) } else toast(R.string.failed)
            } catch (e: Exception) { toast(R.string.failed) }
        }
    }
}
