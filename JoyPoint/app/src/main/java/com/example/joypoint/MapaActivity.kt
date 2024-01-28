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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.joypoint.databinding.ActivityMapaBinding
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.LinkedList
import java.util.concurrent.Flow

class MapaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapaBinding
    private lateinit var objetoIntent: Intent

    private val PETICION_PERMISOS_OSM = 0

    private lateinit var map : MapView

    private val INSERTANDO_MODIFICANDO: Int = 1

    private lateinit var descripcion: String

    /**
     * localizacion
     */
    private lateinit var locListener: LocationListener
    //private lateinit var locManager: LocationManager
    private lateinit var mLocationOverlay: MyLocationNewOverlay

    /**
     * Para dibujar las lineas y marcadores y rutas
     *
     */
    private lateinit var marker: Marker

    private var ubicacionActual: GeoPoint? = null
    private var ubicacionElegida: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearObjetosDelXml()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)==PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)==PackageManager.PERMISSION_DENIED) {

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

        objetoIntent = intent

        descripcion = Uri.parse(objetoIntent.getStringExtra("descripcion")).toString()

        map = binding.map

        generarMapa()

        quitarRepeticionYLimitarScroll()

        binding.buttonGuardar.setOnClickListener {
            volverRetornandoDatos(ubicacionElegida!!)
        }
    }

    /**
     * Generar mapa y poner punto de inicio
     *
     */
    private fun generarMapa() {
        getInstance().load(this, getSharedPreferences(packageName+"osmdroid", Context.MODE_PRIVATE))

        //para limitar el zoom minimo  (que no se ponga pequeño)
        map.minZoomLevel = 4.0
        map.controller.setZoom(12.0)

        //para que vaya a sun determinado punto
        var startPoint =GeoPoint(35.6764, 139.6500)
        map.controller.setCenter(startPoint)

        //decimos que siempre aparezcan los botones de zoom
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)

        //para que se pueda hacer zoom con los dedos
        map.setMultiTouchControls(true)

        //para poner la brujula
        var mCompassOverlay =
            CompassOverlay(this, InternalCompassOrientationProvider(this), map)
        mCompassOverlay.enableCompass()
        map.getOverlays().add(mCompassOverlay)

        //para cambiar el mapa (visual)
        //map.setTileSource(TileSourceFactory.MAPNIK)

        habilitarMiLocalizacion()
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
     * Se guarda la ubicacion actual
     *
     * @param view
     */
    fun usarLocalizacion(view: View) {
        if(!binding.editTitulo.text.isNullOrEmpty()){
            if (mLocationOverlay.isMyLocationEnabled && mLocationOverlay.myLocation != null) {
                ubicacionActual = mLocationOverlay.myLocation

                Log.d("mapas", "al pulsar boton: $ubicacionActual")

                volverRetornandoDatos(ubicacionActual.toString())
            } else {
                // Handle the case when the location is not available yet
                Toast.makeText(this, "Ubicación no disponible aún", Toast.LENGTH_SHORT).show()
            }
        }else {
            // Handle the case when the location is not available yet
            Toast.makeText(this, "Introduzca un título primero", Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * Volver retornando datos
     *
     * @param ubicacion
     */
    fun volverRetornandoDatos(ubicacion: String) {
        val intent = Intent()

        intent.putExtra("ubicacion", ubicacion)
        intent.putExtra("titulo", binding.editTitulo.text.toString())

        Log.d("mapas", "antes retornar: $ubicacion")

        // Establecer el resultado y cerrar la actividad
        setResult(INSERTANDO_MODIFICANDO, intent)
        finish()
    }

    /**
     * Al pulsar se añade un marcador
     *
     * @param view
     */
    fun añadirAccionesMapa(view: View) {
        if (!binding.editTitulo.text.isNullOrEmpty()) {
            Log.d("mapas", "al retornar: ${binding.editTitulo.text}")
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(loc: GeoPoint?): Boolean {
                    if (loc != null) {
                        ubicacionElegida = "${loc.latitude} ${loc.longitude}"
                        clearMarkers() // Remove existing markers
                        añadirMarcador(loc, binding.editTitulo.text.toString(), descripcion!!)
                    }
                    return true
                }

                override fun longPressHelper(loc: GeoPoint?): Boolean {
                    if (loc != null) {
                        mostrarDialogo("Coordenadas: ", loc)
                        ubicacionElegida = "${loc.latitude} ${loc.longitude}"
                        clearMarkers() // Remove existing markers
                        map.overlays.add(añadirMarcadorConAccion(loc, binding.editTitulo.toString(), descripcion!!))
                    }
                    return false
                }
            }
            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)

            // Clear existing markers before adding new ones
            clearMarkers()

            map.overlays.add(0, mapEventsOverlay)
            map.invalidate()
            binding.buttonGuardar.isEnabled = true
        } else {
            // Handle the case when the location is not available yet
            Toast.makeText(this, "Introduzca un título primero", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Añadir marcador con accion (pulsacion corta: abre la actividad para ver el memo. pulsacion larga: abre google maps con la ubicacion)
     *
     * @param posicion_new
     * @param tituloP
     * @param contenidoP
     * @return
     */
    private fun añadirMarcadorConAccion(posicion_new: GeoPoint, tituloP: String, contenidoP: String): ItemizedIconOverlay<OverlayItem> {
        return ItemizedIconOverlay(
            ArrayList<OverlayItem>().apply {
                val marker = Marker(map)
                marker.position = posicion_new
                marker.title = tituloP
                marker.snippet = contenidoP
                marker.icon = ContextCompat.getDrawable(map.context, android.R.drawable.ic_menu_camera)

                val overlayItem = OverlayItem(tituloP, contenidoP, posicion_new)
                overlayItem.setMarker(marker.icon)
                add(overlayItem)
            },
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    var geoPoint = GeoPoint(item.point.latitude, item.point.longitude)
                    mostrarDialogo(item.title, geoPoint)
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    return false
                }

            },
            map.context
        )
    }

    /**
     * Permite que solo aparezca un marcador
     *
     */
    private fun clearMarkers() {
        val overlaysToRemove = mutableListOf<Overlay>()
        for (overlay in map.overlays) {
            if (overlay is Marker) {
                overlaysToRemove.add(overlay)
            }
        }
        map.overlays.removeAll(overlaysToRemove)
        map.invalidate()
    }

    /**
     * Se muestra la informacion del marcador
     *
     * @param tituloP
     * @param loc
     */
    fun mostrarDialogo(tituloP: String, loc: GeoPoint) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(tituloP)
            .setMessage("Latitud: "+loc.latitude+"; Longitud: "+loc.longitude)
            .setCancelable(true)
            .show()
    }

    override fun onStop() {
        super.onStop()
        //pararLocalizacion()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    /**
     * Añadir marcador
     *
     */
    private fun añadirMarcador(
        posicion_new: GeoPoint,
        tituloP: String,
        contenidoP: String) {

        var marker = Marker(map)
        marker.position = posicion_new
        marker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)

        marker.title = tituloP
        marker.snippet = contenidoP
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(marker)
        map.invalidate()

    }

    /**
     * On request permissions result
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
     * Evita que el mapa se repita y que se pueda salir del mismo
     *
     */
    private fun quitarRepeticionYLimitarScroll () {
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

    private fun crearObjetosDelXml() {
        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}