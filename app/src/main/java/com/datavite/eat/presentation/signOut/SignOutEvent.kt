package com.datavite.eat.presentation.signOut

sealed class SignOutEvent () {
    data object SubmitButtonClicked: SignOutEvent()
}