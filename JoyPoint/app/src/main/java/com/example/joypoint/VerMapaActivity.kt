package com.example.joypoint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.joypoint.databinding.ActivityVerMapaBinding
import com.example.joypoint.databinding.ActivityVerMemoBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URLEncoder
import java.util.LinkedList

class VerMapaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerMapaBinding
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private val PETICION_PERMISOS_OSM = 0

    private lateinit var id: String
    private lateinit var comentario: String
    private lateinit var localizacion: String
    private lateinit var titulo: String
    private lateinit var foto: String
    private lateinit var video: String
    private lateinit var audio: String

    private lateinit var locListener: LocationListener
    private lateinit var locManager: LocationManager
    private lateinit var map : MapView
    private lateinit var mLocationOverlay: MyLocationNewOverlay
    private var mapaActualEsMapnik = true
    private var estaActivada = true

    /**
     * Para dibujar las lineas y marcadores y rutas
     *
     */
    private lateinit var posicion_new: GeoPoint
    private lateinit var posicion_old: GeoPoint
    private lateinit var marker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mapa)

        crearObjetosDelXml()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)== PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)== PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET
                ),
                PETICION_PERMISOS_OSM
            )
        }

        val intent = intent
        id = intent.getStringExtra("id").toString()
        comentario = intent.getStringExtra("comentarios").toString()
        localizacion = intent.getStringExtra("localizacion").toString()
        titulo = intent.getStringExtra("tituloLoc").toString()
        foto = intent.getStringExtra("foto").toString()
        video = intent.getStringExtra("video").toString()
        audio = intent.getStringExtra("audio").toString()

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

        map = binding.map

        binding.buttonBuscar.setOnClickListener {
            val coordenadas = localizacion.split(" ")

            if (coordenadas.size == 2) {
                val latitud = coordenadas[0].toDouble()
                val longitud = coordenadas[1].toDouble()

                // Verifica que el mapa no sea nulo
                if (::map.isInitialized) {
                    val marcadorGeoPoint = GeoPoint(latitud, longitud)
                    // Animar el mapa a la ubicación del marcador
                    map.controller.animateTo(marcadorGeoPoint, 18.0, 500L)
                }
            } else {
                Toast.makeText(this, "Formato de ubicación del marcador incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonBrujula.setOnClickListener {
            var mCompassOverlay =
                CompassOverlay(this, InternalCompassOrientationProvider(this), map)
            mCompassOverlay.enableCompass()
            map.getOverlays().add(mCompassOverlay)
            binding.buttonBrujula.isEnabled = false
        }

        binding.buttonRuta.setOnClickListener{
            if(estaActivada == false){
                habilitarLocalizacion()
            }else{
                pararLocalizacion()
            }
            estaActivada=!estaActivada
        }

        generarMapa()
        habilitarLocalizacion()
        quitarRepeticionYLimitarScroll()
        añadirMarcadorConAccion(localizacion, titulo, comentario)
    }

    /**
     * Generar mapa y poner punto de inicio
     *
     */
    private fun generarMapa() {
        Configuration.getInstance().load(this, getSharedPreferences(packageName+"osmdroid", Context.MODE_PRIVATE))

        //para limitar el zoom minimo  (que no se ponga pequeño)
        map.minZoomLevel = 4.0
        map.controller.setZoom(12.0)

        //para que vaya a sun determinado punto
        //var startPoint = GeoPoint(389.902, -39.200)
        //map.controller.setCenter(startPoint)

        //decimos que siempre aparezcan los botones de zoom
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)

        //para que se pueda hacer zoom con los dedos
        map.setMultiTouchControls(true)

        //para cambiar el mapa (visual)
        map.setTileSource(TileSourceFactory.MAPNIK)
        //habilitarMiLocalizacion()
    }

    /**
     * Habilitar mi localizacion actual y seguirla
     *
     */
    private fun habilitarMiLocalizacion() {
        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)

        //esto solo pone un simbolo
        mLocationOverlay.enableMyLocation()

        //para que la pantallase mueva hasta la localizacion
        mLocationOverlay.enableFollowLocation()

        map.getOverlays().add(mLocationOverlay)

    }

    /**
     * Añadir marcador con accion (pulsacion corta: abre la actividad para ver el memo. pulsacion larga: abre google maps con la ubicacion)
     *
     * @param loc
     * @param titulo
     * @param contenido
     * @return
     */
    private fun añadirMarcadorConAccion(loc: String, titulo: String, contenido: String): ItemizedIconOverlay<OverlayItem> {
        val coordenadas = loc.split(" ")

        if (coordenadas.size != 2) {
            // Manejar el caso en que la cadena de localización no tiene el formato esperado
            // Puedes mostrar un mensaje de error, lanzar una excepción, etc.
            return ItemizedIconOverlay(ArrayList(), null, map.context)
        }

        val latitud = coordenadas[0].toDouble()
        val longitud = coordenadas[1].toDouble()
        val posicion_new = GeoPoint(latitud, longitud)

        // Crear el marcador para la nueva ubicación
        val marker = Marker(map)
        marker.position = posicion_new
        marker.title = titulo
        marker.snippet = contenido
        marker.icon = ContextCompat.getDrawable(map.context, android.R.drawable.ic_menu_camera)

        // Crear el OverlayItem con el marcador
        val overlayItem = OverlayItem(titulo, contenido, posicion_new)
        overlayItem.setMarker(marker.icon)

        // Crear una lista para almacenar los overlays
        val overlayList = ArrayList<ItemizedIconOverlay<OverlayItem>>()

        // Agregar el OverlayItem a la lista
        overlayList.add(ItemizedIconOverlay(
            listOf(overlayItem),
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    openVerMemo()
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    val uri = "geo:$latitud,$longitud?q=$latitud,$longitud"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        // Abre el enlace en el navegador web del dispositivo
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/place/$latitud,$longitud"))
                        startActivity(browserIntent)
                    }

                    return true
                }
            },
            map.context
        ))

        // Agregar la lista de overlays al mapa
        map.overlays.addAll(overlayList)

        // Devolver el último overlay añadido
        return overlayList.last()
    }

    /**
     * Habilitar localizacion a seguir para pintar la ruta
     *
     */
    @SuppressLint("MissingPermission")
    fun habilitarLocalizacion() {
        habilitarMiLocalizacion()
        locManager = this.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        val loc: Location? = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //le avisamos al listener de los cambio de ubicacion
        locListener = LocationListener{
                location -> pintarRuta(location)//pintamos la ruta
        }
        //minTimeMs -> indidca la ferecuentcia con la que se obtiene la localizacion en milisegundos
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0f, locListener)
    }

    /**
     * Va añadiendo posiciones y llama a una funcion para pintar la linea
     *
     * @param loc
     */
    private fun pintarRuta(loc: Location?) {

        val geoPoints: ArrayList<GeoPoint> = ArrayList() //+
        var contenido:String//+
        marker = Marker(map)


        if(loc != null) {
            if (!::posicion_new.isInitialized) { //comprobamos si la variable se ha inicializado o no
                posicion_new = GeoPoint(loc.latitude, loc.longitude) //el mapa trabaja con coordenadas geoPoint

                contenido = loc.latitude.toString()+" "+loc.longitude.toString()//+
                añadirMarcador(posicion_new, "Punto", contenido)//+
            } else {
                posicion_old = posicion_new
                posicion_new = GeoPoint(loc.latitude, loc.longitude)

                geoPoints.add(posicion_old)//+
                geoPoints.add(posicion_new)//+
                moverMarcador(posicion_new)
            }
            var contenido = loc.latitude.toString()+" "+loc.longitude.toString()

            añadirMarcador(posicion_new, "Punto", contenido)
            moverAPosicion(posicion_new, 15.5, 1, 29f, false)

            pintarLinea(geoPoints)
            moverAPosicion(posicion_new, 15.5, 1, 29f, false)
        }
    }

    /**
     * Pintar linea con los puntos de los parametros
     *
     * @param geoPoints
     */
    private fun pintarLinea(geoPoints: ArrayList<GeoPoint>) {
        val line = Polyline()
        line.setPoints(geoPoints)

        map.overlayManager.add(line)
    }

    /**
     * Mover a posicion siguiendo mi ubicacion
     *
     * @param latlngP
     * @param zoomP
     * @param speedP
     * @param orientacionP
     * @param tiltP
     */
    private fun moverAPosicion(
        latlngP: GeoPoint,
        zoomP:Double,
        speedP: Long,
        orientacionP: Float,
        tiltP: Boolean) {

        map.controller.animateTo(latlngP, zoomP, speedP, orientacionP, tiltP)

    }

    private fun añadirMarcador(
        posicion_new: GeoPoint,
        tituloP: String,
        contenidoP: String) {

        var marker = Marker(map)
        marker.position = posicion_new
        marker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_compass)

        marker.title = tituloP
        marker.snippet = contenidoP
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(marker)
        map.invalidate()

    }

    private fun moverMarcador(posicion_new: GeoPoint) {
        val t = LinkedList(map.overlays)
        for (o in t) {
            if (o is Marker) {
                map.overlays.remove(o) //con esto eliminamos la capa
            }
        }

        marker.setPosition(posicion_new)
        map.getOverlays().add(marker)
        map.invalidate()
    }

    /**
     * Abre la activity ver memo
     *
     */
    fun openVerMemo() {
        val intent = Intent(this, VerMemoActivity::class.java)

        intent.putExtra("id", id)
        intent.putExtra("comentarios", comentario)
        intent.putExtra("localizacion", localizacion)
        intent.putExtra("foto", foto)
        intent.putExtra("video", video)
        intent.putExtra("audio", audio)

        activityResultLauncher.launch(intent)
    }

    /**
     * Intercala el mapa entre dos opciones
     *
     * @param view
     */
    fun cambiarMapa(view: View) {
        if (mapaActualEsMapnik) {
            map.setTileSource(TileSourceFactory.OpenTopo)
        } else {
            map.setTileSource(TileSourceFactory.MAPNIK)
        }

        // Actualizar el estado actual del mapa
        mapaActualEsMapnik = !mapaActualEsMapnik
    }

    /**
     * Evita que el mapa se repita y que se pueda salir del mismo
     *
     */
    private fun quitarRepeticionYLimitarScroll() {
        map.isHorizontalMapRepetitionEnabled = false
        map.isVerticalMapRepetitionEnabled = false

        //tener cuidado con
        map.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude,
            MapView.getTileSystem().minLatitude,
            0
        )
        map.setScrollableAreaLimitLongitude(
            MapView.getTileSystem().minLongitude,
            MapView.getTileSystem().maxLongitude,
            0
        )
    }

    /**
     * Pedir permisos
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val permissionsToRequest = ArrayList<String>()
        var i = 0
        //comprobamos resultado por resultado lo que hemos obtenido
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        //solicitamos los permisos al usuario, independientemente del numero
        if (permissionsToRequest.size>0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PETICION_PERMISOS_OSM
            )
        }
        //modificacion pendiente de, segun que permisos falten, bloquear botones
    }

    /**
     * Deja de compartir la ubicacion
     *
     */
    private fun pararLocalizacion() {
        //le decimos al manager que queremos dejar de recibir sus updates
        if(::locManager.isInitialized){
            locManager.removeUpdates(locListener)
        }

        if(::mLocationOverlay.isInitialized){
            mLocationOverlay.disableMyLocation()
        }
    }

    override fun onStop() {
        super.onStop()
        pararLocalizacion()
        map.overlays.clear()  // Limpia los overlays al salir de la actividad
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        //habilitarMiLocalizacion()
        añadirMarcadorConAccion(localizacion, titulo, comentario)
    }

    private fun crearObjetosDelXml() {
        binding = ActivityVerMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}