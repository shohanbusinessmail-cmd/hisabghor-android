package com.shohankhan.hisabghor.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar

data class MoreDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val routeKey: String
)

@Composable
fun MoreScreen(onNavigate: (routeKey: String) -> Unit) {
    val destinations = listOf(
        MoreDestination("দোকানের খাতা", "বাকি কেনা ও পরিশোধের হিসাব", Icons.Filled.Storefront, "shops"),
        MoreDestination("ধার ও পাওনা", "মানুষের সঙ্গে ধার দেওয়া-নেওয়া", Icons.Filled.Handshake, "debts"),
        MoreDestination("এনজিও ও ব্যাংক ঋণ", "কিস্তির ঋণ ও পরিশোধের তালিকা", Icons.Filled.AccountBalance, "loans"),
        MoreDestination("সঞ্চয়", "লক্ষ্য অনুযায়ী টাকা জমান", Icons.Filled.Savings, "savings"),
        MoreDestination("বাজেট", "ক্যাটাগরি অনুযায়ী খরচের সীমা", Icons.Filled.PieChart, "budgets"),
        MoreDestination("রিমাইন্ডার", "বিল ও তারিখের মনে করিয়ে দেওয়া", Icons.Filled.Alarm, "reminders"),
        MoreDestination("নিয়মিত আয়/খরচ", "নিজে থেকে তৈরি হওয়া এন্ট্রি", Icons.Filled.Autorenew, "recurring"),
        MoreDestination("ক্যাটাগরি", "আয় ও খরচের ক্যাটাগরি সাজান", Icons.Filled.Category, "categories"),
        MoreDestination("অতিরিক্ত সম্পদ ও দায়", "জমি, সোনা ও অন্যান্য দায়", Icons.Filled.Diamond, "networth"),
        MoreDestination("খুঁজুন", "সব জায়গায় একসাথে খুঁজুন", Icons.Filled.Search, "search"),
        MoreDestination("সেটিংস", "পিন, নিরাপত্তা, ব্যাকআপ ও এক্সপোর্ট", Icons.Filled.Settings, "settings"),
        MoreDestination("অ্যাপ সম্পর্কে", "সংস্করণ, গোপনীয়তা ও যোগাযোগ", Icons.Filled.Info, "about")
    )

    Scaffold(
        topBar = { HisabTopBar(title = "আরও") }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(destinations, key = { it.routeKey }) { destination ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(destination.routeKey) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(destination.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            destination.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(start = 54.dp))
            }
        }
    }
}
