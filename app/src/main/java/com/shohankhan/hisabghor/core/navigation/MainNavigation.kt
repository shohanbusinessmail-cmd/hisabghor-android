package com.shohankhan.hisabghor.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shohankhan.hisabghor.feature.dashboard.DashboardScreen

object Routes {
    const val Dashboard = "dashboard"
    const val Transactions = "transactions"
    const val Reports = "reports"
    const val Accounts = "accounts"
    const val More = "more"
    const val Search = "search"
    const val TransactionEditor = "transaction_editor/{kind}?editId={editId}"
    const val TransactionDetail = "transaction_detail/{id}"
    const val Transfer = "transfer?editId={editId}"
    const val AccountEditor = "account_editor?editId={editId}"
    const val AccountDetail = "account_detail/{id}"
    const val Categories = "categories"
    const val CategoryEditor = "category_editor?editId={editId}"
    const val Shops = "shops"
    const val ShopEditor = "shop_editor?editId={editId}"
    const val ShopDetail = "shop_detail/{id}"
    const val ShopPurchase = "shop_purchase/{shopId}?editId={editId}"
    const val ShopPayment = "shop_payment/{shopId}?editId={editId}"
    const val Debts = "debts"
    const val PersonEditor = "person_editor?editId={editId}"
    const val PersonDetail = "person_detail/{id}"
    const val AgreementEditor = "agreement_editor?personId={personId}&kind={kind}"
    const val AgreementDetail = "agreement_detail/{loanId}"
    const val Loans = "loans"
    const val LoanEditor = "loan_editor?kind={kind}"
    const val LoanDetail = "loan_detail/{id}"
    const val LoanPayment = "loan_payment/{loanId}?editId={editId}"
    const val Savings = "savings"
    const val GoalEditor = "goal_editor?editId={editId}"
    const val SavingsMove = "savings_move?goalId={goalId}&accountId={accountId}&withdraw={withdraw}&editId={editId}"
    const val Budgets = "budgets"
    const val Reminders = "reminders"
    const val Recurring = "recurring"
    const val NetWorthItems = "networth_items"
    const val Settings = "settings"
    const val About = "about"

    fun transactionEditor(kind: String, editId: Long = -1) = "transaction_editor/$kind?editId=$editId"
    fun transactionDetail(id: Long) = "transaction_detail/$id"
    fun transfer(editId: Long = -1) = "transfer?editId=$editId"
    fun accountEditor(editId: Long = -1) = "account_editor?editId=$editId"
    fun accountDetail(id: Long) = "account_detail/$id"
    fun categoryEditor(editId: Long = -1) = "category_editor?editId=$editId"
    fun shopEditor(editId: Long = -1) = "shop_editor?editId=$editId"
    fun shopDetail(id: Long) = "shop_detail/$id"
    fun shopPurchase(shopId: Long, editId: Long = -1) = "shop_purchase/$shopId?editId=$editId"
    fun shopPayment(shopId: Long, editId: Long = -1) = "shop_payment/$shopId?editId=$editId"
    fun personEditor(editId: Long = -1) = "person_editor?editId=$editId"
    fun personDetail(id: Long) = "person_detail/$id"
    fun agreementEditor(personId: Long, kind: String) = "agreement_editor?personId=$personId&kind=$kind"
    fun agreementDetail(loanId: Long) = "agreement_detail/$loanId"
    fun loanEditor(kind: String) = "loan_editor?kind=$kind"
    fun loanDetail(id: Long) = "loan_detail/$id"
    fun loanPayment(loanId: Long, editId: Long = -1) = "loan_payment/$loanId?editId=$editId"
    fun goalEditor(editId: Long = -1) = "goal_editor?editId=$editId"
    fun savingsMove(goalId: Long = -1, accountId: Long = -1, withdraw: Boolean = false, editId: Long = -1) =
        "savings_move?goalId=$goalId&accountId=$accountId&withdraw=$withdraw&editId=$editId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.Dashboard, "হোম", Icons.Filled.Home),
    Tab(Routes.Transactions, "লেনদেন", Icons.Filled.ReceiptLong),
    Tab(Routes.Reports, "রিপোর্ট", Icons.Filled.BarChart),
    Tab(Routes.Accounts, "অ্যাকাউন্ট", Icons.Filled.AccountBalanceWallet),
    Tab(Routes.More, "আরও", Icons.Filled.Apps)
)

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    onSearch = { navController.navigate(Routes.Search) },
                    onAddIncome = { navController.navigate(Routes.transactionEditor("INCOME")) },
                    onAddExpense = { navController.navigate(Routes.transactionEditor("EXPENSE")) },
                    onTransfer = { navController.navigate(Routes.transfer()) },
                    onOpenShops = { navController.navigate(Routes.Shops) },
                    onOpenDebts = { navController.navigate(Routes.Debts) },
                    onOpenLoans = { navController.navigate(Routes.Loans) },
                    onOpenSavings = { navController.navigate(Routes.Savings) },
                    onOpenBudgets = { navController.navigate(Routes.Budgets) },
                    onOpenReports = { navController.navigate(Routes.Reports) },
                    onSeeAllTransactions = { navController.navigate(Routes.Transactions) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            // Remaining destinations are registered with a temporary placeholder
            // and replaced feature-by-feature (see docs/FEATURE_STATUS.md).
            placeholder(Routes.Transactions)
            placeholder(Routes.Reports)
            placeholder(Routes.Accounts)
            placeholder(Routes.More)
            placeholder(Routes.Search)
            composable(
                Routes.TransactionEditor,
                arguments = listOf(
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { PlaceholderScreen() }
            composable(Routes.TransactionDetail) { PlaceholderScreen() }
            composable(
                Routes.Transfer,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(
                Routes.AccountEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(Routes.AccountDetail) { PlaceholderScreen() }
            placeholder(Routes.Categories)
            composable(
                Routes.CategoryEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            placeholder(Routes.Shops)
            composable(
                Routes.ShopEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(Routes.ShopDetail) { PlaceholderScreen() }
            composable(
                Routes.ShopPurchase,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(
                Routes.ShopPayment,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            placeholder(Routes.Debts)
            composable(
                Routes.PersonEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(Routes.PersonDetail) { PlaceholderScreen() }
            composable(
                Routes.AgreementEditor,
                arguments = listOf(
                    navArgument("personId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("kind") { type = NavType.StringType; defaultValue = "PERSONAL_BORROW" }
                )
            ) { PlaceholderScreen() }
            composable(Routes.AgreementDetail) { PlaceholderScreen() }
            placeholder(Routes.Loans)
            composable(
                Routes.LoanEditor,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType; defaultValue = "NGO" }
                )
            ) { PlaceholderScreen() }
            composable(Routes.LoanDetail) { PlaceholderScreen() }
            composable(
                Routes.LoanPayment,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            placeholder(Routes.Savings)
            composable(
                Routes.GoalEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { PlaceholderScreen() }
            composable(
                Routes.SavingsMove,
                arguments = listOf(
                    navArgument("goalId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("withdraw") { type = NavType.BoolType; defaultValue = false },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { PlaceholderScreen() }
            placeholder(Routes.Budgets)
            placeholder(Routes.Reminders)
            placeholder(Routes.Recurring)
            placeholder(Routes.NetWorthItems)
            placeholder(Routes.Settings)
            placeholder(Routes.About)
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.placeholder(route: String) {
    composable(route) { PlaceholderScreen() }
}

@Composable
private fun PlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("তৈরি হচ্ছে…", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Small helper so screens can pop with a NavHostController extension. */
fun NavHostController.pop() {
    popBackStack()
}
