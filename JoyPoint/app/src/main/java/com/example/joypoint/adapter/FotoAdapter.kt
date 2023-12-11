package com.example.joypoint.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.joypoint.Foto
import com.example.joypoint.R

class FotoAdapter (
    private val fotosList: MutableList<Foto>): RecyclerView.Adapter<FotoViewHolder>() {

    /**
     * Crea un nuevo FotoViewHolder inflando el diseño del elemento de la lista
     *
     * @param parent El ViewGroup en el que se inflará la nueva vista después
     * @param viewType El tipo de la nueva vista
     * return El FotoViewHolder creado
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return FotoViewHolder(layoutInflater.inflate(R.layout.foto, parent, false))
    }

    /**
     * Método para actualizar el contenido de la vista
     *
     * @param fotosViewHolder El FotoViewHolder que representa la vista del memo
     * @param position La posición del memo a actualizar
     */
    override fun onBindViewHolder(fotosViewHolder: FotoViewHolder, position: Int) {
        val foto = fotosList[position]
        fotosViewHolder.render(foto)
    }

    /**
     * Método que obtiene la cantidad total de fotos
     *
     * @return La cantidad total de elementos en la lista.
     */
    override fun getItemCount(): Int {
        return fotosList.size
    }
}