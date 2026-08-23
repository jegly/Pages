package com.jegly.pages.core.datastore

import android.content.SharedPreferences

class InMemorySharedPreferences : SharedPreferences {

    private val map = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = map.toMap()
    override fun getString(key: String, defValue: String?) = map.getOrDefault(key, defValue) as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?) = map.getOrDefault(key, defValues) as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int) = (map.getOrDefault(key, defValue) as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long) = (map.getOrDefault(key, defValue) as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float) = (map.getOrDefault(key, defValue) as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = (map.getOrDefault(key, defValue) as? Boolean) ?: defValue
    override fun contains(key: String) = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = apply { changes[key] = values }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { removes.add(key) }
        override fun clear() = apply { clearAll = true }

        override fun commit(): Boolean { apply(); return true }

        override fun apply() {
            if (clearAll) map.clear()
            removes.forEach { map.remove(it) }
            map.putAll(changes)
            val changed = (changes.keys + removes).toSet()
            listeners.toList().forEach { l -> changed.forEach { k -> l.onSharedPreferenceChanged(this@InMemorySharedPreferences, k) } }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }
}
