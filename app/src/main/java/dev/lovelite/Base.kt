package dev.lovelite

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Ngôn ngữ trong app: mặc định tiếng Việt, không phụ thuộc ngôn ngữ hệ thống. */
object Lang {
    fun wrap(base: Context): Context {
        val loc = Locale(Store.lang(base))
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        return base.createConfigurationContext(cfg)
    }
}

open class BaseActivity : Activity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Lang.wrap(newBase))

    fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    fun toast(id: Int) = android.widget.Toast.makeText(this, id, android.widget.Toast.LENGTH_SHORT).show()
}
