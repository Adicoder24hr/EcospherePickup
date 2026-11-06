
package com.ecosphere.wastepickup.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ecosphere.wastepickup.EcosphereApp
import com.ecosphere.wastepickup.R
import com.ecosphere.wastepickup.auth.viewmodel.RegisterUiEvent
import com.ecosphere.wastepickup.auth.viewmodel.RegisterViewModel
import com.ecosphere.wastepickup.core.di.AuthViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val dialogs = SweetAlertDialogManager()

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels {

        val container =
            (requireActivity().application as EcosphereApp).container

        AuthViewModelFactory(
            container.authRepository,
            container.tokenManager
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRegisterBinding.inflate(
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

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {

        binding.btnRegister.setOnClickListener {

            viewModel.register(
                name = binding.etFullName.text
                    ?.toString().orEmpty(),

                email = binding.etEmail.text
                    ?.toString().orEmpty(),

                mobile = binding.etMobile.text
                    ?.toString().orEmpty(),

                password = binding.etPassword.text
                    ?.toString().orEmpty(),

                confirmPassword = binding.etConfirmPassword.text
                    ?.toString().orEmpty()
            )
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etFullName.doAfterTextChanged {
            viewModel.onNameChanged()
        }

        binding.etEmail.doAfterTextChanged {
            viewModel.onEmailChanged()
        }

        binding.etMobile.doAfterTextChanged {
            viewModel.onMobileChanged()
        }

        binding.etPassword.doAfterTextChanged {
            viewModel.onPasswordChanged()
        }

        binding.etConfirmPassword.doAfterTextChanged {
            viewModel.onConfirmPasswordChanged()
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.uiState.collect { state ->

                        binding.tilFullName.error =
                            state.nameError

                        binding.tilEmail.error =
                            state.emailError

                        binding.tilMobile.error =
                            state.mobileError

                        binding.tilPassword.error =
                            state.passwordError

                        binding.tilConfirmPassword.error =
                            state.confirmPasswordError

                        binding.btnRegister.isEnabled =
                            !state.isLoading

                        binding.btnRegister.text =
                            if (state.isLoading) {
                                "Creating account..."
                            } else {
                                "Create Account"
                            }
                        if (state.isLoading) dialogs.showLoading("Creating account", "Please wait while we register you.")
                        else dialogs.dismissLoading()
                    }
                }

                launch {

                    viewModel.events.collect { event ->

                        when (event) {

                            is RegisterUiEvent.Success -> {
                                dialogs.showSuccess(
                                    title = "Registration Successful!",
                                    message = "Your Ecosphere account has been created successfully. Please log in using your credentials.",
                                    buttonText = "OK",
                                    onConfirm = { findNavController().popBackStack(R.id.loginFragment, false) }
                                )
                            }

                            is RegisterUiEvent.ShowMessage -> {

                                dialogs.showError("Registration failed", event.message)
                            }
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
