package com.example.feature.home.ui.modules

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TNEB_BILL_STATUS_URL = "https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillStatusRoute(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val openBillStatus = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TNEB_BILL_STATUS_URL))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open TNEB Bill Status right now.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TNEB Bill Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F7FB),
                    titleContentColor = Color(0xFF1E222D),
                    navigationIconContentColor = Color(0xFF1E222D)
                )
            )
        },
        containerColor = Color(0xFFF5F7FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Hero / Introduction ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Check Your Bill Status",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E222D)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "View your latest electricity bill status using your TNEB/TNPDCL service connection details.",
                fontSize = 15.sp,
                color = Color(0xFF7D8492),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── What you'll need ──────────────────────────────────────────
            Text(
                text = "What you'll need",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E222D)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val requirements = listOf(
                "Service Connection Number",
                "Registered Mobile Number",
                "CAPTCHA verification"
            )
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    requirements.forEach { requirement ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = requirement,
                                fontSize = 15.sp,
                                color = Color(0xFF1E222D)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Important ──────────────────────────────────────────────
            Text(
                text = "Important",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E222D)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF8E1),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Your registered mobile number must match the mobile number associated with your service connection.",
                            fontSize = 14.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You will complete the verification securely on the official TNEB/TNPDCL website.",
                            fontSize = 14.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Primary CTA ───────────────────────────────────────────────
            Button(
                onClick = openBillStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E7038), // Brand Green
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Check Bill Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
