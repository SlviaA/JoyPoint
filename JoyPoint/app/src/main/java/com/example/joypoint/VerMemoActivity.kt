package com.example.joypoint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.joypoint.databinding.ActivityVerMemoBinding
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actividad para visualizar un memo
 */
class VerMemoActivity : AppCompatActivity() {

    // Declaración de variables
    private lateinit var binding: ActivityVerMemoBinding
    private lateinit var imageView: ImageView

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_memo)
        // Inflar el diseño de la actividad
        crearObjetosDelXml()

        // Obtener datos del intent
        val intent = intent
        val comentarios = intent.getStringExtra("comentarios")
        val localizacion = intent.getStringExtra("localizacion")
        val acompanado = intent.getStringExtra("acompanado")
        val sentimiento = intent.getStringExtra("sentimiento")
        val foto = intent.getStringExtra("foto")

        // Configuración de vistas con los datos del intent
        binding.viewComentarios.setText(comentarios)
        binding.viewLocalizacion.setText(localizacion)
        binding.viewAcompanado.setText(acompanado)
        binding.viewSentimiento.setText(sentimiento)
        imageView = findViewById(R.id.imageView)

        // Cargar la imagen desde la URL
        loadImageFromURL(foto)

        // Configuración del EditText para comentarios
        binding.viewComentarios.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        binding.viewComentarios.gravity = Gravity.START or Gravity.TOP
        binding.viewComentarios.maxLines = 10
        binding.viewComentarios.isVerticalScrollBarEnabled = true

        // Configuración del botón de cancelar
        binding.btnCancelar.setOnClickListener {
            volver()
        }
    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityVerMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Método para cargar una imagen desde una URL
     */
    fun loadImageFromURL(foto: String?) {
        val url = foto
        DownloadImageTask().execute(url)
    }

    /**
     * Clase interna para descargar una imagen desde una URL en segundo plano
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
                    this@VerMemoActivity,
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
}
