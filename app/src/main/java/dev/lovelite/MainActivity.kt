package dev.lovelite

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate

class MainActivity : Activity() {
    private companion object {
        const val REQ_PHOTO = 1
        const val REQ_EXPORT = 2
        const val REQ_IMPORT = 3
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        click(R.id.btn_info) { editInfo() }
        click(R.id.btn_photo) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            }, REQ_PHOTO)
        }
        click(R.id.btn_diary) { startActivity(Intent(this, DiaryActivity::class.java)) }
        click(R.id.btn_export) {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "lovelite-backup.json")
            }, REQ_EXPORT)
        }
        click(R.id.btn_import) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }, REQ_IMPORT)
        }
        click(R.id.btn_pin) { editPin() }
    }

    override fun onStart() {
        super.onStart()
        if (!Guard.check(this)) render()
    }

    override fun onPause() { super.onPause(); Guard.touch() }

    private fun click(id: Int, f: () -> Unit) = findViewById<Button>(id).setOnClickListener { f() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(id: Int) = Toast.makeText(this, id, Toast.LENGTH_SHORT).show()

    private fun render() {
        val (a, b) = Store.names(this)
        val start = Store.start(this)
        findViewById<TextView>(R.id.names).text =
            listOf(a, b).filter { it.isNotBlank() }.joinToString(" ♥ ").ifEmpty { getString(R.string.tap_setup) }
        findViewById<TextView>(R.id.days).text = start?.let { Milestones.daysTogether(it).toString() } ?: "♥"
        findViewById<TextView>(R.id.since).text =
            start?.let { getString(R.string.since, it.format(Milestones.fmt)) } ?: ""
        findViewById<TextView>(R.id.upcoming).text =
            start?.let { Milestones.upcoming(this, it).joinToString("\n") } ?: ""
        findViewById<ImageView>(R.id.bg).setImageBitmap(Photo.load(this))
    }

    private fun editInfo() {
        val (a, b) = Store.names(this)
        var date = Store.start(this) ?: LocalDate.now()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val ea = EditText(this).apply { hint = getString(R.string.you); setText(a); setSingleLine() }
        val eb = EditText(this).apply { hint = getString(R.string.partner); setText(b); setSingleLine() }
        val bd = Button(this).apply { text = date.format(Milestones.fmt) }
        bd.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                date = LocalDate.of(y, m + 1, d)
                bd.text = date.format(Milestones.fmt)
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }
        box.addView(ea); box.addView(eb); box.addView(bd)
        AlertDialog.Builder(this).setTitle(R.string.edit_info).setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Store.saveInfo(this, ea.text.toString().trim(), eb.text.toString().trim(), date)
                render()
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun editPin() {
        val et = EditText(this).apply {
            hint = getString(R.string.pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle(R.string.btn_pin).setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val p = et.text.toString()
                if (p.isEmpty() || p.length in 4..8) { Store.setPin(this, p); Guard.touch(); toast(R.string.saved) }
                else toast(R.string.failed)
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri: Uri = data?.data ?: return
        if (res != RESULT_OK) return
        when (req) {
            REQ_PHOTO -> Thread {
                val ok = Photo.save(this, uri)
                runOnUiThread { if (ok) render() else toast(R.string.failed) }
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
