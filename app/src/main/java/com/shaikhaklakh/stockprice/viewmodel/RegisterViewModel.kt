package com.shaikhaklakh.stockprice.viewmodel



import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.shaikhaklakh.stockprice.data.User
import com.shaikhaklakh.stockprice.util.RegisterFieldState
import com.shaikhaklakh.stockprice.util.RegisterValidation
import com.shaikhaklakh.stockprice.util.Resource
import com.shaikhaklakh.stockprice.util.validateEmail
import com.shaikhaklakh.stockprice.util.validatePassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseFirestore): ViewModel() {


    private val _register= MutableStateFlow<Resource<FirebaseUser>>(Resource.Unspecified())
    val register:Flow<Resource<FirebaseUser>> = _register

    private val _validation = Channel<RegisterFieldState>()
    val validation = _validation.receiveAsFlow()


    fun createAccountWithEmailAndPassword(user: User,password: String){
        if(checkValidation(user,password)) {
            runBlocking {
                _register.emit(Resource.Loading())
            }
            firebaseAuth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener { authResult ->

                    val firebaseUser = authResult.user
                    firebaseUser?.let {
                        saveUserInfoToFirestore(it.uid,user)
                        _register.value = Resource.Success(it)
                    }
                }.addOnFailureListener {
                    _register.value = Resource.Error(it.message.toString())
                }
        }
        else{
            val  registerFieldState = RegisterFieldState(
                validateEmail(user.email),validatePassword(password)
            )
            runBlocking {
                _validation.send(registerFieldState)
            }
        }
    }

    fun saveUserInfoToFirestore(uid: String,user: User) {
        db.collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener {

            }.addOnFailureListener {
                _register.value = Resource.Error(it.message.toString())
            }
    }


    private fun checkValidation(user: User,password: String): Boolean{
        val emailValidation = validateEmail(user.email)
        val passwordValidation = validatePassword(password)
        val shouldRegister = emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success

        return shouldRegister
    }


    fun logout(){
        firebaseAuth.signOut()
    }


}