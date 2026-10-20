package com.shohankhan.hisabghor.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }

    Scaffold(
        topBar = { HisabTopBar(title = "অ্যাপ সম্পর্কে", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("হিসাবঘর", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "আপনার সব হিসাব, এক জায়গায়।",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "হিসাবঘর একটি সম্পূর্ণ অফলাইন ব্যক্তিগত হিসাবের অ্যাপ — দৈনন্দিন আয়-খরচ, " +
                        "দোকানের বাকির খাতা, ধার-দেনা, এনজিও ও ব্যাংক ঋণের কিস্তি, সঞ্চয়, বাজেট " +
                        "ও রিমাইন্ডার — সবকিছু এক জায়গায়, শুধুমাত্র আপনার ফোনেই।",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    KvRow("অ্যাপের নাম", "হিসাবঘর (HisabGhor)")
                    KvRow("সংস্করণ", BengaliNumber.toBengaliDigits(versionName))
                    KvRow("তৈরি করেছেন", "Shohan Khan")
                    KvRow("যোগাযোগ", "helloiamshohan@gmail.com")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:helloiamshohan@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "হিসাবঘর — মতামত")
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ইমেইল পাঠান") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("গোপনীয়তা ও নিরাপত্তা", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "• আপনার কোনো তথ্য ইন্টারনেটে পাঠানো হয় না — সব ডেটা এই ফোনেই থাকে।\n" +
                            "• অ্যাপ ব্যবহারে কোনো অ্যাকাউন্ট বা ইন্টারনেট লাগে না।\n" +
                            "• পিন দিয়ে অ্যাপ লক করা যায়; পিন কখনো সাদাভাবে সংরক্ষণ হয় না।\n" +
                            "• ব্যাকআপ ফাইল পাসওয়ার্ড দিয়ে এনক্রিপ্ট করা হয় — পাসওয়ার্ড ছাড়া কেউ পড়তে পারবে না।\n" +
                            "• পিন ভুলে গেলে ডেটা ফেরতের উপায় নেই (কোনো মাস্টার পিন নেই) — তাই নিয়মিত ব্যাকআপ রাখুন।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("হিসাবের নিয়ম", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "• ধার নেওয়া টাকা আয় নয়, ধার দেওয়া টাকা খরচ নয়।\n" +
                            "• এক অ্যাকাউন্ট থেকে অন্য অ্যাকাউন্টে টাকা সরালে (ট্রান্সফার/সঞ্চয়) আয়-খরচ বদলায় না।\n" +
                            "• দোকানে বাকি কেনা একবারই খরচ হয়; পরে পরিশোধে আবার খরচ ধরা হয় না।\n" +
                            "• ঋণের ক্ষেত্রে আসল, সুদ ও চার্জ সবসময় আলাদা রাখা হয়।\n" +
                            "• প্রতিটি লেনদেন দ্বৈত হিসাব (ডবল-এন্ট্রি) পদ্ধতিতে রাখা হয় — তাই ব্যালান্স সবসময় মিলে যায়।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                "এই অ্যাপে ওপেন-সোর্স লাইব্রেরি (AndroidX, Jetpack Compose, Room, Kotlin) ব্যবহার করা হয়েছে। কোনো ব্যাংক বা প্রতিষ্ঠানের লোগো/ব্র্যান্ড ব্যবহার করা হয়নি।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
