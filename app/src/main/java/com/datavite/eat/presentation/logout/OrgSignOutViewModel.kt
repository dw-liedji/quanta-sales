package com.datavite.eat.presentation.logout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datavite.eat.data.local.database.AppDatabase
import com.datavite.eat.data.local.datastore.AuthOrgUserCredentialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrgSignOutViewModel @Inject constructor (
    private val authOrgUserCredentialManager: AuthOrgUserCredentialManager,
    private val database: AppDatabase
) : ViewModel() {
    // State for showing success alert
    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    fun signOut(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clearAllTables()
                authOrgUserCredentialManager.deleteAuthOrgUser()
                _isLoggedOut.value = true
            }catch (e:Exception){
                _isLoggedOut.value = false
            }

        }
    }
}