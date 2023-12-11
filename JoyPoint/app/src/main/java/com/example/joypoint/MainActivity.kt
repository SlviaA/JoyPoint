package com.example.joypoint

import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.joypoint.databinding.ActivityMainBinding

/**
 * Actividad principal que representa la pantalla principal de la aplicación
 */
class MainActivity : AppCompatActivity() {

    // Declaración de variables
    private lateinit var binding: ActivityMainBinding

    //Sonidos en botones
    lateinit var soundPool: SoundPool

    var pop: Int = 0

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de objetos y configuración de la barra de herramientas
        crearObjetosDelXml()

        // Configuración de la toolbar
        setSupportActionBar(binding.appbar.toolbarApp)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configuración de EditText y Button
        val editText = findViewById<EditText>(R.id.editNombre)
        val button = findViewById<Button>(R.id.btnComenzar)

        button.isEnabled = false // Deshabilitar el botón que lleva a VisualizarActivity al inicio

        // Agregar un TextWatcher al EditText para habilitar/deshabilitar el botón en función del texto
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString()
                button.isEnabled = !inputText.isEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        //Variables para sonidos en botones
        var audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        pop = soundPool.load(this, R.raw.pop, 1)
    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Crea y muestra el menú de opciones en la barra de acción
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Maneja las selecciones de elementos en el menú de opciones
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.preferencias -> {
                preferencias()
                true
            }

            R.id.acercaDe -> {
                lanzarAcercaDe()
                true
            }

            R.id.Web -> {
                abrirPagina()
                true
            }

            R.id.ContactarMail -> {
                mandarCorreo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Mostrar un mensaje indicando que las preferencias no están disponibles
     */
    fun preferencias(){
        Toast.makeText(this, resources.getString(R.string.labelPref), Toast.LENGTH_LONG).show()
    }

    /**
     * Muestra un mensaje informativo
     */
    fun lanzarAcercaDe() {
        Toast.makeText(this, resources.getString(R.string.infoAcerca), Toast.LENGTH_LONG).show()
    }

    /**
     * Abre una página web en el navegador
     */
    fun abrirPagina() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.avancepsicologos.com/teoria-personalidad")))
    }

    /**
     * Inicia un envío de correo electrónico
     */
    fun mandarCorreo() {
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                // El EXTRA_SUBJECT es el asunto
                putExtra(Intent.EXTRA_SUBJECT, "Correo de prueba")
                // El EXTRA_TEXT es el contenido del correo
                putExtra(Intent.EXTRA_TEXT, "Hola, ésto es un mensaje de prueba para la asignatura de ProgMult")
                // Declaramos un array en el que podemos meter varias direcciones
                putExtra(Intent.EXTRA_EMAIL, arrayOf("silvitaat02@gmail.com"))
            }
        )
    }

    fun playSound(view: View){
        var sonido: Int = 0

        when (view.tag){
            "pop" -> sonido = pop
        }
        soundPool.play(sonido, 1F, 1F, 1, 0, 1F)
        openSomeActivity(view)
    }

    /**
     * Abre la actividad VisualizarActivity
     */
    fun openSomeActivity(view: View) {
        val intent = Intent(this, VisualizarActivity::class.java)
        intent.putExtra("nombre", binding.editNombre.text.toString())
        startActivity(intent)
    }
}
