package com.example.joypoint

/**
 * Clase que se usará para visulizar cada memo con detalle
 */
data class Memo (
    var id: Int = 0,
    val comentarios: String="",
    val localizacion: String="",
    val acompanado: String="",
    val sentimiento: String="",
    val foto: String=""
)