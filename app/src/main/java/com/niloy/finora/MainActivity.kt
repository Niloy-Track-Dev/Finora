package com.niloy.finora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niloy.finora.ui.components.AddTransactionDialog
import com.niloy.finora.ui.screens.*
import com.niloy.finora.ui.theme.BluePrimary
import com.niloy.finora.ui.theme.MyApplicationTheme
import com.niloy.finora.ui.viewmodel.FinanceViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FinanceViewModel = viewModel()
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAppLocked) {
                        PINLockScreen(
                            viewModel = viewModel,
                            onUnlockSuccess = { viewModel.setAppPIN(viewModel.getAppPIN()) }
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8) {
                    if (isBottomBarVisible) isBottomBarVisible = false
                } else if (available.y > 8) {
                    if (!isBottomBarVisible) isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val categories by viewModel.allCategories.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 10.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_analytics_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Logs") },
                        label = { Text("Logs") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_logs_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = "Track") },
                        label = { Text("Track") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_track_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Setting") },
                        label = { Text("Setting") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_setting_tab")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 2) {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = BluePrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = modifier.nestedScroll(nestedScrollConnection)
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
                1 -> AnalyticsScreen(viewModel = viewModel)
                2 -> LogsScreen(viewModel = viewModel)
                3 -> TrackScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
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
