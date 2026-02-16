package com.udyneos.animex.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.udyneos.animex.R
import com.udyneos.animex.model.Anime

class SectionAdapter(
    private val onAnimeClick: (Anime) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {

    private val sections = mutableListOf<Section>()

    data class Section(
        val title: String,
        val animeList: List<Anime>,
        val viewAllTag: String
    )

    fun submitSections(newSections: List<Section>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section_grid, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount() = sections.size

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tvSectionTitle)
        private val btnViewAll: MaterialButton = itemView.findViewById(R.id.btnViewAll)
        private val rvGrid: RecyclerView = itemView.findViewById(R.id.rvGrid)

        fun bind(section: Section) {
            tvSectionTitle.text = section.title
            
            // Setup Grid RecyclerView dengan 3 kolom
            rvGrid.layoutManager = GridLayoutManager(itemView.context, 3)
            
            // Tampilkan 9 item (3 baris x 3 kolom)
            val displayList = section.animeList.take(9)
            
            val animeAdapter = AnimeAdapter(onAnimeClick)
            rvGrid.adapter = animeAdapter
            animeAdapter.submitList(displayList)

            btnViewAll.setOnClickListener {
                onViewAllClick(section.viewAllTag)
            }
        }
    }
}
