package com.whatdrink.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.whatdrink.app.R
import com.whatdrink.app.data.model.User
import com.whatdrink.app.ui.components.BottomBar
import com.whatdrink.app.ui.components.BottomBarTab
import com.whatdrink.app.ui.language.AppLanguage
import com.whatdrink.app.ui.language.LocalAppLanguage
import com.whatdrink.app.viewmodel.ProfileUiState
import com.whatdrink.app.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    onGoHome: () -> Unit = {},
    onOpenMap: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pickerImages by viewModel.pickerImages.collectAsState()
    val lang = LocalAppLanguage.current
    var showImagePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.drink_profile_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            is ProfileUiState.NotLoggedIn -> {
                LaunchedEffect(Unit) { onNavigateToLogin() }
            }
            is ProfileUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    user = state.user,
                    profileImageUrl = state.profileImageUrl,
                    lang = lang,
                    onUpdateUsername = { viewModel.updateUsername(it) },
                    onAvatarClick = {
                        viewModel.loadProfileImages()
                        showImagePicker = true
                    },
                    onSignOut = { viewModel.signOut() }
                )
            }
        }

        BottomBar(
            activeTab = BottomBarTab.PROFILE,
            onOpenMap = onOpenMap,
            onGoHome = onGoHome,
            onOpenProfile = {},
            modifier = Modifier.align(Alignment.BottomStart)
        )

        if (showImagePicker) {
            ProfileImagePickerSheet(
                images = pickerImages,
                onSelect = { gsUrl ->
                    viewModel.updateProfileImage(gsUrl)
                    showImagePicker = false
                },
                onDismiss = { showImagePicker = false }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    profileImageUrl: String?,
    lang: AppLanguage,
    onUpdateUsername: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onSignOut: () -> Unit
) {
    var editingUsername by remember { mutableStateOf(false) }
    var displayUsername by remember { mutableStateOf(user.username) }
    var usernameInput by remember(displayUsername) { mutableStateOf(displayUsername) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 35.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Profile image
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(70.dp)
                    )
                }
            }
            // Camera badge
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (editingUsername) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.4f),
                        cursorColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        displayUsername = usernameInput
                        onUpdateUsername(usernameInput)
                        editingUsername = false
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                IconButton(onClick = {
                    displayUsername = usernameInput
                    onUpdateUsername(usernameInput)
                    editingUsername = false
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.Black)
                }
                IconButton(onClick = {
                    usernameInput = displayUsername
                    editingUsername = false
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.Black)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayUsername,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit username",
                    tint = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { editingUsername = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.88f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = user.reviewsCount.toString(),
                    label = if (lang == AppLanguage.JP) "レビュー" else "Reviews"
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = Color.Black.copy(alpha = 0.12f)
                )
                StatItem(
                    value = user.ratingCount.toString(),
                    label = if (lang == AppLanguage.JP) "評価" else "Ratings"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.88f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                InfoRow(
                    label = if (lang == AppLanguage.JP) "メール" else "Email",
                    value = user.email
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.Black.copy(alpha = 0.12f)
                )
                InfoRow(
                    label = if (lang == AppLanguage.JP) "登録日" else "Member since",
                    value = user.memberSince?.toDate()?.let {
                        if (lang == AppLanguage.JP)
                            SimpleDateFormat("yyyy年M月", Locale.JAPANESE).format(it)
                        else
                            SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(it)
                    } ?: "—"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.88f))
                .clickable { onSignOut() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (lang == AppLanguage.JP) "ログアウト" else "Sign Out",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileImagePickerSheet(
    images: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = "Choose Profile Photo",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(images) { (gsUrl, httpsUrl) ->
                    AsyncImage(
                        model = httpsUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(gsUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
