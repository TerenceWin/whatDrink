package com.whatdrink.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.whatdrink.app.data.model.User
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User, val profileImageUrl: String?) : ProfileUiState()
    object NotLoggedIn : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repository: DrinkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    // gsUrl -> httpsUrl pairs for the image picker
    private val _pickerImages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val pickerImages: StateFlow<List<Pair<String, String>>> = _pickerImages

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = ProfileUiState.NotLoggedIn
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val user = repository.getUser(uid)
                if (user == null) {
                    _uiState.value = ProfileUiState.Error("User data not found")
                    return@launch
                }
                val imageUrl = if (user.profileImage.isNotBlank())
                    repository.getImageUrl(user.profileImage)
                else null
                _uiState.value = ProfileUiState.Success(user, imageUrl)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun loadProfileImages() {
        viewModelScope.launch {
            try {
                val gsUrls = repository.getProfileImages()
                val pairs = gsUrls.mapNotNull { gsUrl ->
                    val https = repository.getImageUrl(gsUrl) ?: return@mapNotNull null
                    gsUrl to https
                }
                _pickerImages.value = pairs
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load profile images: ${e.message}")
            }
        }
    }

    fun updateProfileImage(gsUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.updateProfileImage(uid, gsUrl)
                val https = repository.getImageUrl(gsUrl)
                val current = _uiState.value
                if (current is ProfileUiState.Success) {
                    _uiState.value = current.copy(
                        user = current.user.copy(profileImage = gsUrl),
                        profileImageUrl = https
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile image: ${e.message}")
            }
        }
    }

    fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        val trimmed = newUsername.trim()
        if (trimmed.isBlank()) return

        val previous = _uiState.value
        if (previous is ProfileUiState.Success) {
            _uiState.value = previous.copy(user = previous.user.copy(username = trimmed))
        }

        viewModelScope.launch {
            try {
                repository.updateUsername(uid, trimmed)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update username: ${e.message}")
                _uiState.value = previous
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = ProfileUiState.NotLoggedIn
    }
}
