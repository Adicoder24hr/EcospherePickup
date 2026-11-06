package com.ecosphere.wastepickup.booking.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecosphere.wastepickup.R
import com.ecosphere.wastepickup.booking.local.BookingEntity
import com.ecosphere.wastepickup.booking.util.BookingReferenceFormatter
import com.ecosphere.wastepickup.databinding.ItemBookingBinding

class BookingAdapter(
    private val onBookingClick: (String) -> Unit
) : ListAdapter<BookingEntity, BookingAdapter.BookingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding, onBookingClick)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) =
        holder.bind(getItem(position))

    class BookingViewHolder(
        private val binding: ItemBookingBinding,
        private val onBookingClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: BookingEntity) {
            val context = binding.root.context
            binding.tvBookingReference.text = BookingReferenceFormatter.format(booking.id)
            binding.tvBookingSummary.text = "${booking.category} · ${booking.pickupDate}"
            binding.tvBookingTime.text = booking.pickupTime
            binding.tvBookingStatus.text = booking.status
            val (foreground, background) = when (booking.status) {
                "Confirmed" -> R.color.status_blue to R.color.status_blue_bg
                "Completed" -> R.color.status_green to R.color.status_green_bg
                "Cancelled" -> R.color.status_red to R.color.status_red_bg
                else -> R.color.status_amber to R.color.status_amber_bg
            }
            binding.tvBookingStatus.setTextColor(ContextCompat.getColor(context, foreground))
            binding.tvBookingStatus.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, background))
            binding.root.setOnClickListener { onBookingClick(booking.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BookingEntity>() {
        override fun areItemsTheSame(oldItem: BookingEntity, newItem: BookingEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BookingEntity, newItem: BookingEntity) = oldItem == newItem
    }
}
