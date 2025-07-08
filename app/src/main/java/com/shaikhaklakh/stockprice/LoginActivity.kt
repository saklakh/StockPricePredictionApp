package com.shaikhaklakh.stockprice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shaikhaklakh.stockprice.databinding.ActivityLoginBinding
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.getValue
import android.util.Base64
import androidx.activity.viewModels
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.shaikhaklakh.stockprice.data.User
import com.shaikhaklakh.stockprice.dialog.PasswordResetDialog
import com.shaikhaklakh.stockprice.util.RegisterValidation
import com.shaikhaklakh.stockprice.util.Resource
import com.shaikhaklakh.stockprice.util.Utils
import com.shaikhaklakh.stockprice.viewmodel.LoginViewModel
import com.shaikhaklakh.stockprice.viewmodel.RegisterViewModel


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginViewModel>()
    private val viewModel2 by viewModels<RegisterViewModel>()
    var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        loginActivity()
        passwordVisibility()
        setupGoogleSignInLogin()
        Forgotpassword()
        movetoRegister()
    }



    private fun movetoRegister() {
        binding.tvDontHaveAccount.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            finish()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun passwordVisibility() {
        binding.editLoginPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.editLoginPassword.compoundDrawables[drawableEnd]

                drawable?.let {
                    val drawableWidth = it.bounds.width()
                    val touchAreaStart = binding.editLoginPassword.right - drawableWidth - 40

                    if (event.rawX >= touchAreaStart) {
                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            binding.editLoginPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            binding.editLoginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.lock, 0, R.drawable.eye, 0
                            )
                            binding.editLoginPassword.transformationMethod = null
                        } else {
                            binding.editLoginPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            binding.editLoginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.lock, 0, R.drawable.eye_hide, 0
                            )
                            binding.editLoginPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                        }

                        binding.editLoginPassword.setSelection(binding.editLoginPassword.text?.length ?: 0)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }



    private fun loginActivity() {
        binding.apply {
            binding.loginSignInButton.setOnClickListener {
                val email = editLoginEmail.text.toString().trim()
                val password = editLoginPassword.text.toString()
                viewModel.login(email,password)
            }
        }
        lifecycleScope.launch {
            viewModel.login.collect {
                when(it){
                    is Resource.Loading->{

                    }
                    is Resource.Success->{

                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        finish()
                    }
                    is Resource.Error->{
                        Utils.showCustomToast(this@LoginActivity,"Incorrect Email or Password")

                    }
                    else -> Unit
                }

            }
        }
        lifecycleScope.launch {
            viewModel.validation.collect {
                viewModel.validation.collect { validation->
                    if (validation.email is RegisterValidation.Failed)
                    {
                        withContext(Dispatchers.Main){
                            binding.editLoginEmail.apply {
                                requestFocus()
                                error = validation.email.message
                            }
                        }
                    }
                    if (validation.password is RegisterValidation.Failed)
                    {
                        withContext(Dispatchers.Main){
                            binding.editLoginPassword.apply {
                                requestFocus()
                                error = validation.password.message
                            }
                        }
                    }
                }
            }
        }
    }



    private fun Forgotpassword() {
        binding.forgotPassBtn.setOnClickListener {
            PasswordResetDialog(this@LoginActivity){ email->
                viewModel.resetPassword(email)
            }.show()
        }
        lifecycleScope.launch {
            viewModel.resetPassword.collect {
                when(it){
                    is Resource.Loading->{

                    }
                    is Resource.Success->{
                        Snackbar.make(binding.root,"Reset link was sent to your email", Snackbar.LENGTH_LONG).show()
                    }
                    is Resource.Error->{
                        Snackbar.make(binding.root,"Failed to send reset link to your email", Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }

            }
        }
    }


    private fun setupGoogleSignInLogin() {
        binding.googleLoginBtn.setOnClickListener {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .setServerClientId(getString(R.string.web_client_id))
                .build()

            val credentialManager= CredentialManager.create(this@LoginActivity)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = this@LoginActivity
                    )
                    when(result.credential)
                    {
                        is CustomCredential->{
                            if (result.credential.type  == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential = GoogleAuthProvider.getCredential(googleTokenId,null)
                                val user=Firebase.auth.signInWithCredential(authCredential).await().user
                                user?.let {
                                    if (it.isAnonymous.not()){
                                        createUserIfNotExists(it)
                                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                                        finish()
                                    }
                                }
                            }
                        }
                    }
                }
                catch (e: NoCredentialException) {

                    getIntentlauncher()
                } catch (e: GetCredentialCancellationException) {

                    e.printStackTrace()

                } catch (e: Exception) {

                }
            }
        }
    }

    private fun getIntentlauncher(): Intent
    {
        return Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES,arrayOf("com.google"))
        }
    }





    private fun createUserIfNotExists(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val email = user.email ?: ""
                val newUser = User( email)
                viewModel2.saveUserInfoToFirestore(user.uid, newUser)

            } else {

            }
        }
    }






}