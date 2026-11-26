package dev.lovelite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

/** Khoá PIN: yêu cầu lại nếu rời app quá 30 giây. */
object Guard {
    private var last = 0L
    fun touch() { last = System.currentTimeMillis() }

    /** Trả về true nếu đã mở màn hình khoá (activity gọi nên dừng vẽ UI). */
    fun check(a: Activity): Boolean {
        if (Store.hasPin(a) && System.currentTimeMillis() - last > 30_000) {
            a.startActivity(Intent(a, LockActivity::class.java))
            return true
        }
        return false
    }
}

class LockActivity : BaseActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_lock)
        val pin = findViewById<EditText>(R.id.pin)
        val tryUnlock = {
            if (Store.checkPin(this, pin.text.toString())) {
                Guard.touch()
                finish()
            } else {
                pin.text.clear()
                Toast.makeText(this, R.string.wrong_pin, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.unlock).setOnClickListener { tryUnlock() }
        pin.setOnEditorActionListener { _, _, _ -> tryUnlock(); true }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = finishAffinity()
}
