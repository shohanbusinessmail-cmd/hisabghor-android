package com.shohankhan.hisabghor.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shohankhan.hisabghor.BuildConfig
import com.shohankhan.hisabghor.core.designsystem.HisabTopBar
import com.shohankhan.hisabghor.core.designsystem.KvRow
import com.shohankhan.hisabghor.core.money.BengaliNumber
import androidx.compose.foundation.layout.PaddingValues
import com.shohankhan.hisabghor.core.designsystem.Eyebrow
import com.shohankhan.hisabghor.core.designsystem.Hg
import com.shohankhan.hisabghor.core.designsystem.HgButton
import com.shohankhan.hisabghor.core.designsystem.HgButtonStyle
import com.shohankhan.hisabghor.core.designsystem.HgCard
import com.shohankhan.hisabghor.core.designsystem.HgDivider
import com.shohankhan.hisabghor.core.designsystem.HgNotice
import com.shohankhan.hisabghor.core.designsystem.HgScreen
import com.shohankhan.hisabghor.core.designsystem.HgTone
import com.shohankhan.hisabghor.core.designsystem.ScreenPadding
import com.shohankhan.hisabghor.core.designsystem.Space

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }

    HgScreen(
        title = "অ্যাপ সম্পর্কে",
        onBack = onBack
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            Spacer(Modifier.height(Space.sm))
            Column {
                Text(
                    "হিসাবঘর",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Hg.colors.ink
                )
                Text(
                    "আপনার সব হিসাব, এক জায়গায়।",
                    style = MaterialTheme.typography.titleSmall,
                    color = Hg.colors.brand
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    "হিসাবঘর একটি সম্পূর্ণ অফলাইন ব্যক্তিগত হিসাবের অ্যাপ — দৈনন্দিন আয়-খরচ, " +
                        "দোকানের বাকির খাতা, ধার-দেনা, এনজিও ও ব্যাংক ঋণের কিস্তি, সঞ্চয়, বাজেট " +
                        "ও রিমাইন্ডার — সবকিছু এক জায়গায়, শুধুমাত্র আপনার ফোনেই।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hg.colors.inkSecondary
                )
            }

            HgCard(padding = PaddingValues(vertical = Space.xs)) {
                AboutLine("অ্যাপের নাম", "হিসাবঘর (HisabGhor)")
                AboutLine("সংস্করণ", BengaliNumber.toBengaliDigits(versionName))
                AboutLine("তৈরি করেছেন", "Shohan Khan")
                AboutLine("যোগাযোগ", "helloiamshohan@gmail.com")
                Spacer(Modifier.height(Space.sm))
                HgButton(
                    text = "ইমেইল পাঠান",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:helloiamshohan@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "হিসাবঘর — মতামত")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    style = HgButtonStyle.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space.lg)
                )
                Spacer(Modifier.height(Space.sm))
            }

            HgCard(padding = PaddingValues(Space.lg)) {
                Eyebrow("গোপনীয়তা ও নিরাপত্তা")
                Spacer(Modifier.height(Space.sm))
                AboutBullet("আপনার কোনো তথ্য ইন্টারনেটে পাঠানো হয় না — সব ডেটা এই ফোনেই থাকে।")
                AboutBullet("অ্যাপ ব্যবহারে কোনো অ্যাকাউন্ট বা ইন্টারনেট লাগে না।")
                AboutBullet("পিন দিয়ে অ্যাপ লক করা যায়; পিন কখনো সাদাভাবে সংরক্ষণ হয় না।")
                AboutBullet("ব্যাকআপ ফাইল পাসওয়ার্ড দিয়ে এনক্রিপ্ট করা হয় — পাসওয়ার্ড ছাড়া কেউ পড়তে পারবে না।")
                AboutBullet("পিন ভুলে গেলে ডেটা ফেরতের উপায় নেই (কোনো মাস্টার পিন নেই) — তাই নিয়মিত ব্যাকআপ রাখুন।")
            }

            HgCard(padding = PaddingValues(Space.lg)) {
                Eyebrow("হিসাবের নিয়ম")
                Spacer(Modifier.height(Space.sm))
                AboutBullet("ধার নেওয়া টাকা আয় নয়, ধার দেওয়া টাকা খরচ নয়।")
                AboutBullet("এক অ্যাকাউন্ট থেকে অন্য অ্যাকাউন্টে টাকা সরালে (ট্রান্সফার/সঞ্চয়) আয়-খরচ বদলায় না।")
                AboutBullet("দোকানে বাকি কেনা একবারই খরচ হয়; পরে পরিশোধে আবার খরচ ধরা হয় না।")
                AboutBullet("ঋণের ক্ষেত্রে আসল, সুদ ও চার্জ সবসময় আলাদা রাখা হয়।")
                AboutBullet("প্রতিটি লেনদেন দ্বৈত হিসাব (ডবল-এন্ট্রি) পদ্ধতিতে রাখা হয় — তাই ব্যালেন্স সবসময় মিলে যায়।")
            }

            HgNotice(
                title = "ওপেন-সোর্স লাইব্রেরি",
                message = "AndroidX, Jetpack Compose, Room ও Kotlin ব্যবহার করা হয়েছে। " +
                    "কোনো ব্যাংক বা প্রতিষ্ঠানের লোগো/ব্র্যান্ড ব্যবহার করা হয়নি।",
                tone = HgTone.Neutral
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(Modifier.padding(vertical = Space.xs)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = Hg.colors.brand)
        Spacer(Modifier.width(Space.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Hg.colors.inkSecondary
        )
    }
}
