package com.example.joypoint

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.joypoint.databinding.ActivityAnadirBinding
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextMenu
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

/**
 * Actividad para añadir o editar un memo
 */
class AnadirActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener {

    // Declaración de variables
    private lateinit var binding: ActivityAnadirBinding
    private lateinit var idMemo: String
    private val INSERTANDO_MODIFICANDO: Int = 1
    private lateinit var objetoIntent: Intent
    private val CHANNEL_ID: String = "123"
    val PETICION_PERMISO_GRABACION = 0

    //1) carga imagen de la galeria
    lateinit var activityResultLauncherCargarImagenDeGaleria: ActivityResultLauncher<Intent>
    lateinit var activityResultLauncherCargarVideoDeGaleria: ActivityResultLauncher<Intent>
    lateinit var activityResultLauncherCargarAudio: ActivityResultLauncher<Intent>
    lateinit var activityResultLauncherGrabarVideo: ActivityResultLauncher<Intent>
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    var fotoUri: Uri? = null
    var videoUri: Uri? = null
    var audioUri: Uri? = null

    lateinit var mVideoView: VideoView
    lateinit var mediaController: MediaController

    private var mediaPlayer: MediaPlayer? = null

    private var ubicacionMemo: String? = null
    private var tituloMemo: String? = null

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearObjetosDelXml()
        //createNotificationChannel()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {


            // Si están denegados, los volvemos a pedir
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                // Como tercer parámetro se pasa la constante con la que identificamos los permisos solicitados
                PETICION_PERMISO_GRABACION
            )
        }

        objetoIntent = intent

        // Verificar si se abre la activity con un id agregado, en caso de que si se haya agragado se añaden automaticamente los datos del memo
        if (objetoIntent.hasExtra("id")) {
            idMemo = objetoIntent.getStringExtra("id")!!
            binding.editComentario.setText(objetoIntent.getStringExtra("comentarios"))
            binding.txtUbicacion?.setText(objetoIntent.getStringExtra("localizacion"))
            fotoUri = Uri.parse(objetoIntent.getStringExtra("foto")) //en caso de editar pero no modificar la foto, inicializamos la uri
            videoUri = Uri.parse(objetoIntent.getStringExtra("video"))
            audioUri = Uri.parse(objetoIntent.getStringExtra("audio"))


            binding.imageView?.let {
                Glide.with(it)
                    .load(fotoUri)
                    .into(binding.imageView!!)
            }

            mVideoView = binding.videoView!!

            mVideoView.setOnPreparedListener {
                mVideoView.start()
                mVideoView.requestFocus()
                ponerControles(mVideoView)
                binding.btnVideo?.isEnabled = false
            }

            //habilitamos el boton para cuando termine de reproducirse
            mVideoView.setOnCompletionListener {
                binding.btnVideo?.isEnabled = true
            }

            cargarMultimedia(videoUri!!)

            controlSonido(audioUri)
        }

        // Configuración de los botones
        binding.btnCancelar.setOnClickListener {
            volver()
        }

        binding.btnGuardar.setOnClickListener {
            volverRetornandoDatos()
        }

        binding.btnFoto!!.setOnClickListener{
            cargarImagen()
        }
        activityResultLauncherCargarImagenDeGaleria =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result ->

                if (result.data != null){
                    val data: Intent = result.data!!

                    if (result.resultCode == RESULT_OK){
                        fotoUri = data!!.data
                        binding.imageView?.let {
                            Glide.with(it.context)
                                .load(fotoUri)
                                .into(binding.imageView!!)
                        }
                    }
                }
            }

        mVideoView = binding.videoView!!

        mVideoView.setOnPreparedListener {
            mVideoView.start()
            mVideoView.requestFocus()
            ponerControles(mVideoView)
            binding.btnVideo?.isEnabled = false
        }

        //habilitamos el boton para cuando termine de reproducirse
        mVideoView.setOnCompletionListener {
            binding.btnVideo?.isEnabled = true
        }

        binding.btnVideo!!.setOnClickListener{
            cargarVideo()
        }
        activityResultLauncherCargarVideoDeGaleria =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result ->

                if (result.data != null){
                    val data: Intent = result.data!!

                    if (result.resultCode == RESULT_OK){
                        videoUri = data!!.data
                        videoUri?.let { cargarMultimedia(it) }
                    }
                }
            }

        binding.btnAudio!!.setOnClickListener{
            cargarAudio()
        }
        activityResultLauncherCargarAudio =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result ->

                if (result.data != null){
                    Log.d("urlUri", "ACTIVITY")
                    val data: Intent = result.data!!

                    if (result.resultCode == RESULT_OK){
                        audioUri = data!!.data
                        controlSonido(audioUri)
                    }
                }
            }

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data != null) {
                val data: Intent? = result.data
                ubicacionMemo = data?.getStringExtra("ubicacion")
                tituloMemo = data?.getStringExtra("titulo")
                Log.d("mapas", "al retornar: $ubicacionMemo")
                Log.d("mapas", "al retornar: $tituloMemo")
                actualizarTextViewConUbicacion()
            }
        }

        activityResultLauncherGrabarVideo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if (result.data != null) {
                val data: Intent = result.data!!

                // Si el activityResultLauncher tiene datos, se asigna la Uri al videoView
                if (result.resultCode == RESULT_OK) {
                    videoUri = data!!.data
                    videoUri?.let { cargarMultimedia(it) }
                }
            }
        }
        binding.btnMapa?.setOnClickListener {
            openActivityMapa()
        }
    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityAnadirBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun actualizarTextViewConUbicacion() {
        // Actualizar el TextView con la ubicación actual si está disponible
        if (ubicacionMemo != null) {
            /*val latitud = ubicacionMemo!!.latitude
            val longitud = ubicacionMemo!!.longitude
            val textoUbicacion = "Latitud: $latitud, Longitud: $longitud"*/
            binding.txtUbicacion?.setText(ubicacionMemo)
        }
    }

    /**
     * Cargar multimedia
     *
     * @param video
     */
    fun cargarMultimedia(video: Uri) {
        mVideoView.setVideoURI(video)
    }

    /**
     * Poner controles al video
     *
     * @param videoView
     */
    private fun ponerControles(videoView: VideoView) {
        mediaController = MediaController(this)

        //indicamos en donde queremos poner (anclar) ese controlador
        mediaController.setAnchorView(videoView as View) //(videoView.parent as View) //así le ponemos los controles al que contiene
        videoView.setMediaController(mediaController)
    }

    /**
     * Control sonido introducido por parametro
     *
     * @param id
     */
    private fun controlSonido(id: Uri?) { //#Paso 3 - el id ahora es una uri

        binding.playButton?.isEnabled = true

        binding.playButton?.setOnClickListener {      // Acción al pulsar el botón del play
            iniciarReproduccion(id)
        }

        binding.pauseButton?.setOnClickListener {     // Acción al pulsar el botón de pausa
            if (mediaPlayer != null) {                      // Si está cargado el mediaPlayer
                if (mediaPlayer!!.isPlaying()) {            // Si está ejecutándose lo pausamos
                    mediaPlayer!!.pause()
                } else {                                   // Si no está ejecutándose es porque está en pausa, lo volvemos a iniciar
                    mediaPlayer!!.start()
                }
            }
        }
    }

    /**
     * Iniciar reproduccion
     *
     * @param id
     */
    private fun iniciarReproduccion(id: Uri?){

        if (mediaPlayer==null){                                 // Si MediaPlayer no está inicializado
            mediaPlayer = MediaPlayer.create(this, id)   // Creamos el MediaPlayer para el id del audio a reproducir
            mediaPlayer!!.setOnCompletionListener(this)         // Le ponemos el listener para cuando finalice la reproducción         // Inicializamos la barra de progreso
            mediaPlayer!!.start()                               // Comenzamos la reproducción
            binding.playButton?.isEnabled = false                // Habilitamos y deshabilitamos los botones
            binding.pauseButton?.isEnabled = true
        }
    }

    /**
     * Parar reproductor
     *
     */
    private fun pararReproductor() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            //mediaPlayer!!.reset() //solo si da problemas al parar
            mediaPlayer!!.release() //importante liberar recursos
            mediaPlayer = null

            binding.playButton!!.isEnabled = true
            binding.pauseButton!!.isEnabled = false
        }
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
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
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
     * Método para cerrar la actividad actual
     */
    fun volver() {
        finish()
    }

    /**
     * Método para cerrar la actividad actual y retornar datos a la actividad anterior
     */
    fun volverRetornandoDatos() {
        Log.d("urlUri", "al clicar aceptar URL: $fotoUri")

        val intent = Intent()

        // Incluir datos en el intent
        if (objetoIntent.hasExtra("id")) {
            intent.putExtra("id", idMemo)
        }
        intent.putExtra("comentarios", binding.editComentario.text.toString())
        intent.putExtra("localizacion", binding.txtUbicacion?.text.toString())
        intent.putExtra("tituloLoc", tituloMemo)
        intent.putExtra("foto", fotoUri.toString())
        intent.putExtra("video", videoUri.toString())
        intent.putExtra("audio", audioUri.toString())


        Log.d("urlUri", "al retornar: $fotoUri")

        // Establecer el resultado y cerrar la actividad
        setResult(INSERTANDO_MODIFICANDO, intent)
        finish()
    }

    /**
     * Elegir imagen de la galeria
     *
     */
    private fun cargarImagen(){
        val intent = Intent()

        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if(intent.resolveActivity(packageManager)!=null){
            activityResultLauncherCargarImagenDeGaleria.launch(intent)
        }
    }

    /**
     * Elegir video de la galeria
     *
     */
    private fun cargarVideo(){
        val intent = Intent()

        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 1)

        if(intent.resolveActivity(packageManager)!=null){
            activityResultLauncherCargarVideoDeGaleria.launch(intent)
        }
    }

    /**
     * Elegir audio de la galeria
     *
     */
    private fun cargarAudio() {
        Log.d("urlUri", "SOCORRO")
        val intent = Intent()

        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "audio/*"
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 1)

        activityResultLauncherCargarAudio.launch(intent)
    }

    fun openActivityMapa() {
        if(!binding.editComentario.text.isNullOrEmpty()){
            val intent = Intent(this, MapaActivity::class.java)

            intent.putExtra("descripcion", binding.editComentario.text.toString())

            activityResultLauncher.launch(intent)
        }else {
            // Handle the case when the location is not available yet
            Toast.makeText(this, "Primero cuenta un poco sobre el recuerdo", Toast.LENGTH_SHORT).show()
        }
    }

    fun comenzarGrabacion(view: View) {
        val intent = Intent()
        // Indico la acción del intent
        intent.action = MediaStore.ACTION_VIDEO_CAPTURE
        if (intent.resolveActivity(packageManager) != null) {
            activityResultLauncherGrabarVideo.launch(intent)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Este método se llama cuando hago cualquier solicitud de permisos
        // Si me está devolviendo el control, tengo que saber por qué solicitud de permisos es
        if (requestCode == PETICION_PERMISO_GRABACION) { // Que sea por los permisos que me he definido yo
            // Si hay un array con dos permisos y ambos han sido aceptados
            if (grantResults.size == 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                binding.buttonGrabar!!.visibility = View.VISIBLE

            } else {
                // Si no se cumple todo ésto, lo desactivamos
                // La app va a seguir funcionando, pero sin ese botón
                binding.buttonGrabar!!.visibility = View.INVISIBLE
            }
        }
    }


    /**
     * On destroy
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        pararReproductor()
    }

    /**
     * On coompletion
     *
     * @param mp
     */
    override fun onCompletion(mp: MediaPlayer?) {
        pararReproductor()
    }

}
