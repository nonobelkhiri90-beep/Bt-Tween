package com.carcam.core

import android.content.Context

/** تخزين عنوان MAC لبلوتوث السيارة المختارة */
class CarPrefs(context: Context) {
    private val sp = context.getSharedPreferences("carcam_prefs", Context.MODE_PRIVATE)
    var carAddress: String?
        get() = sp.getString("car_address", null)
        set(v) = sp.edit().putString("car_address", v).apply()
    var carName: String?
        get() = sp.getString("car_name", null)
        set(v) = sp.edit().putString("car_name", v).apply()
}
