package com.ecosphere.wastepickup.booking.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecosphere.wastepickup.EcosphereApp
import com.ecosphere.wastepickup.R
import com.ecosphere.wastepickup.core.di.FeatureViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.booking.ui.BookingAdapter
import com.ecosphere.wastepickup.databinding.FragmentBookingHistoryBinding
import com.ecosphere.wastepickup.booking.viewmodel.BookingViewModel
import kotlinx.coroutines.launch

class BookingHistoryFragment : Fragment() {
    private val dialogs = SweetAlertDialogManager()
    private var _binding: FragmentBookingHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels {
        val container = (requireActivity().application as EcosphereApp).container
        FeatureViewModelFactory(container.bookingRepository, container.tokenManager)
    }
    private val adapter by lazy {
        BookingAdapter { id ->
            findNavController().navigate(
                R.id.action_bookingHistoryFragment_to_bookingDetailsFragment,
                bundleOf("bookingId" to id)
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialogs.bind(requireContext(), viewLifecycleOwner)
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
        binding.swipeRefresh.setColorSchemeResources(R.color.eco_green)
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.refreshBookings()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bookings.collect { bookings ->
                        adapter.submitList(bookings)
                        binding.tvEmpty.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvBookings.visibility = if (bookings.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.uiState.collect {
                        binding.swipeRefresh.isRefreshing = false
                        if (it.isRefreshing) dialogs.showLoading("Loading bookings", "Syncing the latest booking updates.")
                        else dialogs.dismissLoading()
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        if (event is com.ecosphere.wastepickup.booking.viewmodel.BookingUiEvent.Message) {
                            dialogs.showError("Bookings couldn't refresh", event.message)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) viewModel.refreshBookings()
    }

    override fun onDestroyView() {
        dialogs.dismiss()
        binding.rvBookings.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
