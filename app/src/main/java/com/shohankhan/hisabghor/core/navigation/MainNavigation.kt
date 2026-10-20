package com.shohankhan.hisabghor.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgBottomBar
import com.shohankhan.hisabghor.core.designsystem.HgNavItem
import com.shohankhan.hisabghor.feature.accounts.AccountDetailScreen
import com.shohankhan.hisabghor.feature.accounts.AccountEditorScreen
import com.shohankhan.hisabghor.feature.accounts.AccountListScreen
import com.shohankhan.hisabghor.feature.categories.CategoryEditorScreen
import com.shohankhan.hisabghor.feature.categories.CategoryListScreen
import com.shohankhan.hisabghor.feature.dashboard.DashboardScreen
import com.shohankhan.hisabghor.feature.debts.AgreementDetailScreen
import com.shohankhan.hisabghor.feature.debts.AgreementEditorScreen
import com.shohankhan.hisabghor.feature.debts.DebtListScreen
import com.shohankhan.hisabghor.feature.debts.PersonDetailScreen
import com.shohankhan.hisabghor.feature.debts.PersonEditorScreen
import com.shohankhan.hisabghor.feature.loans.LoanDetailScreen
import com.shohankhan.hisabghor.feature.more.MoreScreen
import com.shohankhan.hisabghor.feature.reports.ReportsScreen
import com.shohankhan.hisabghor.feature.search.SearchScreen
import com.shohankhan.hisabghor.feature.settings.AboutScreen
import com.shohankhan.hisabghor.feature.settings.SettingsScreen
import com.shohankhan.hisabghor.feature.loans.LoanEditorScreen
import com.shohankhan.hisabghor.feature.loans.LoanListScreen
import com.shohankhan.hisabghor.feature.loans.LoanPaymentScreen
import com.shohankhan.hisabghor.feature.planning.BudgetsScreen
import com.shohankhan.hisabghor.feature.planning.NetWorthItemsScreen
import com.shohankhan.hisabghor.feature.planning.RecurringScreen
import com.shohankhan.hisabghor.feature.planning.RemindersScreen
import com.shohankhan.hisabghor.feature.savings.GoalEditorScreen
import com.shohankhan.hisabghor.feature.savings.SavingsMoveScreen
import com.shohankhan.hisabghor.feature.savings.SavingsScreen
import com.shohankhan.hisabghor.feature.shops.ShopDetailScreen
import com.shohankhan.hisabghor.feature.shops.ShopEditorScreen
import com.shohankhan.hisabghor.feature.shops.ShopListScreen
import com.shohankhan.hisabghor.feature.shops.ShopPaymentScreen
import com.shohankhan.hisabghor.feature.shops.ShopPurchaseScreen
import com.shohankhan.hisabghor.feature.transactions.TransactionDetailScreen
import com.shohankhan.hisabghor.feature.transactions.TransactionEditorScreen
import com.shohankhan.hisabghor.feature.transactions.TransactionListScreen
import com.shohankhan.hisabghor.feature.transactions.TransferScreen

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

/**
 * Primary destinations. Five is the practical maximum for a bottom bar in
 * Bengali: labels stay single-line on 320 dp-wide screens.
 */
private val tabs = listOf(
    HgNavItem(Routes.Dashboard, "হোম", Icons.Outlined.Home, Icons.Filled.Home),
    HgNavItem(Routes.Transactions, "লেনদেন", Icons.Outlined.ReceiptLong, Icons.Filled.ReceiptLong),
    HgNavItem(Routes.Accounts, "হিসাব", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    HgNavItem(Routes.Reports, "রিপোর্ট", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    HgNavItem(Routes.More, "আরও", Icons.Outlined.Apps, Icons.Filled.Apps)
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
        containerColor = Hg.colors.canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                HgBottomBar(
                    items = tabs,
                    currentRoute = tabs.firstOrNull { tab ->
                        currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    }?.route,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
            composable(Routes.Transactions) {
                TransactionListScreen(
                    onAddTransaction = { navController.navigate(Routes.transactionEditor("EXPENSE")) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            composable(Routes.Reports) {
                ReportsScreen(
                    onOpenNetWorthItems = { navController.navigate(Routes.NetWorthItems) }
                )
            }
            composable(Routes.Accounts) {
                AccountListScreen(
                    onAddAccount = { navController.navigate(Routes.accountEditor()) },
                    onOpenAccount = { navController.navigate(Routes.accountDetail(it)) }
                )
            }
            composable(Routes.More) {
                MoreScreen(
                    onNavigate = { key ->
                        val route = when (key) {
                            "shops" -> Routes.Shops
                            "debts" -> Routes.Debts
                            "loans" -> Routes.Loans
                            "savings" -> Routes.Savings
                            "budgets" -> Routes.Budgets
                            "reminders" -> Routes.Reminders
                            "recurring" -> Routes.Recurring
                            "categories" -> Routes.Categories
                            "networth" -> Routes.NetWorthItems
                            "search" -> Routes.Search
                            "settings" -> Routes.Settings
                            "about" -> Routes.About
                            else -> Routes.More
                        }
                        navController.navigate(route)
                    }
                )
            }
            composable(Routes.Search) {
                SearchScreen(
                    onBack = { navController.pop() },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) },
                    onOpenShop = { navController.navigate(Routes.shopDetail(it)) },
                    onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                    onOpenLoan = { navController.navigate(Routes.loanDetail(it)) },
                    onOpenAgreement = { navController.navigate(Routes.agreementDetail(it)) },
                    onOpenAccount = { navController.navigate(Routes.accountDetail(it)) },
                    onOpenCategory = { navController.navigate(Routes.categoryEditor(it)) }
                )
            }
            composable(
                Routes.TransactionEditor,
                arguments = listOf(
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                TransactionEditorScreen(
                    kindArg = entry.arguments?.getString("kind") ?: "EXPENSE",
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.TransactionDetail) { entry ->
                TransactionDetailScreen(
                    transactionId = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(
                Routes.Transfer,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                TransferScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(
                Routes.AccountEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                AccountEditorScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.AccountDetail) { entry ->
                AccountDetailScreen(
                    accountId = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onEdit = { navController.navigate(Routes.accountEditor(it)) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            composable(Routes.Categories) {
                CategoryListScreen(
                    onBack = { navController.pop() },
                    onAddCategory = { navController.navigate(Routes.categoryEditor()) },
                    onEditCategory = { navController.navigate(Routes.categoryEditor(it)) }
                )
            }
            composable(
                Routes.CategoryEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                CategoryEditorScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.Shops) {
                ShopListScreen(
                    onAddShop = { navController.navigate(Routes.shopEditor()) },
                    onOpenShop = { navController.navigate(Routes.shopDetail(it)) }
                )
            }
            composable(
                Routes.ShopEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                ShopEditorScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.ShopDetail) { entry ->
                ShopDetailScreen(
                    shopId = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onEdit = { navController.navigate(Routes.shopEditor(it)) },
                    onNewPurchase = { navController.navigate(Routes.shopPurchase(it)) },
                    onNewPayment = { navController.navigate(Routes.shopPayment(it)) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            composable(
                Routes.ShopPurchase,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                ShopPurchaseScreen(
                    shopId = entry.arguments?.getString("shopId")?.toLongOrNull() ?: -1L,
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(
                Routes.ShopPayment,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                ShopPaymentScreen(
                    shopId = entry.arguments?.getString("shopId")?.toLongOrNull() ?: -1L,
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.Debts) {
                DebtListScreen(
                    onAddPerson = { navController.navigate(Routes.personEditor()) },
                    onOpenPerson = { navController.navigate(Routes.personDetail(it)) }
                )
            }
            composable(
                Routes.PersonEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                PersonEditorScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.PersonDetail) { entry ->
                PersonDetailScreen(
                    personId = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onEditPerson = { navController.navigate(Routes.personEditor(it)) },
                    onNewAgreement = { pid, kind ->
                        navController.navigate(Routes.agreementEditor(pid, kind))
                    },
                    onOpenAgreement = { navController.navigate(Routes.agreementDetail(it)) }
                )
            }
            composable(
                Routes.AgreementEditor,
                arguments = listOf(
                    navArgument("personId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("kind") { type = NavType.StringType; defaultValue = "PERSONAL_BORROW" }
                )
            ) { entry ->
                AgreementEditorScreen(
                    personId = entry.arguments?.getLong("personId") ?: -1L,
                    kindArg = entry.arguments?.getString("kind") ?: "PERSONAL_BORROW",
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.AgreementDetail) { entry ->
                AgreementDetailScreen(
                    loanId = entry.arguments?.getString("loanId")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onRecordPayment = { navController.navigate(Routes.loanPayment(it)) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            composable(Routes.Loans) {
                LoanListScreen(
                    onNewLoan = { kind -> navController.navigate(Routes.loanEditor(kind)) },
                    onOpenLoan = { navController.navigate(Routes.loanDetail(it)) }
                )
            }
            composable(
                Routes.LoanEditor,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType; defaultValue = "NGO" }
                )
            ) { entry ->
                LoanEditorScreen(
                    kindArg = entry.arguments?.getString("kind") ?: "NGO",
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.LoanDetail) { entry ->
                LoanDetailScreen(
                    loanId = entry.arguments?.getString("id")?.toLongOrNull() ?: -1L,
                    onBack = { navController.pop() },
                    onRecordPayment = { navController.navigate(Routes.loanPayment(it)) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) }
                )
            }
            composable(
                Routes.LoanPayment,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                LoanPaymentScreen(
                    loanId = entry.arguments?.getString("loanId")?.toLongOrNull() ?: -1L,
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(Routes.Savings) {
                SavingsScreen(
                    onAddGoal = { navController.navigate(Routes.goalEditor()) },
                    onEditGoal = { navController.navigate(Routes.goalEditor(it)) },
                    onMove = { goalId, accountId, withdraw ->
                        navController.navigate(Routes.savingsMove(goalId, accountId, withdraw))
                    }
                )
            }
            composable(
                Routes.GoalEditor,
                arguments = listOf(navArgument("editId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                GoalEditorScreen(
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() }
                )
            }
            composable(
                Routes.SavingsMove,
                arguments = listOf(
                    navArgument("goalId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("withdraw") { type = NavType.BoolType; defaultValue = false },
                    navArgument("editId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { entry ->
                SavingsMoveScreen(
                    goalId = entry.arguments?.getLong("goalId") ?: -1L,
                    accountId = entry.arguments?.getLong("accountId") ?: -1L,
                    withdraw = entry.arguments?.getBoolean("withdraw") ?: false,
                    editId = entry.arguments?.getLong("editId") ?: -1L,
                    onBack = { navController.pop() },
                    onSaved = { navController.pop() },
                    onCreateSavingsAccount = { navController.navigate(Routes.accountEditor()) }
                )
            }
            composable(Routes.Budgets) {
                BudgetsScreen(onBack = { navController.pop() })
            }
            composable(Routes.Reminders) {
                RemindersScreen(onBack = { navController.pop() })
            }
            composable(Routes.Recurring) {
                RecurringScreen(onBack = { navController.pop() })
            }
            composable(Routes.NetWorthItems) {
                NetWorthItemsScreen(onBack = { navController.pop() })
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onBack = { navController.pop() },
                    onOpenAbout = { navController.navigate(Routes.About) }
                )
            }
            composable(Routes.About) {
                AboutScreen(onBack = { navController.pop() })
            }
        }
    }
}

/** Small helper so screens can pop with a NavHostController extension. */
fun NavHostController.pop() {
    popBackStack()
}
