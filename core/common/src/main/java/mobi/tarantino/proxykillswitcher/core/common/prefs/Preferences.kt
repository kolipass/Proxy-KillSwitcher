package mobi.tarantino.proxykillswitcher.core.common.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class Preferences(
    context: Context,
    storeName: String,
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(storeName, Context.MODE_PRIVATE)

    protected fun stringProperty(
        key: String,
        defaultValue: String = "",
    ): ReadWriteProperty<Any?, String> {
        return preferenceProperty(
            readValue = { sharedPreferences.getString(key, defaultValue) ?: defaultValue },
            writeValue = { updatedValue -> sharedPreferences.edit().putString(key, updatedValue).apply() },
        )
    }

    protected fun intProperty(
        key: String,
        defaultValue: Int = 0,
    ): ReadWriteProperty<Any?, Int> {
        return preferenceProperty(
            readValue = { sharedPreferences.getInt(key, defaultValue) },
            writeValue = { updatedValue -> sharedPreferences.edit().putInt(key, updatedValue).apply() },
        )
    }

    protected fun booleanProperty(
        key: String,
        defaultValue: Boolean = false,
    ): ReadWriteProperty<Any?, Boolean> {
        return preferenceProperty(
            readValue = { sharedPreferences.getBoolean(key, defaultValue) },
            writeValue = { updatedValue -> sharedPreferences.edit().putBoolean(key, updatedValue).apply() },
        )
    }

    private fun <T> preferenceProperty(
        readValue: () -> T,
        writeValue: (T) -> Unit,
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(
                thisRef: Any?,
                property: KProperty<*>,
            ): T {
                return readValue()
            }

            override fun setValue(
                thisRef: Any?,
                property: KProperty<*>,
                value: T,
            ) {
                writeValue(value)
            }
        }
    }
}
