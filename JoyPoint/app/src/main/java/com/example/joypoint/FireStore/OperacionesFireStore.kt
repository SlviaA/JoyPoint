package com.example.joypoint.FireStore

import android.net.Uri
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.joypoint.Memo
import com.example.joypoint.MemoProvider
import com.example.joypoint.adapter.MemoAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class OperacionesFireStore (var coleccion: String,
                            var memosProvider: MemoProvider,
                            var memosAdapter: MemoAdapter,
                            var manager: LinearLayoutManager
) {

    // Inicialización de Firebase Firestore
    private var db = FirebaseFirestore.getInstance()
    private var myCollection = db.collection(this.coleccion)
    private var storage = FirebaseStorage.getInstance()
    var downloadUri: Uri? = null

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
                    "tituloLoc" to memo.tituloLoc,
                    "foto" to memo.foto,
                    "video" to memo.video,
                    "audio" to memo.audio
                )
            )
            .addOnSuccessListener {
                memosProvider.memosList.set(posicion, memo)             // Con set sustituimos el elemento
                memosAdapter.notifyItemChanged(posicion)                // Con notifyItemChanged le indicamos al adapter el ítem que ha cambiado
                memosAdapter.notifyDataSetChanged()
                manager.scrollToPositionWithOffset(posicion,10)
            }
    }

    /**
     * Inserta un memo nuevo
     *
     * @param memo El memo a insertar
     */
    fun insertRegister(memo: Memo) {
        Log.d("urlUri", "ENTRÓ")

        var photoUploaded = false
        var videoUploaded = false
        var audioUploaded = false

        // Upload photo
        uploadFoto(memo.foto) { photoUrl ->
            memo.foto = photoUrl
            photoUploaded = true

            // Check if both photo and video uploads are complete
            if (photoUploaded && videoUploaded && audioUploaded) {
                updateFirestore(memo)
            }
        }

        // Upload video
        uploadVideo(memo.video) { videoUrl ->
            memo.video = videoUrl
            videoUploaded = true

            // Check if both photo and video uploads are complete
            if (photoUploaded && videoUploaded && audioUploaded) {
                updateFirestore(memo)
            }
        }

        // Upload audio
        uploadAudio(memo.audio) { audioUrl ->
            memo.audio = audioUrl
            audioUploaded = true

            // Check if both photo and video uploads are complete
            if (photoUploaded && videoUploaded && audioUploaded) {
                updateFirestore(memo)
            }
        }
    }

    private fun updateFirestore(memo: Memo) {
        Log.d("urlUri", "BIEN")
        myCollection
            .document(memo.id.toString())
            .set(
                hashMapOf(
                    "comentarios" to memo.comentarios,
                    "localizacion" to memo.localizacion.uppercase(),
                    "tituloLoc" to memo.tituloLoc,
                    "foto" to memo.foto,
                    "video" to memo.video,
                    "audio" to memo.audio
                )
            )
            .addOnSuccessListener {
                memosProvider.memosList.add(memo)
                memosAdapter.notifyItemInserted(memo.id)
                memosAdapter.notifyDataSetChanged()
                manager.scrollToPositionWithOffset(memo.id, 10)
            }
    }


    private fun uploadFoto(uriStr: String, callback: (String) -> Unit) {
        //subimos la imagen a firestore

        var link = ""


        var storageRef = storage.reference.child("Images")
        var file = Uri.parse(uriStr)
        var fileName = FirebaseAuth.getInstance().currentUser?.uid.toString() + System.currentTimeMillis().toString()
        storageRef = storageRef.child(fileName)

        var uploadTask = storageRef.putFile(file)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                Log.d("uploadestado", "success")
                task.exception?.let {
                    throw it
                }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                downloadUri = task.result
                callback(downloadUri.toString())
            } else {
                // Handle failures
                // ...
            }
        }

        //return urlTask.toString()

    }

    private fun uploadVideo(uriStr: String, callback: (String) -> Unit) {
        // Upload the video to Firestore

        var link = ""

        // Set the storage reference path for videos
        var storageRef = storage.reference.child("Videos")

        // Convert the video URI string to a Uri object
        var file = Uri.parse(uriStr)

        // Generate a unique file name for the video
        var fileName = FirebaseAuth.getInstance().currentUser?.uid.toString() + System.currentTimeMillis().toString()

        // Append the file name to the storage reference
        storageRef = storageRef.child(fileName)

        // Upload the video file
        var uploadTask = storageRef.putFile(file)

        // Continue with the task to get the download URL
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                Log.d("uploadestado", "success")
                task.exception?.let {
                    throw it
                }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the download URL for the video
                val downloadUri = task.result
                callback(downloadUri.toString())
            } else {
                // Handle failures
                // ...
            }
        }
    }

    private fun uploadAudio(uriStr: String, callback: (String) -> Unit) {
        Log.d("urlUri", "Entra en uploadAudio $uriStr")
        // Upload the audio to Firestore

        // Set the storage reference path for audios
        val storageRef = storage.reference.child("Audios")

        // Convert the audio URI string to a Uri object
        val file = Uri.parse(uriStr)

        // Generate a unique file name for the audio
        val fileName = "${FirebaseAuth.getInstance().currentUser?.uid}_${System.currentTimeMillis()}.mp3"

        // Append the file name to the storage reference
        val audioRef = storageRef.child(fileName)

        // Upload the audio file
        val uploadTask = audioRef.putFile(file)

        // Continue with the task to get the download URL
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                Log.d("uploadestado", "success")
                task.exception?.let {
                    throw it
                }
            }
            audioRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the download URL for the audio
                val downloadUri = task.result
                callback(downloadUri.toString())
            } else {
                // Handle failures
                // ...
            }
        }
    }

}