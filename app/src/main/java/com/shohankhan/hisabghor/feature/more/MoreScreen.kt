package com.shohankhan.hisabghor.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.BuildConfig
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgListRow
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.IconChip
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.SectionHeader
import com.shohankhan.hisabghor.core.designsystem.Space

private data class MoreDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val routeKey: String
)

private data class MoreGroup(
    val title: String,
    val destinations: List<MoreDestination>
)

private val groups = listOf(
    MoreGroup(
        title = "খাতা ও ঋণ",
        destinations = listOf(
            MoreDestination("দোকানের খাতা", "বাকি কেনা ও পরিশোধের হিসাব", Icons.Filled.Storefront, "shops"),
            MoreDestination("ধার ও পাওনা", "মানুষের সঙ্গে ধার দেওয়া-নেওয়া", Icons.Filled.Handshake, "debts"),
            MoreDestination("এনজিও ও ব্যাংক ঋণ", "কিস্তি ও পরিশোধের তালিকা", Icons.Filled.AccountBalance, "loans")
        )
    ),
    MoreGroup(
        title = "পরিকল্পনা",
        destinations = listOf(
            MoreDestination("সঞ্চয়", "লক্ষ্য অনুযায়ী টাকা জমান", Icons.Filled.Savings, "savings"),
            MoreDestination("বাজেট", "ক্যাটাগরি অনুযায়ী খরচের সীমা", Icons.Filled.PieChart, "budgets"),
            MoreDestination("রিমাইন্ডার", "বিল ও কিস্তির মনে করিয়ে দেওয়া", Icons.Filled.Alarm, "reminders"),
            MoreDestination("নিয়মিত আয়/খরচ", "নিজে থেকে তৈরি হওয়া এন্ট্রি", Icons.Filled.Autorenew, "recurring")
        )
    ),
    MoreGroup(
        title = "তথ্য",
        destinations = listOf(
            MoreDestination("ক্যাটাগরি", "আয় ও খরচের ক্যাটাগরি সাজান", Icons.Filled.Category, "categories"),
            MoreDestination("অতিরিক্ত সম্পদ ও দায়", "জমি, সোনা ও অন্যান্য দায়", Icons.Filled.Diamond, "networth"),
            MoreDestination("সব জায়গায় খুঁজুন", "লেনদেন, দোকান, ব্যক্তি ও ঋণ", Icons.Filled.Search, "search")
        )
    ),
    MoreGroup(
        title = "নিরাপত্তা ও অ্যাপ",
        destinations = listOf(
            MoreDestination("সেটিংস", "পিন, নিরাপত্তা, ব্যাকআপ ও এক্সপোর্ট", Icons.Filled.Settings, "settings"),
            MoreDestination("অ্যাপ সম্পর্কে", "সংস্করণ, গোপনীয়তা ও যোগাযোগ", Icons.Filled.Info, "about")
        )
    )
)

@Composable
fun MoreScreen(onNavigate: (routeKey: String) -> Unit) {
    HgScreen(title = "আরও", insetBottom = false) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxxl)
        ) {
            item {
                BrandCard()
            }
            groups.forEach { group ->
                item(key = "h-${group.title}") {
                    SectionHeader(
                        title = group.title,
                        modifier = Modifier.padding(horizontal = ScreenPadding)
                    )
                }
                item(key = "g-${group.title}") {
                    HgCard(
                        Modifier.padding(horizontal = ScreenPadding),
                        padding = PaddingValues(vertical = Space.xs)
                    ) {
                        group.destinations.forEachIndexed { index, destination ->
                            HgListRow(
                                title = destination.title,
                                subtitle = destination.subtitle,
                                leading = {
                                    IconChip(
                                        destination.icon,
                                        tint = Hg.colors.brand,
                                        background = Hg.colors.brandTint
                                    )
                                },
                                trailing = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Hg.colors.inkTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = { onNavigate(destination.routeKey) }
                            )
                            if (index != group.destinations.lastIndex) {
                                HgDivider(startInset = 68)
                            }
                        }
                    }
                }
            }
            item {
                OfflineNote()
            }
        }
    }
}

@Composable
private fun BrandCard() {
    HgCard(
        Modifier.padding(horizontal = ScreenPadding, vertical = Space.md),
        padding = PaddingValues(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .padding(0.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.shohankhan.hisabghor.R.drawable.ic_brand_mark),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    "হিসাবঘর",
                    style = MaterialTheme.typography.titleLarge,
                    color = Hg.colors.ink
                )
                Text(
                    "আপনার সব হিসাব, এক জায়গায়",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hg.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = Hg.colors.inkTertiary
            )
        }
    }
}

@Composable
private fun OfflineNote() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Hg.colors.hairline)
        Spacer(Modifier.height(Space.lg))
        Text(
            "অফলাইন • কোনো ইন্টারনেট সংযোগ নেই",
            style = MaterialTheme.typography.labelMedium,
            color = Hg.colors.inkSecondary
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "সব তথ্য শুধু আপনার ফোনে সংরক্ষিত থাকে।",
            style = MaterialTheme.typography.bodySmall,
            color = Hg.colors.inkTertiary
        )
    }
}
