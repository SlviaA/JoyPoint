package com.example.joypoint

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.joypoint.adapter.FotoAdapter
import com.example.joypoint.databinding.ActivityGaleriaBinding
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Actividad para ver solo las fotos de los memos
 */
class GaleriaActivity : AppCompatActivity() {

    // Declaración de variables
    private lateinit var binding: ActivityGaleriaBinding

    // Conexión a Firestore
    private val db = FirebaseFirestore.getInstance()
    private val myCollection = db.collection("memos")

    // Proveedor y adaptador para la galería de fotos
    private lateinit var fotosProvider: FotoProvider
    private lateinit var fotosAdapter: FotoAdapter

    // Administrador de diseño para el RecyclerView
    val managerGaleria = LinearLayoutManager(this)

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_memo)
        // Inflar el diseño de la actividad
        crearObjetosDelXml()

        // Inicializar y configurar el RecyclerView
        initRecyclerView()

        // Configurar el botón de cancelar
        binding.btnCancelar.setOnClickListener {
            volver()
        }
    }

    /**
     * Inflar el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml(){
        binding = ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Inicializar y configurar el RecyclerView para mostrar la galería de fotos
     */
    private fun initRecyclerView() {
        val decoration = DividerItemDecoration(this, managerGaleria.orientation)
        binding.recyclerFoto.layoutManager = managerGaleria
        fotosProvider = FotoProvider()
        fotosAdapter = FotoAdapter(
            fotosList = fotosProvider.fotosList
        )

        // Obtener datos de Firestore y actualizar la lista de fotos
        myCollection
            .get()
            .addOnSuccessListener { resultado ->
                fotosProvider.actualizarLista(resultado)
                binding.recyclerFoto.adapter = fotosAdapter
                binding.recyclerFoto.addItemDecoration(decoration)
            }
    }

    /**
     * Volver a la actividad anterior
     */
    fun volver() {
        finish()
    }
}
