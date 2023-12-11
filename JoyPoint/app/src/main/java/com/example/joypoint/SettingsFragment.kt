package com.example.joypoint

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

// Definición de la clase SettingsFragment que extiende PreferenceFragmentCompat.
class SettingsFragment: PreferenceFragmentCompat() {

    /**
     * Hereda de PreferenceFragmentCompat, para que extienda de la clase dedicada a fragmentos de preferencias
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferencias, rootKey)
    }
}