package com.example.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.User
import android.Manifest
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.LocationOn
import com.google.android.gms.location.LocationServices
import java.util.Locale
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFBF7)
                )
            )
        },
        containerColor = Color(0xFFFDFBF7)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
                        onSave = { name, username, mobile, address, lat, lng ->
                            viewModel.updateProfile(name, username, mobile, address, lat, lng)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    onSave: (String, String, String, String, Double?, Double?) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    var name by remember(user) { mutableStateOf(user.name ?: "") }
    var username by remember(user) { mutableStateOf(user.username) }
    var mobileNumber by remember(user) { mutableStateOf(user.mobileNumber ?: "") }
    var houseAddress by remember(user) { mutableStateOf(user.houseAddress ?: "") }
    var latitude by remember(user) { mutableStateOf(user.latitude) }
    var longitude by remember(user) { mutableStateOf(user.longitude) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    val usernameRegex = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,30}$".toRegex()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val addressText = listOfNotNull(
                                    address.subThoroughfare,
                                    address.thoroughfare,
                                    address.locality,
                                    address.adminArea,
                                    address.postalCode
                                ).joinToString(", ")
                                houseAddress = addressText
                            }
                        } catch (e: Exception) {
                            // Geocoder failed, keep coordinates and allow manual entry
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val primaryGreen = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { 
                if (isEditing) {
                    if (!usernameRegex.matches(username)) {
                        usernameError = "Invalid username format"
                        return@IconButton
                    } else {
                        usernameError = null
                    }
                    onSave(name, username, mobileNumber, houseAddress, latitude, longitude)
                }
                isEditing = !isEditing 
            }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Edit",
                    tint = primaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                if (usernameRegex.matches(it)) {
                    usernameError = null
                }
            },
            label = { Text("Username") },
            enabled = isEditing,
            isError = usernameError != null,
            supportingText = { usernameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = user.email ?: "Not linked",
            onValueChange = { },
            label = { Text("Google Account") },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mobileNumber,
            onValueChange = { mobileNumber = it },
            label = { Text("Mobile Number") },
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = houseAddress,
            onValueChange = { houseAddress = it },
            label = { Text("House Address") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )
        
        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Current Location")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!usernameRegex.matches(username)) {
                        usernameError = "Invalid username format"
                        return@Button
                    }
                    onSave(name, username, mobileNumber, houseAddress, latitude, longitude)
                    isEditing = false
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Save Changes", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}
