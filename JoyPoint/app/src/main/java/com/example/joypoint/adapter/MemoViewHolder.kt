package com.example.joypoint.adapter

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.joypoint.FireStore.OperacionesFireStore
import com.example.joypoint.Memo
import com.example.joypoint.VisualizarActivity
import com.example.joypoint.databinding.MemoBinding

class MemoViewHolder (view: View): RecyclerView.ViewHolder (view) {
    val binding = MemoBinding.bind(view)
    private var sonidos: VisualizarActivity = VisualizarActivity()
    var borrar: Int = 0

    /**
     * Método que renderizará cada elemento del RecyclerView
     * Asigna los datos al elemento
     *
     * @param memo El memo a mostrar
     * @param openSomeActivitySendingData Método para abrir actividad del detalle y que se implementa en el MainActivity
     * @param deleteRegister Método para eliminar el registro y que se implementa en el MainActivity
     */
    fun render (
        memo: Memo,
        deleteRegister: (Int, Int) -> Unit,
        openActivityEditar: (Int, Memo) -> Unit,
        openVerMemo: (Int, Memo) -> Unit,
        openVerMapa: (Int, Memo) -> Unit,
        playSound: () -> Unit
    ) {
        binding.viewComentarios.text = memo.comentarios
        Glide.with(binding.imageViewFoto.context)
            .load(memo.foto)
            .into(binding.imageViewFoto)

        // Al hacer click en la imagen mostrará el comentario del memo guardado
        binding.imageViewFoto.setOnClickListener {
            Toast.makeText(
                binding.imageViewFoto.context,
                memo.comentarios,
                Toast.LENGTH_SHORT
            ).show()
        }

        // Al hacer click en cualquier parte mostrará la localización del memo
        itemView.setOnClickListener{
            Toast.makeText(
                binding.imageViewFoto.context,
                memo.localizacion,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnEliminar.setOnClickListener {
            playSound()
            deleteRegister(adapterPosition, memo.id)
        }

        binding.btnVer.setOnClickListener{
            openVerMemo(adapterPosition, memo)
        }

        binding.btnMapa.setOnClickListener {
            openVerMapa(adapterPosition, memo)
        }
    }
}