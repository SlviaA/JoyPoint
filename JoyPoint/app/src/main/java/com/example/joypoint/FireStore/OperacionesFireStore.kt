package com.example.joypoint.FireStore

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.joypoint.AnadirActivity
import com.example.joypoint.Memo
import com.example.joypoint.MemoProvider
import com.example.joypoint.adapter.MemoAdapter
import com.google.firebase.firestore.FirebaseFirestore

class OperacionesFireStore (var coleccion: String,
                            var memosProvider: MemoProvider,
                            var memosAdapter: MemoAdapter,
                            var manager: LinearLayoutManager
) {

    // Inicialización de Firebase Firestore
    private var db = FirebaseFirestore.getInstance()
    private var myCollection = db.collection(this.coleccion)

    /**
     * Elimina un memo
     *
     * @param posicion Posición del memo a eliminar
     * @param id Id del memo a eliminar
     */
    fun deleteRegister(posicion: Int, id: Int){


        myCollection
            .document((id).toString())
            .delete()
            .addOnSuccessListener {
                memosProvider.memosList.removeAt(posicion)         // Eliminamos el elemento de la lista
                memosAdapter.notifyItemRemoved(posicion)           // Se lo notificamos al adapter

            }

    }

    /**
     * Modifica un memo
     *
     * @param posicion Posición del memo a actualizar
     * @param id Id del memo a actualizar
     */
    fun updateRegister(posicion: Int, memo: Memo){

        myCollection
            .document(memo.id.toString())
            .set(
                hashMapOf(
                    "comentarios" to memo.comentarios,
                    "localizacion" to memo.localizacion.uppercase(),
                    "acompanado" to memo.acompanado,
                    "sentimiento" to memo.sentimiento,
                    "foto" to memo.foto
                )
            )
            .addOnSuccessListener {
                memosProvider.memosList.set(posicion, memo)             // Con set sustituimos el elemento
                memosAdapter.notifyItemChanged(posicion)                // Con notifyItemChanged le indicamos al adapter el ítem que ha cambiado
                manager.scrollToPositionWithOffset(posicion,10)
            }
    }

    /**
     * Inserta un memo nuevo
     *
     * @param memo El memo a insertar
     */
    fun insertRegister(memo: Memo){
        myCollection
            .document(memo.id.toString())
            .set(
                hashMapOf(
                    "comentarios" to memo.comentarios,
                    "localizacion" to memo.localizacion.uppercase(),
                    "acompanado" to memo.acompanado,
                    "sentimiento" to memo.sentimiento,
                    "foto" to memo.foto
                )
            )
            .addOnSuccessListener {
                memosProvider.memosList.add(memo)                       // Añadimos el producto a la lista
                memosAdapter.notifyItemInserted(memo.id)
                memosAdapter.notifyDataSetChanged()
                manager.scrollToPositionWithOffset(memo.id,10)
            }
    }
}