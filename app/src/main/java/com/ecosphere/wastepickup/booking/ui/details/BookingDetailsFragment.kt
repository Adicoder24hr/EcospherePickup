package com.ecosphere.wastepickup.booking.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ecosphere.wastepickup.EcosphereApp
import com.ecosphere.wastepickup.R
import com.ecosphere.wastepickup.booking.viewmodel.BookingUiEvent
import com.ecosphere.wastepickup.booking.util.BookingReferenceFormatter
import com.ecosphere.wastepickup.booking.viewmodel.BookingViewModel
import com.ecosphere.wastepickup.core.di.FeatureViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.databinding.FragmentBookingDetailsBinding
import kotlinx.coroutines.launch

class BookingDetailsFragment : Fragment() {

    private val dialogs = SweetAlertDialogManager()

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels {

        val container =
            (requireActivity().application as EcosphereApp).container

        FeatureViewModelFactory(
            container.bookingRepository,
            container.tokenManager
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBookingDetailsBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val bookingId =
            requireArguments().getString("bookingId").orEmpty()

        dialogs.bind(requireContext(), viewLifecycleOwner)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnCancelBooking.setOnClickListener {
            if (!binding.btnCancelBooking.isEnabled) return@setOnClickListener

            dialogs.showConfirmation(
                title = "Cancel booking?",
                message = "Are you sure you want to cancel this waste pickup booking?",
                cancelText = "No, go back",
                confirmText = "Yes, cancel"
            ) {
                    viewModel.cancelBooking(bookingId)
                }
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.refreshBooking(bookingId)
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.eco_green)

        observeBooking(bookingId)
        observeEvents()
        observeLoading()
        viewModel.refreshBooking(bookingId)
    }

    private fun observeBooking(id: String) {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.observeBooking(id).collect { booking ->

                    if (booking == null) return@collect

                    binding.tvBookingId.text = BookingReferenceFormatter.format(booking.id)
                    binding.tvSummaryCategory.text = booking.category

                    binding.tvStatus.text =
                        booking.status
                    val statusColor = when (booking.status) {
                        "Confirmed" -> R.color.status_blue
                        "Completed" -> R.color.status_green
                        "Cancelled" -> R.color.status_red
                        else -> R.color.status_amber
                    }
                    binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
                    binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), when (booking.status) {
                            "Confirmed" -> R.color.status_blue_bg
                            "Completed" -> R.color.status_green_bg
                            "Cancelled" -> R.color.status_red_bg
                            else -> R.color.status_amber_bg
                        })
                    )

                    renderProgress(booking.status)

                    binding.tvCustomerName.text =
                        "Customer: ${booking.customerName}"

                    binding.tvMobile.text =
                        "Mobile: ${booking.mobile}"

                    binding.tvCategory.text =
                        "Waste Category: ${booking.category}"

                    binding.tvAddress.text =
                        "Pickup Address: ${booking.address}"

                    binding.tvDate.text =
                        "Pickup Date: ${booking.pickupDate}"

                    binding.tvTime.text =
                        "Pickup Time: ${booking.pickupTime}"

                    binding.btnCancelBooking.isVisible =
                        booking.status == "Pending" ||
                                booking.status == "Confirmed"
                }
            }
        }
    }

    private fun renderProgress(status: String) {
        val cancelled = status == "Cancelled"
        binding.cardProgress.isVisible = !cancelled
        binding.cardCancelled.isVisible = cancelled
        if (cancelled) return
        val currentStep = when (status) {
            "Confirmed" -> 1
            "Completed" -> 2
            else -> 0
        }
        val steps = listOf(
            binding.stepPendingIcon to binding.stepPendingLabel,
            binding.stepConfirmedIcon to binding.stepConfirmedLabel,
            binding.stepCompletedIcon to binding.stepCompletedLabel
        )
        steps.forEachIndexed { index, (icon, label) ->
            val complete = index < currentStep
            val active = index == currentStep
            icon.text = if (complete) "✓" else (index + 1).toString()
            icon.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (complete || active) R.color.eco_green else R.color.eco_border)
            )
            icon.setTextColor(ContextCompat.getColor(requireContext(), if (complete || active) R.color.white else R.color.eco_secondary_text))
            label.setTextColor(ContextCompat.getColor(requireContext(), if (complete || active) R.color.eco_dark_green else R.color.eco_secondary_text))
            label.setTypeface(null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        binding.progressLineOne.setBackgroundColor(ContextCompat.getColor(requireContext(), if (currentStep >= 1) R.color.eco_green else R.color.eco_border))
        binding.progressLineTwo.setBackgroundColor(ContextCompat.getColor(requireContext(), if (currentStep >= 2) R.color.eco_green else R.color.eco_border))
    }

    private fun observeLoading() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = false
                    when {
                        state.isLoading -> dialogs.showLoading("Cancelling booking", "Please wait while we update your booking.")
                        state.isRefreshing -> dialogs.showLoading("Refreshing booking", "Fetching the latest booking information.")
                        else -> dialogs.dismissLoading()
                    }
                    binding.btnCancelBooking.isEnabled = !state.isLoading && !state.isRefreshing
                    binding.btnCancelBooking.text = if (state.isLoading) "Cancelling..." else "Cancel Booking"
                }
            }
        }
    }

    private fun observeEvents() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.events.collect { event ->

                    when (event) {

                        BookingUiEvent.Cancelled -> {
                            dialogs.showSuccess(
                                "Booking cancelled!",
                                "Your booking ${BookingReferenceFormatter.format(requireArguments().getString("bookingId").orEmpty())} has been cancelled successfully."
                            )
                        }

                        is BookingUiEvent.Message -> {
                            dialogs.showError("Unable to complete request", event.message)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        dialogs.dismiss()
        super.onDestroyView()
        _binding = null
    }
}
