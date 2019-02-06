package org.wordpress.android.ui.plans

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.ui.plans.PlansListAdapter.PlanOffersItemViewHolder
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class PlansListAdapter(
    val activity: Activity,
    private val itemClickListener: (PlanOffersModel) -> Unit
) : Adapter<PlanOffersItemViewHolder>() {
    private val list = mutableListOf<PlanOffersModel>()
    @Inject lateinit var imageManager: ImageManager

    init {
        (activity.applicationContext as WordPress).component().inject(this)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: PlanOffersItemViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: PlanOffersItemViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanOffersItemViewHolder {
        return PlanOffersItemViewHolder(
                parent,
                itemClickListener,
                imageManager
        )
    }

    internal fun updateList(items: List<PlanOffersModel>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    class PlanOffersItemViewHolder(
        parent: ViewGroup,
        private val itemClickListener: (PlanOffersModel) -> Unit,
        val imageManager: ImageManager
    ) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                    R.layout.plan_offers_list_item,
                    parent,
                    false
            )
    ) {
        private val container: View = itemView.findViewById(R.id.item_layout)
        private val planImage: ImageView = itemView.findViewById(R.id.plan_image)
        private val title: TextView = itemView.findViewById(R.id.item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.item_subtitle)

        fun bind(planOffersModel: PlanOffersModel) {
            container.setOnClickListener { itemClickListener(planOffersModel) }
            title.text = planOffersModel.name
            subtitle.text = planOffersModel.tagline

            if (!TextUtils.isEmpty(planOffersModel.iconUrl)) {
                imageManager.loadIntoCircle(
                        planImage, ImageType.PLAN,
                        StringUtils.notNullStr(planOffersModel.iconUrl)
                )
            }
        }
    }
}
