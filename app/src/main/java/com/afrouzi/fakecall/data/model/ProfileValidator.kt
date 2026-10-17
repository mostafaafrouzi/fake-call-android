package com.afrouzi.fakecall.data.model

object ProfileValidator {
    fun isValidName(name: String?): Boolean {
        return !name.isNullOrBlank()
    }

    fun isValidNumber(number: String?): Boolean {
        return !number.isNullOrBlank()
    }

    fun isValid(name: String?, number: String?): Boolean {
        return isValidName(name) && isValidNumber(number)
    }
}
