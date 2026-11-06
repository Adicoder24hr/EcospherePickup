package com.ecosphere.wastepickup.home.ui

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
import com.ecosphere.wastepickup.booking.ui.BookingAdapter
import com.ecosphere.wastepickup.core.di.FeatureViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.databinding.FragmentHomeBinding
import com.ecosphere.wastepickup.home.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private val dialogs = SweetAlertDialogManager()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels {
        val container = (requireActivity().application as EcosphereApp).container
        FeatureViewModelFactory(container.bookingRepository, container.tokenManager)
    }
    private val adapter by lazy {
        BookingAdapter { id ->
            findNavController().navigate(
                R.id.action_homeFragment_to_bookingDetailsFragment,
                bundleOf("bookingId" to id)
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialogs.bind(requireContext(), viewLifecycleOwner)
        binding.rvRecentBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentBookings.adapter = adapter
        binding.swipeRefresh.setColorSchemeResources(R.color.eco_green)
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            viewModel.refreshBookings()
        }
        binding.btnCreateBooking.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createBookingFragment)
        }
        binding.btnViewBookings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_bookingHistoryFragment)
        }
        binding.btnLogout.setOnClickListener {
            (requireActivity().application as EcosphereApp).container.tokenManager.clearSession()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.tvGreeting.text = "Hello, ${state.customerName}!"
                        binding.tvCustomerInfo.text = state.email
                        binding.tvTotal.text = state.totalBookings.toString()
                        binding.tvPending.text = state.pendingBookings.toString()
                        binding.tvCompleted.text = state.completedBookings.toString()
                        adapter.submitList(state.recentBookings)
                        binding.tvRecentEmpty.visibility = if (state.recentBookings.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvRecentBookings.visibility = if (state.recentBookings.isEmpty()) View.GONE else View.VISIBLE
                        binding.swipeRefresh.isRefreshing = false
                        if (state.isRefreshing) dialogs.showLoading("Updating your dashboard", "Refreshing your bookings and activity.")
                        else dialogs.dismissLoading()
                    }
                }
                launch {
                    viewModel.messages.collect { message ->
                        dialogs.showError("Bookings couldn't refresh", message)
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
        binding.rvRecentBookings.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
