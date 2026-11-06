
package com.ecosphere.wastepickup.booking.ui.create

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ecosphere.wastepickup.EcosphereApp
import com.ecosphere.wastepickup.booking.viewmodel.BookingUiEvent
import com.ecosphere.wastepickup.booking.viewmodel.BookingViewModel
import com.ecosphere.wastepickup.core.di.FeatureViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.databinding.FragmentCreateBookingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateBookingFragment : Fragment() {

    private val dialogs = SweetAlertDialogManager()

    private var _binding: FragmentCreateBookingBinding? = null
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

        _binding = FragmentCreateBookingBinding.inflate(
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

        dialogs.bind(requireContext(), viewLifecycleOwner)

        setupCategoryDropdown()
        setupDatePicker()
        setupTimePicker()
        setupClickListeners()
        observeUiState()

        val tokenManager =
            (requireActivity().application as EcosphereApp)
                .container.tokenManager

        binding.etCustomerName.setText(
            tokenManager.getUserName()
        )

        binding.etMobile.setText(
            tokenManager.getUserMobile()
        )
    }

    private fun setupCategoryDropdown() {

        val categories = listOf(
            "Plastic",
            "Paper",
            "E-waste",
            "General Waste"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categories
        )

        binding.actCategory.setAdapter(adapter)
    }

    private fun setupDatePicker() {

        binding.etDate.setOnClickListener {

            val calendar = Calendar.getInstance()

            val dialog = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->

                    calendar.set(year, month, day)

                    binding.etDate.setText(
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                        ).format(calendar.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            dialog.datePicker.minDate =
                System.currentTimeMillis()

            dialog.show()
        }
    }

    private fun setupTimePicker() {

        binding.etTime.setOnClickListener {

            val calendar = Calendar.getInstance()

            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->

                    val selectedTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }

                    binding.etTime.setText(
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.US
                        ).format(selectedTime.time)
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun setupClickListeners() {

        binding.btnSubmitBooking.setOnClickListener {

            viewModel.createBooking(
                name = binding.etCustomerName.text
                    ?.toString().orEmpty(),

                mobile = binding.etMobile.text
                    ?.toString().orEmpty(),

                category = binding.actCategory.text
                    ?.toString().orEmpty(),

                address = binding.etAddress.text
                    ?.toString().orEmpty(),

                date = binding.etDate.text
                    ?.toString().orEmpty(),

                time = binding.etTime.text
                    ?.toString().orEmpty()
            )
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.uiState.collect { state ->

                        binding.tilCustomerName.error =
                            state.nameError

                        binding.tilMobile.error =
                            state.mobileError

                        binding.tilCategory.error =
                            state.categoryError

                        binding.tilAddress.error =
                            state.addressError

                        binding.tilDate.error =
                            state.dateError

                        binding.tilTime.error =
                            state.timeError

                        binding.btnSubmitBooking.isEnabled =
                            !state.isLoading

                        binding.btnSubmitBooking.text =
                            if (state.isLoading) {
                                "Submitting..."
                            } else {
                                "Submit Booking"
                            }
                        if (state.isLoading) dialogs.showLoading("Saving your booking", "Please wait while we submit your pickup request.")
                        else dialogs.dismissLoading()
                    }
                }

                launch {

                    viewModel.events.collect { event ->

                        when (event) {

                            is BookingUiEvent.Created -> {
                                dialogs.showSuccess(
                                    "Booking saved",
                                    event.message,
                                    onConfirm = { findNavController().navigateUp() }
                                )
                            }

                            is BookingUiEvent.Message -> {
                                dialogs.showError("Booking couldn't be submitted", event.message)
                            }

                            else -> Unit
                        }
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
