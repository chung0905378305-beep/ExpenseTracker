package com.expensetracker.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.ui.screen.AIScreen
import com.expensetracker.app.ui.screen.AddSubscriptionScreen
import com.expensetracker.app.ui.screen.AssetsScreen
import com.expensetracker.app.ui.screen.EditTransactionScreen
import com.expensetracker.app.ui.screen.HoldingDetailScreen
import com.expensetracker.app.ui.screen.ProfileScreen
import com.expensetracker.app.ui.screen.StatsScreen
import com.expensetracker.app.ui.screen.TransactionDetailScreen
import com.expensetracker.app.ui.screen.TransactionsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0
)

@Composable
fun MainScreen(
    pendingTransactionCount: Int = 0,
    upcomingSubscriptionCount: Int = 0,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("明细", Icons.Default.Receipt, Screen.Transactions.route, pendingTransactionCount),
        BottomNavItem("统计", Icons.Default.BarChart, Screen.Stats.route, upcomingSubscriptionCount),
        BottomNavItem("资产", Icons.Default.AccountBalance, Screen.Assets.route),
        BottomNavItem("我的", Icons.Default.Person, Screen.Profile.route)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.dp.times(8)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Transactions.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (item.badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(text = item.badgeCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                }
                            },
                            label = { Text(text = item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onTransactionClick = { id ->
                        navController.navigate(Screen.TransactionDetail(id).route)
                    },
                    onAddClick = {
                        navController.navigate(Screen.EditTransaction(null).route)
                    }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(
                    onCategoryClick = { categoryId ->
                        navController.navigate(Screen.EditTransaction(null).route)
                    }
                )
            }
            composable(Screen.Assets.route) {
                AssetsScreen(
                    onHoldingClick = { id ->
                        navController.navigate(Screen.HoldingDetail(id).route)
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onAIClick = {
                        navController.navigate(Screen.AIAssistant.route)
                    }
                )
            }
            composable(Screen.TransactionDetail(0).route) { backStackEntry ->
                val route = backStackEntry.arguments?.getString(androidx.navigation.NavController.KEY_ROUTE)
                    ?: backStackEntry.destination?.route ?: ""
                val id = route.removePrefix("transaction_detail/").toLongOrNull() ?: 0L
                TransactionDetailScreen(
                    transactionId = id,
                    onEditClick = { editId ->
                        navController.navigate(Screen.EditTransaction(editId).route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.HoldingDetail(0).route) { backStackEntry ->
                val route = backStackEntry.arguments?.getString(androidx.navigation.NavController.KEY_ROUTE)
                    ?: backStackEntry.destination?.route ?: ""
                val id = route.removePrefix("holding_detail/").toLongOrNull() ?: 0L
                HoldingDetailScreen(
                    holdingId = id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.EditTransaction(null).route) { backStackEntry ->
                val route = backStackEntry.arguments?.getString(androidx.navigation.NavController.KEY_ROUTE)
                    ?: backStackEntry.destination?.route ?: ""
                val idStr = route.removePrefix("edit_transaction/")
                val id = idStr.toLongOrNull()?.takeIf { it != -1L }
                EditTransactionScreen(
                    transactionId = id,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.AddSubscription.route) {
                AddSubscriptionScreen(
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.AIAssistant.route) {
                AIScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
