package com.example.joypoint.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.joypoint.Foto
import com.example.joypoint.databinding.FotoBinding

class FotoViewHolder (view: View): RecyclerView.ViewHolder (view) {
    val binding = FotoBinding.bind(view)

    /**
     * Método que renderizará cada elemento del RecyclerView
     * Asigna los datos al elemento
     *
     * @param foto La foto a mostrar
     */
    fun render (
        foto: Foto
    ) {
        Glide.with(binding.imageFoto.context)
            .load(foto.foto)
            .into(binding.imageFoto)
    }
}