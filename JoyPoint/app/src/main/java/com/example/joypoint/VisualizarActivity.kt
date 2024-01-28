package com.example.joypoint

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.joypoint.FireStore.OperacionesFireStore
import com.example.joypoint.adapter.MemoAdapter
import com.example.joypoint.databinding.ActivityVisualizarBinding
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Actividad que muestra y permite gestionar la visualización de los memos
 */
class VisualizarActivity : AppCompatActivity() {

    // Declaración de variables
    private lateinit var binding: ActivityVisualizarBinding
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var activityResultPreferencias: ActivityResultLauncher<Intent>
    private lateinit var operacionesFireStore: OperacionesFireStore
    private var posicion: Int = 0

    // Inicialización de Firebase Firestore
    private val db = FirebaseFirestore.getInstance()
    private val myCollection = db.collection("memos")

    // Proveedores y adaptadores para la lista de "memos"
    private lateinit var memosProvider: MemoProvider
    private lateinit var memosAdapter: MemoAdapter
    val managerMemo = LinearLayoutManager(this)

    //Sonidos en botones
    lateinit var soundPool: SoundPool

    var pop: Int = 0

    /**
     * Método llamado al crear la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creación de objetos del archivo de diseño XML
        crearObjetosDelXml()

        // Configuración de la toolbar
        setSupportActionBar(binding.appbar.toolbarApp)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Obtención del nombre desde la actividad anterior
        val nombre = intent.getStringExtra("nombre")
        val mensaje = resources.getString(R.string.saludo, nombre)
        val textView = TextView(this)
        textView.text = mensaje
        textView.setTypeface(Typeface.create("baloo", Typeface.BOLD))

        // Configuración del mensaje de la toolbar
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.customView = textView

        // Inicialización del RecyclerView
        initRecyclerView()

        // Inicialización de operaciones con Firebase Firestore
        operacionesFireStore = OperacionesFireStore("memos", memosProvider, memosAdapter, managerMemo)
        val textLocalizacion = findViewById<EditText>(R.id.editLocalizacion)

        binding.btnBuscar.setOnClickListener {
            buscarMemosPorLocalizacion(binding.editLocalizacion.text.toString())
        }

        textLocalizacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString()
                //btnAnadir.isEnabled = inputText.isEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })


        // Carga de preferencias al iniciar la actividad
        loadPref()

        // Registro de lanzadores de actividad para resultados
        activityResultPreferencias =        //Launcher de las preferencias, diferente al launcher para la RecyclerView
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                loadPref()
            }

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

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
                if (resultado.data != null) {
                    val datos: Intent = resultado.data!!
                    val id: Int

                    if (datos.hasExtra("id")) {
                        id = datos.getStringExtra("id")!!.toInt()
                    } else {
                        id = memosProvider.getId()
                    }
                    var memo = Memo(
                        id,
                        datos.getStringExtra("comentarios")!!,
                        datos.getStringExtra("localizacion")!!,
                        datos.getStringExtra("tituloLoc")!!,
                        datos.getStringExtra("foto")!!,
                        datos.getStringExtra("video")!!,
                        datos.getStringExtra("audio")!!
                    )
                    if (datos.hasExtra("id")) {
                        operacionesFireStore.updateRegister(posicion, memo)
                    } else {
                        operacionesFireStore.insertRegister(memo)
                    }
                }
            }

    }

    /**
     * Infla el diseño de la actividad desde el binding
     */
    private fun crearObjetosDelXml() {
        binding = ActivityVisualizarBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Crea y muestra el menú de opciones en la barra de acción
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_visualizar, menu)
        return true
    }

    /**
     * Carga las preferencias del usuario
     */
    private fun loadPref() {
        val misPreferencias = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

        //Guardamos datos por defecto
        val preferenciaAcompanado = misPreferencias.getString("acompanado", "")
        val preferenciaSentimiento = misPreferencias.getString("sentimiento", "")
        val elegirPreferencias = misPreferencias.getString("noPref", "false")

        filtrarRegistros(preferenciaAcompanado, preferenciaSentimiento, elegirPreferencias.toString())
    }

    /**
     * Funcion para indicar el sonido de los botones
     *
     */
    fun playSound(){
        soundPool.play(pop, 10F, 10F, 1, 0, 1F)
    }

    /**
     * Filtra los registros según las preferencias del usuario
     */
    private fun filtrarRegistros(
        preferenciaAcompanado: String?,
        preferenciaSentimiento: String?,
        elegirPreferencias: String
    ) {
        if (elegirPreferencias.equals("false")) {
            myCollection
                .get()
                .addOnSuccessListener { resultado ->
                    memosProvider.memosList.clear()
                    memosProvider.actualizarLista(resultado)
                    memosAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error al cargar todos los memos: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            if (!preferenciaSentimiento.isNullOrEmpty()) {
                myCollection
                    .whereEqualTo("sentimiento", preferenciaSentimiento)
                    .get()
                    .addOnSuccessListener { resultado ->
                        val filtrados = if (!preferenciaAcompanado.isNullOrEmpty()) {
                            resultado.documents.filter { documento ->
                                documento.get("acompanado") == preferenciaAcompanado
                            }
                        } else {
                            resultado.documents
                        }

                        memosProvider.memosList.clear()
                        memosProvider.actualizarLista(filtrados)
                        memosAdapter.notifyDataSetChanged()
                    }
            } else if (!preferenciaAcompanado.isNullOrEmpty()) {
                myCollection
                    .whereEqualTo("acompanado", preferenciaAcompanado)
                    .get()
                    .addOnSuccessListener { resultado ->
                        memosProvider.memosList.clear()
                        memosProvider.actualizarLista(resultado)
                        memosAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Error al buscar los memos: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    /**
     * Abre la actividad de edición con los datos del memo elegido
     */
    fun openActivityEditar(posicion: Int, memo: Memo) {
        val intent = Intent(this, AnadirActivity::class.java)

        this.posicion = posicion

        intent.putExtra("id", memo.id.toString())
        intent.putExtra("comentarios", memo.comentarios)
        intent.putExtra("localizacion", memo.localizacion)
        intent.putExtra("tituloLoc", memo.tituloLoc)
        intent.putExtra("foto", memo.foto)
        intent.putExtra("video", memo.video)
        intent.putExtra("audio", memo.audio)

        activityResultLauncher.launch(intent)
    }

    /**
     * Abre la actividad de visualización detallada del memo elegido
     */
    fun openVerMemo(posicion: Int, memo: Memo) {
        val intent = Intent(this, VerMemoActivity::class.java)

        this.posicion = posicion

        intent.putExtra("id", memo.id)
        intent.putExtra("comentarios", memo.comentarios)
        intent.putExtra("localizacion", memo.localizacion)
        intent.putExtra("foto", memo.foto)
        intent.putExtra("video", memo.video)
        intent.putExtra("audio", memo.audio)

        activityResultLauncher.launch(intent)
    }

    fun openVerMapa(posicion: Int, memo: Memo) {
        val intent = Intent(this, VerMapaActivity::class.java)

        this.posicion = posicion

        intent.putExtra("id", memo.id)
        intent.putExtra("comentarios", memo.comentarios)
        intent.putExtra("localizacion", memo.localizacion)
        intent.putExtra("tituloLoc", memo.tituloLoc)
        intent.putExtra("foto", memo.foto)
        intent.putExtra("video", memo.video)
        intent.putExtra("audio", memo.audio)

        activityResultLauncher.launch(intent)
    }

    /**
     * Maneja las selecciones de elementos en el menú de opciones
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.anadir -> {
                openActivityAnadir()
                true
            }

            R.id.galeria -> {
                openActivityGaleria()
                true
            }

            R.id.preferencias -> {
                abrirPreferencias()
                activityResultPreferencias
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
     * Abre la actividad para añadir un nuevo memo
     */
    fun openActivityAnadir() {
        val intent = Intent(this, AnadirActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    /**
     * Abre la actividad de la galería de fotos
     */
    fun openActivityGaleria() {
        val intent = Intent(this, GaleriaActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    /**
     * Abre la actividad de preferencias
     */
    fun abrirPreferencias() {
        val intent = Intent(this, SettingsActivity::class.java)
        activityResultPreferencias.launch(intent)
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
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.avancepsicologos.com/teoria-personalidad")
            )
        )
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
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hola, ésto es un mensaje de prueba para la asignatura de ProgMult"
                )
                // Declaramos un array en el que podemos meter varias direcciones
                putExtra(Intent.EXTRA_EMAIL, arrayOf("silvitaat02@gmail.com"))
            }
        )
    }

    /**
     * Busca memos por una ubicación específica en Firebase Firestore
     */
    private fun buscarMemosPorLocalizacion(localizacion: String) {
        val localizacionAux = localizacion.uppercase()

        if (localizacion.isNotBlank()) {
            myCollection
                .whereEqualTo("localizacion", localizacionAux)
                .get()
                .addOnSuccessListener { resultado ->
                    memosProvider.memosList.clear()
                    memosProvider.actualizarLista(resultado)
                    memosAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error al buscar los memos: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Si la ubicación está vacía, muestra todos los "memos" en la lista
            myCollection
                .get()
                .addOnSuccessListener { resultado ->
                    memosProvider.memosList.clear()
                    memosProvider.actualizarLista(resultado)
                    memosAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error al cargar todos los memos: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Inicializa el RecyclerView y carga todos los memos desde Firebase Firestore
     */
    private fun initRecyclerView() {
        val decoration = DividerItemDecoration(this, managerMemo.orientation)
        binding.recyclerMemos.layoutManager = managerMemo
        memosProvider = MemoProvider()
        memosAdapter = MemoAdapter(
            memosList = memosProvider.memosList,
            deleteRegister = { posicion: Int, id: Int ->
                operacionesFireStore.deleteRegister(
                    posicion,
                    id
                )
            },
            openActivityEditar = { posicion: Int, memo: Memo -> openActivityEditar(posicion, memo) },
            openVerMemo = { posicion: Int, memo: Memo -> openVerMemo(posicion, memo) },
            openVerMapa = { posicion: Int, memo: Memo -> openVerMapa(posicion, memo) },
            playSound = { -> playSound()}//Funciones lambda: Indicamos los tipos de los parámetros de entrada y luego se lo indicamos con ->
        )

        myCollection
            .get()
            .addOnSuccessListener { resultado ->
                memosProvider.actualizarLista(resultado)
                binding.recyclerMemos.adapter = memosAdapter
                binding.recyclerMemos.addItemDecoration(decoration)
            }
    }
}
