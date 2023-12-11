package com.example.joypoint

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// Definición de la clase SettingsActivity que extiende AppCompatActivity
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings) // Establece el diseño de la actividad

        // Comprueba si el fragmento ya está en el savedInstanceState antes de agregarlo.
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment()) // Reemplaza el contenido del contenedor con el SettingsFragment
                .commit()
        }
    }
}
