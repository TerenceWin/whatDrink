package com.whatdrink.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.whatdrink.app.data.model.Comment
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DrinkProfileUiState {
    object Loading : DrinkProfileUiState()
    data class Success(val detail: DrinkDetail, val stats: DrinkStats, val imageUrl: String?) : DrinkProfileUiState()
    object NotFound : DrinkProfileUiState()
    data class Error(val message: String) : DrinkProfileUiState()
}

@HiltViewModel
class DrinkProfileViewModel @Inject constructor(
    private val repository: DrinkRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<DrinkProfileUiState>(DrinkProfileUiState.Loading)
    val uiState: StateFlow<DrinkProfileUiState> = _uiState

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment

    val isLoggedIn get() = auth.currentUser != null

    private var currentDrinkId: String = ""

    fun load(drinkId: String) {
        currentDrinkId = drinkId
        viewModelScope.launch {
            _uiState.value = DrinkProfileUiState.Loading
            try {
                repository.incrementViews(drinkId)

                val detailDeferred = async { repository.getDrinkDetail(drinkId) }
                val statsDeferred  = async { repository.getDrinkStats(drinkId) }
                val drinkDeferred  = async { repository.getDrinkById(drinkId) }

                val detail = detailDeferred.await()
                val stats  = statsDeferred.await()
                val drink  = drinkDeferred.await()

                Log.d("DrinkProfile", "drinkId=$drinkId drink=$drink imageUrl=${drink?.imageUrl}")
                val rawImageUrl = drink?.imageUrl?.takeIf { it.isNotBlank() } ?: drinkId
                val imageUrl = repository.getImageUrl(rawImageUrl)
                Log.d("DrinkProfile", "rawImageUrl=$rawImageUrl resolvedUrl=$imageUrl")

                _uiState.value = if (detail != null && stats != null)
                    DrinkProfileUiState.Success(detail, stats, imageUrl)
                else
                    DrinkProfileUiState.NotFound
            } catch (e: Exception) {
                _uiState.value = DrinkProfileUiState.Error(e.message ?: "Unknown error")
            }
        }

        viewModelScope.launch {
            repository.getComments(drinkId).collect { _comments.value = it }
        }
    }

    fun postComment(context: String, onNotLoggedIn: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onNotLoggedIn()
            return
        }
        val trimmed = context.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _isPostingComment.value = true
            try {
                val userData = repository.getUser(user.uid)
                val username = userData?.username ?: user.email ?: "Anonymous"
                repository.addComment(
                    drinkId = currentDrinkId,
                    userId = user.uid,
                    username = username,
                    context = trimmed
                )
            } catch (e: Exception) {
                Log.e("DrinkProfile", "Failed to post comment: ${e.message}")
            } finally {
                _isPostingComment.value = false
            }
        }
    }
}
