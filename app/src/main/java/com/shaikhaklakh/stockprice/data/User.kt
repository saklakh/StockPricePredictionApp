package com.shaikhaklakh.stockprice.data

data class User(
    val email: String,
    val imagePath: String = ""
){
    constructor():this("","")
}
