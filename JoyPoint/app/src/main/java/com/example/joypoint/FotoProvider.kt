package com.example.joypoint

import com.google.firebase.firestore.QuerySnapshot

class FotoProvider {
    var fotosList: MutableList<Foto> = mutableListOf()

    /**
     * Actualiza la lista de memos a partir de un resultado de consulta
     *
     * @param resultado Resultado de una consulta a Firestore que contiene los memos
     */
    fun actualizarLista(resultado: QuerySnapshot) {
        lateinit var foto: Foto

        for (documento in resultado) {
            foto = documento.toObject(Foto::class.java)
            foto.id = documento.id.toInt()
            fotosList.add(foto)
        }
    }

    /**
     * Obtiene el siguiente ID disponible
     *
     * @return el ID conseguido
     */
    fun getId(): Int {
        var posicion = 0
        if(!fotosList.isEmpty()) {
            for (memo in fotosList) {
                if (memo.id == posicion) {
                    posicion = posicion + 1
                }else{
                    break
                }
            }
        }
        return posicion
    }
}