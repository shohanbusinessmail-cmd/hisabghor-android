package com.shohankhan.hisabghor.di

import android.content.Context
import com.shohankhan.hisabghor.BuildConfig
import com.shohankhan.hisabghor.core.backup.BackupRepository
import com.shohankhan.hisabghor.core.database.HisabGhorDatabase
import com.shohankhan.hisabghor.core.security.AppLockManager
import com.shohankhan.hisabghor.core.security.KeystoreCrypto
import com.shohankhan.hisabghor.core.security.SecurityRepository
import com.shohankhan.hisabghor.core.security.hisabGhorDataStore
import com.shohankhan.hisabghor.data.AccountRepository
import com.shohankhan.hisabghor.data.AuditLogger
import com.shohankhan.hisabghor.data.CategoryRepository
import com.shohankhan.hisabghor.data.LedgerRepository
import com.shohankhan.hisabghor.data.LoanRepository
import com.shohankhan.hisabghor.data.PartyRepository
import com.shohankhan.hisabghor.data.PlanningRepository
import com.shohankhan.hisabghor.data.ReportRepository
import com.shohankhan.hisabghor.data.SavingsRepository
import com.shohankhan.hisabghor.data.SearchRepository
import com.shohankhan.hisabghor.data.ShopRepository
import com.shohankhan.hisabghor.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Manual dependency container (keeps the build free of DI-framework codegen).
 * One instance lives in [com.shohankhan.hisabghor.HisabGhorApp].
 */
class AppContainer(appContext: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: HisabGhorDatabase = HisabGhorDatabase.create(appContext)

    val auditLogger = AuditLogger(database.auditDao())

    val ledgerRepository = LedgerRepository(database, auditLogger)
    val accountRepository = AccountRepository(database, ledgerRepository)
    val categoryRepository = CategoryRepository(database)
    val transactionRepository = TransactionRepository(database, ledgerRepository)
    val shopRepository = ShopRepository(database, ledgerRepository)
    val partyRepository = PartyRepository(database, ledgerRepository)
    val loanRepository = LoanRepository(database, ledgerRepository, categoryRepository)
    val savingsRepository = SavingsRepository(database, ledgerRepository)
    val planningRepository = PlanningRepository(database, transactionRepository)
    val reportRepository = ReportRepository(
        database, ledgerRepository, accountRepository, planningRepository
    )
    val searchRepository = SearchRepository(database)

    val securityRepository = SecurityRepository(
        appContext.hisabGhorDataStore,
        KeystoreCrypto()
    )
    val appLockManager = AppLockManager(securityRepository, appScope)

    val receiptsDir: File = File(appContext.filesDir, "receipts").apply { mkdirs() }
    val backupRepository = BackupRepository(
        database, receiptsDir, auditLogger, BuildConfig.VERSION_NAME
    )
}
