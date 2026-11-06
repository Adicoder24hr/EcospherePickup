
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
import com.ecosphere.wastepickup.auth.viewmodel.LoginUiEvent
import com.ecosphere.wastepickup.auth.viewmodel.LoginViewModel
import com.ecosphere.wastepickup.core.di.AuthViewModelFactory
import com.ecosphere.wastepickup.core.ui.dialog.SweetAlertDialogManager
import com.ecosphere.wastepickup.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val dialogs = SweetAlertDialogManager()

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {

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

        _binding = FragmentLoginBinding.inflate(
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

        binding.btnLogin.setOnClickListener {

            viewModel.login(
                identity = binding.etEmailOrMobile.text
                    ?.toString().orEmpty(),

                password = binding.etPassword.text
                    ?.toString().orEmpty()
            )
        }

        binding.tvCreateAccount.setOnClickListener {

            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }

        binding.tvForgotPassword.setOnClickListener {

            dialogs.showInfo("Password recovery", "Password recovery is not included in this demo.")
        }

        binding.etEmailOrMobile.doAfterTextChanged {
            viewModel.onIdentityChanged()
        }

        binding.etPassword.doAfterTextChanged {
            viewModel.onPasswordChanged()
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.uiState.collect { state ->

                        binding.tilEmailOrMobile.error =
                            state.identityError

                        binding.tilPassword.error =
                            state.passwordError

                        binding.btnLogin.isEnabled =
                            !state.isLoading

                        binding.btnLogin.text =
                            if (state.isLoading) {
                                "Signing in..."
                            } else {
                                "Login"
                            }
                        if (state.isLoading) dialogs.showLoading("Signing in", "Checking your account details.")
                        else dialogs.dismissLoading()
                    }
                }

                launch {

                    viewModel.events.collect { event ->

                        when (event) {

                            is LoginUiEvent.Success -> {
                                dialogs.showSuccess(
                                    "Welcome to Ecosphere!",
                                    "You have signed in successfully.",
                                    onConfirm = {
                                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                                    }
                                )
                            }

                            is LoginUiEvent.ShowMessage -> {

                                dialogs.showError("Login failed", event.message)
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
