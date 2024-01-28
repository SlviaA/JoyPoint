package com.example.joypoint

/**
 * Clase que se usará para visulizar cada memo con detalle
 */
data class Memo (
    var id: Int = 0,
    val comentarios: String="",
    val localizacion: String="",
    val tituloLoc: String="",
    var foto: String="",
    var video: String="",
    var audio: String=""
)