package com.example.joypoint

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.joypoint.databinding.ActivityAnadirBinding
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextMenu
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actividad para añadir o editar un memo
 */
class AnadirActivity : AppCompatActivity() {

    // Declaración de variables
    private lateinit var binding: ActivityAnadirBinding
    private lateinit var urlEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var idMemo: String
    private val INSERTANDO_MODIFICANDO: Int = 1
    private lateinit var objetoIntent: Intent
    private val CHANNEL_ID: String = "123"

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearObjetosDelXml()
        //createNotificationChannel()

        // Registrar el menú contextual para los EditText
        registerForContextMenu(binding.editAcompanado)
        registerForContextMenu(binding.editSentimiento)

        objetoIntent = intent

        // Verificar si se abre la activity con un id agregado, en caso de que si se haya agragado se añaden automaticamente los datos del memo
        if (objetoIntent.hasExtra("id")) {
            idMemo = objetoIntent.getStringExtra("id")!!
            binding.editComentario.setText(objetoIntent.getStringExtra("comentarios"))
            binding.editLocalizacion.setText(objetoIntent.getStringExtra("localizacion"))
            binding.editAcompanado.setText(objetoIntent.getStringExtra("acompanado"))
            binding.editSentimiento.setText(objetoIntent.getStringExtra("sentimiento"))
            binding.editTextEnlaceFoto.setText(objetoIntent.getStringExtra("foto"))
        }

        // Cargar la imagen desde la URL usando Glide
        Glide.with(binding.imageViewFoto.context)
            .load(binding.editTextEnlaceFoto.text.toString()).into(binding.imageViewFoto)

        // Configuración de los botones
        binding.btnCancelar.setOnClickListener {
            volver()
        }

        binding.btnGuardar.setOnClickListener {
            volverRetornandoDatos()
        }

        // Inicializar el EditText y el ImageView
        urlEditText = findViewById(R.id.editTextEnlaceFoto)
        imageView = findViewById(R.id.imageViewFoto)

        // Agregar un TextWatcher al EditText para cargar la imagen desde la URL
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No usado
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadImageFromURL()
            }

            override fun afterTextChanged(s: Editable?) {
                // No usado
            }
        })
    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityAnadirBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Función que genera el canal de notificaciones
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = resources.getString(R.string.app_name)
            val descriptionText = "Canal para notificaciones"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply{
                description = descriptionText
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun lanzarNotificaciones(view: View) {

        // Queremos que se haga algo cuando se pulse en la notificación; creamos
        // un intent, una intención

        // PendingIntent permite lanzar algo al sistema a alguien, al margen de que
        // la aplicación que ha hecho el lanzamiento ya no esté en ejecución; se
        // queda ahí aunque hayas cerrado la aplicación
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            // FLAG_IMMUTABLE hace que el contenido de PendingIntent no pueda ser modificado
            // Puede haber casos en que lance un PendingIntent varias veces y se pueda
            // modificar; no es este caso.
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        // Ahora hay que decirle a la app que ejecute ese PendingIntent cuando
        // pulsemos en la notificación: línea de .setContentIntent(contentIntent)
        // Con ello hago que, al pulsar en la notificación desde fuera de la app,
        // me devuelva a ella






    }

    /**
     * Crea el menú de opciones en la barra de acción
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Crea el menú contextual para los EditText
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        when (v) {
            binding.editAcompanado -> menuInflater.inflate(R.menu.menu_compania, menu)
            binding.editSentimiento -> menuInflater.inflate(R.menu.menu_emocion, menu)
        }
    }

    /**
     * Maneja las selecciones de elementos en el menú contextual
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.solo -> {
                binding.editAcompanado.setText(resources.getString(R.string.solo))
                true
            }

            R.id.acompanado -> {
                binding.editAcompanado.setText(resources.getString(R.string.acompanado))
                true
            }

            R.id.feliz -> {
                binding.editSentimiento.setText(resources.getString(R.string.feliz))
                true
            }

            R.id.normal -> {
                binding.editSentimiento.setText(resources.getString(R.string.normal))
                true
            }

            R.id.triste -> {
                binding.editSentimiento.setText(resources.getString(R.string.triste))
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    /**
     * Carga una imagen desde una URL utilizando un AsyncTask (impide que el teléfono se congele)
     */
    fun loadImageFromURL() {
        val url = urlEditText.text.toString()
        DownloadImageTask().execute(url)
    }

    /**
     * Clase interna para descargar una imagen desde una URL en segundo plano.
     */
    private inner class DownloadImageTask : AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg urls: String): Bitmap? {
            val imageUrl = urls[0]
            var bitmap: Bitmap? = null

            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val input = connection.inputStream
                bitmap = BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                // Mostrar la imagen descargada en el ImageView
                imageView.setImageBitmap(result)
            } else {
                // Manejar el caso en que la imagen no se pueda cargar
                Toast.makeText(
                    this@AnadirActivity,
                    "Error al cargar la imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Método para cerrar la actividad actual
     */
    fun volver() {
        finish()
    }

    /**
     * Método para cerrar la actividad actual y retornar datos a la actividad anterior
     */
    fun volverRetornandoDatos() {
        val intent = Intent()

        // Incluir datos en el intent
        if (objetoIntent.hasExtra("id")) {
            intent.putExtra("id", idMemo)
        }
        intent.putExtra("comentarios", binding.editComentario.text.toString())
        intent.putExtra("localizacion", binding.editLocalizacion.text.toString())
        intent.putExtra("acompanado", binding.editAcompanado.text.toString())
        intent.putExtra("sentimiento", binding.editSentimiento.text.toString())
        intent.putExtra("foto", binding.editTextEnlaceFoto.text.toString())

        // Establecer el resultado y cerrar la actividad
        setResult(INSERTANDO_MODIFICANDO, intent)
        finish()
    }
}
