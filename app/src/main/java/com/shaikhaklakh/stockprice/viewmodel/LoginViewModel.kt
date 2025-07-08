package com.shaikhaklakh.stockprice.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.shaikhaklakh.stockprice.util.RegisterFieldState
import com.shaikhaklakh.stockprice.util.RegisterValidation
import com.shaikhaklakh.stockprice.util.Resource
import com.shaikhaklakh.stockprice.util.validateEmail
import com.shaikhaklakh.stockprice.util.validatePassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor (private val firebaseAuth: FirebaseAuth): ViewModel() {
    private val _login = MutableSharedFlow<Resource<FirebaseUser>>()
    val login = _login.asSharedFlow()

    private val _validation = Channel<RegisterFieldState>()
    val validation = _validation.receiveAsFlow()

    private val _resetPassword = MutableSharedFlow<Resource<String>>()
    val resetPassword = _resetPassword.asSharedFlow()

    fun login(email: String, password: String)
    {
        val emailValidation = validateEmail(email)
        val passwordValidation = validatePassword(password)
        if (emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success){

            viewModelScope.launch { _login.emit(Resource.Loading()) }

            firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener {
                    viewModelScope.launch {
                        it.user?.let {
                            _login.emit(Resource.Success(it))
                        }
                    }
                }.addOnFailureListener {
                    viewModelScope.launch {
                        _login.emit(Resource.Error(it.message.toString()))
                    }
                }
        }
        else{
            val  registerFieldState = RegisterFieldState(
                validateEmail(email),validatePassword(password)
            )
            viewModelScope.launch {
                _validation.send(registerFieldState)
            }

        }

    }


    fun resetPassword(email: String){
        viewModelScope.launch {
            _resetPassword.emit(Resource.Loading())
        }
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Success(email))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Error(it.message.toString()))
                }
            }
    }

}