package com.example.joypoint

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

class MemoProvider {

    // Declaración de variables
    var memosList: MutableList<Memo> = mutableListOf()

    /**
     * Actualiza la lista de memos a partir de un resultado de consulta
     *
     * @param resultado Resultado de una consulta a Firestore que contiene los memos
     */
    fun actualizarLista(resultado: QuerySnapshot) {
        lateinit var memo: Memo

        for (documento in resultado) {
            memo = documento.toObject(Memo::class.java)
            memo.id = documento.id.toInt()
            memosList.add(memo)
        }
    }

    /**
     * Actualiza la lista de memos a partir de una lista de resultados filtrados.
     *
     * @param resultadosFiltrados Lista de documentos filtrados que contiene los memos
     */
    fun actualizarLista(resultadosFiltrados: List<DocumentSnapshot>) {
        lateinit var memo: Memo

        for (documento in resultadosFiltrados) {
            memo = documento.toObject(Memo::class.java) ?: Memo()  // Manejo de posibles objetos nulos
            memo.id = documento.id.toInt()
            memosList.add(memo)
        }
    }

    /**
     * Obtiene el siguiente ID disponible
     *
     * @return el ID conseguido
     */
    fun getId(): Int {
        var posicion = 0
        if (!memosList.isEmpty()) {
            for (memo in memosList) {
                if (memo.id == posicion) {
                    posicion = posicion + 1
                } else {
                    break
                }
            }
        }
        return posicion
    }
}