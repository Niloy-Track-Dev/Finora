package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel()
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    if (isAppLocked) {
                        PINLockScreen(
                            viewModel = viewModel,
                            onUnlockSuccess = { viewModel.setAppPIN(viewModel.getAppPIN()) } // Unlocks by setting locked state to false
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Transactions, 2: Analytics, 3: Calendar, 4: More
    var showAddDialog by remember { mutableStateOf(false) }
    
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_home_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("nav_logs_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                    label = { Text("Reports") },
                    modifier = Modifier.testTag("nav_reports_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    modifier = Modifier.testTag("nav_calendar_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add cash movement", modifier = Modifier.size(24.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onAddTransactionClick = { showAddDialog = true }
                )
                1 -> TransactionsScreen(viewModel = viewModel)
                2 -> AnalyticsScreen(viewModel = viewModel)
                3 -> CalendarScreen(viewModel = viewModel)
                4 -> MoreScreen(viewModel = viewModel)
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onSave = { type, amount, title, category, date, sender, bankName, note, location, reason, subCategory ->
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        title = title,
                        category = category,
                        date = date,
                        sender = sender,
                        bankName = bankName,
                        note = note,
                        location = location,
                        reason = reason,
                        subCategory = subCategory
                    )
                    showAddDialog = false
                }
            )
        }
    }
}
