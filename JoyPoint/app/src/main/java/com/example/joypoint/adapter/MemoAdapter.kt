package com.example.joypoint.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.joypoint.Memo
import com.example.joypoint.R

/**
 * Adaptador para la lista de Memos en el RecyclerView.
 */
class MemoAdapter(
    private val memosList: MutableList<Memo>,
    private val deleteRegister: (Int, Int) -> Unit,
    private val openActivityEditar: (Int, Memo) -> Unit,
    private val openVerMemo: (Int, Memo) -> Unit
) : RecyclerView.Adapter<MemoViewHolder>() {

    /**
     * Crea un nuevo MemoViewHolder inflando el diseño del elemento de la lista
     *
     * @param parent El ViewGroup en el que se inflará la nueva vista después
     * @param viewType El tipo de la nueva vista
     * return El MemoViewHolder creado
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)                                // Se obtiene el LayoutInflater del contexto del parent
        return MemoViewHolder(layoutInflater.inflate(R.layout.memo, parent, false))  // Se infla el diseño de R.layout.memo y se crea una nueva instancia de MemoViewHolder con la vista inflada
    }

    /**
     * Método para actualizar el contenido de la vista
     *
     * @param memosViewHolder El MemoViewHolder que representa la vista del memo
     * @param position La posición del memo a actualizar
     */
    override fun onBindViewHolder(memosViewHolder: MemoViewHolder, position: Int) {
        val memo = memosList[position]
        memosViewHolder.render(memo, deleteRegister, openActivityEditar, openVerMemo)
    }

    /**
     * Método que obtiene la cantidad total de memos
     *
     * @return La cantidad total de elementos en la lista.
     */
    override fun getItemCount(): Int {
        return memosList.size
    }
}
