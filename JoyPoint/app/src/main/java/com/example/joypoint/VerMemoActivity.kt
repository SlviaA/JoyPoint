package com.example.joypoint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.joypoint.databinding.ActivityVerMemoBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Actividad para visualizar un memo
 */
class VerMemoActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener  {

    // Declaración de variables
    private lateinit var binding: ActivityVerMemoBinding
    private lateinit var imageView: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val coleccionAudios = db.collection("memos")
    private lateinit var id: String

    lateinit var mVideoView: VideoView
    lateinit var mediaController: MediaController

    private var mediaPlayer: MediaPlayer? = null

    // Para controlar el estado de reproducción
    private var currentPosition: Int = 0
    private var isVideoPlaying: Boolean = false

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
        id = intent.getStringExtra("id").toString()
        val comentarios = intent.getStringExtra("comentarios")
        val localizacion = intent.getStringExtra("localizacion")
        val foto = intent.getStringExtra("foto")
        val video = intent.getStringExtra("video")
        val audio = intent.getStringExtra("audio")

        Glide.with(binding.imageView.context)
            .load(foto)
            .into(binding.imageView)

        mVideoView = binding.videoView!!

        mVideoView?.setOnPreparedListener {
            mVideoView?.start()
            mVideoView?.requestFocus()
            ponerControles(mVideoView!!)
            binding.botonReproducirVideo?.isEnabled = false
        }

        mVideoView?.setOnCompletionListener {
            binding.botonReproducirVideo?.isEnabled = true
        }


        binding.botonReproducirVideo?.setOnClickListener {
            if (video != null) {
                cargarMultimedia(video)
            }
        }

        controlSonido(Uri.parse(audio))

        binding.txtComentario?.setText(comentarios)


        Log.d("cargarFoto", "Antes $foto")
        //foto(foto)

        binding.seekBar?.isEnabled = false

        binding.pauseButton?.setOnClickListener{     // Acción al pulsar el botón de pausa
            if (mediaPlayer != null){                      // Si está cargado el mediaPlayer
                if (mediaPlayer!!.isPlaying()){            // Si está ejecutándose lo pausamos
                    mediaPlayer!!.pause()
                } else {                                   // Si no está ejecutándose es porque está en pausa, lo volvemos a iniciar
                    mediaPlayer!!.start()
                }
            }
        }

        binding.stopButton?.setOnClickListener{      // Acción al pulsar el botón de Stop
            pararReproductor()
        }


        binding.seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{        // Controles de la seekBar
            // Control del avance por parte del usuario
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {                             // Si el cambio ha sido por pulsación del usuario
                    mediaPlayer!!.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityVerMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Cargar multimedia
     *
     * @param video
     */
    fun cargarMultimedia(video: String) {
        mVideoView.setVideoURI(Uri.parse(video))
    }

    /**
     * Poner controles al video
     *
     * @param videoView
     */
    private fun ponerControles(videoView: VideoView) {
        mediaController = MediaController(this)

        //indicamos en donde queremos poner (anclar) ese controlador
        mediaController.setAnchorView(videoView as View)
        videoView.setMediaController(mediaController)
    }

    /**
     * Prepara los eventos para todos los botones de la interfaz
     *
     * @param id Identificador del audio a reproducir
     */
    private fun controlSonido(id: Uri?){ //#Paso 3 - el id ahora es una uri

        binding.playButton?.isEnabled  = true

        binding.playButton?.setOnClickListener{      // Acción al pulsar el botón del play
            iniciarReproduccion(id)
        }

        binding.pauseButton?.setOnClickListener{     // Acción al pulsar el botón de pausa
            if (mediaPlayer != null){                      // Si está cargado el mediaPlayer
                if (mediaPlayer!!.isPlaying()){            // Si está ejecutándose lo pausamos
                    mediaPlayer!!.pause()
                } else {                                   // Si no está ejecutándose es porque está en pausa, lo volvemos a iniciar
                    mediaPlayer!!.start()
                }
            }
        }

        binding.stopButton?.setOnClickListener{      // Acción al pulsar el botón de Stop
            pararReproductor()
        }


        binding.seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{        // Controles de la seekBar
            // Control del avance por parte del usuario
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {                             // Si el cambio ha sido por pulsación del usuario
                    mediaPlayer!!.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

    }

    /**
     * Iniciar reproduccion
     *
     * @param id Identificador del audio a reproducir
     */
    private fun iniciarReproduccion(id: Uri?){

        if (mediaPlayer==null){                                 // Si MediaPlayer no está inicializado
            mediaPlayer = MediaPlayer.create(this, id)   // Creamos el MediaPlayer para el id del audio a reproducir
            mediaPlayer!!.setOnCompletionListener(this)         // Le ponemos el listener para cuando finalice la reproducción
            inicializarSeekBar()                                // Inicializamos la barra de progreso
            mediaPlayer!!.start()                               // Comenzamos la reproducción
            binding.playButton?.isEnabled = false                // Habilitamos y deshabilitamos los botones
            binding.stopButton?.isEnabled = true
            binding.pauseButton?.isEnabled = true
            binding.seekBar?.isEnabled = true
        }
    }

    /**
     * Parar reproductor
     * Para el reproductor y libera los recursos
     */
    private fun pararReproductor(){
        if (mediaPlayer !=null){
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null

            binding.seekBar?.isEnabled = false
            binding.playButton?.isEnabled = true
            binding.stopButton?.isEnabled = false
            binding.pauseButton?.isEnabled = false
        }
    }

    /**
     * Inicializar seek bar
     * Ejecuta en un hilo la actualización de la barra de progreso
     */
    private fun inicializarSeekBar(){
        binding.seekBar?.max = mediaPlayer!!.duration    // Establecemos la duración máxima de la seekbar

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(
            object: Runnable { //Parámetro 1: Lo que queremos ejecutar en segundo plano
                override fun run() {
                    try {
                        binding.seekBar?.progress = mediaPlayer!!.currentPosition             // Se sitúa la seekbar en la posición del mediaPlayer
                        handler.postDelayed(this, 1000)                          // Volvemos a ejecutar pasado 1 segundo
                    } catch (e: Exception){
                        binding.seekBar?.progress = 0
                    }
                }
            }
            , 0) //Parámetro 2: El tiempo de retardo para que se ejecute parámetro 1
    }

    /**
     * Método para cerrar la actividad actual
     */
    fun volver() {
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (mVideoView.isPlaying) {
            currentPosition = mVideoView.currentPosition
            mVideoView.pause()
            isVideoPlaying = true
        } else {
            isVideoPlaying = false
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt("currentPosition", currentPosition)
        bundle.putBoolean("isVideoPlaying", isVideoPlaying)
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onRestoreInstanceState(bundle)
        currentPosition = bundle.getInt("currentPosition")
        isVideoPlaying = bundle.getBoolean("isVideoPlaying")
    }

    override fun onResume() {
        super.onResume()
        if (isVideoPlaying) {
            mVideoView.seekTo(currentPosition)
            mVideoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pararReproductor()
    }

    /**
     * On completion
     *
     * @param mp
     */
    override fun onCompletion(mp: MediaPlayer?) {
        pararReproductor()
    }
}


