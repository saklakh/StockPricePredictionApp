package com.shaikhaklakh.stockprice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shaikhaklakh.stockprice.databinding.ActivityRegisterBinding
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import androidx.activity.viewModels
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.shaikhaklakh.stockprice.data.User
import com.shaikhaklakh.stockprice.util.RegisterValidation
import com.shaikhaklakh.stockprice.util.Resource
import com.shaikhaklakh.stockprice.viewmodel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.getValue

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel by viewModels<RegisterViewModel>()
    var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }


        createAccount()
        passwordVisibility()
        setupGoogleSignIn()
        moveToLogin()


    }

    private fun moveToLogin() {
        binding.tvHaveAccount.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            finish()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun passwordVisibility() {
        binding.editSignUpPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.editSignUpPassword.compoundDrawables[drawableEnd]

                drawable?.let {
                    val drawableWidth = it.bounds.width()
                    val touchAreaStart = binding.editSignUpPassword.right - drawableWidth - 40

                    if (event.rawX >= touchAreaStart) {
                        isPasswordVisible = !isPasswordVisible

                        if (isPasswordVisible) {
                            binding.editSignUpPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            binding.editSignUpPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.lock, 0, R.drawable.eye, 0
                            )
                            binding.editSignUpPassword.transformationMethod = null
                        } else {
                            binding.editSignUpPassword.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            binding.editSignUpPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.lock, 0, R.drawable.eye_hide, 0
                            )
                            binding.editSignUpPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                        }

                        binding.editSignUpPassword.setSelection(binding.editSignUpPassword.text?.length ?: 0)
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }


    private fun createAccount() {
        binding.apply {
            registerSignUpButton.setOnClickListener {
                val user = User(
                    editSignUpEmail.text.toString().trim(),
                )
                val password = editSignUpPassword.text.toString()
                viewModel.createAccountWithEmailAndPassword(user, password)
            }
        }
        lifecycleScope.launch {
            viewModel.register.collect {
                when(it)
                {
                    is Resource.Loading->{

                    }
                    is Resource.Success->{

                        //  Navigate to MainActivity
                        val uid = it.data!!.uid
                        startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                        finish()
                    }
                    is Resource.Error->{
                        val errorMessage = when {
                            it.message?.contains("email address is already in use", ignoreCase = true) == true ->
                                "This email is already registered. Please log in or use another email."

                            else -> it.message ?: "Something went wrong. Please try again later."
                        }

                        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }

        lifecycleScope.launch {
            viewModel.validation.collect { validation->
                if (validation.email is RegisterValidation.Failed)
                {
                    withContext(Dispatchers.Main){
                        binding.editSignUpEmail.apply {
                            requestFocus()
                            error = validation.email.message
                        }
                    }
                }
                if (validation.password is RegisterValidation.Failed)
                {
                    withContext(Dispatchers.Main){
                        binding.editSignUpPassword.apply {
                            requestFocus()
                            error = validation.password.message
                        }
                    }
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        binding.googleSignUpBtn.setOnClickListener {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(getString(R.string.web_client_id))
                .build()

            val credentialManager= CredentialManager.create(this@RegisterActivity)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = this@RegisterActivity
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
                                        startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
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
                val newUser = User(email)
                viewModel.saveUserInfoToFirestore(user.uid, newUser)

            } else {

            }
        }
    }










}