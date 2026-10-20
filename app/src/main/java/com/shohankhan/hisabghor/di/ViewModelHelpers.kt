package com.shohankhan.hisabghor.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shohankhan.hisabghor.HisabGhorApp

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember { (context.applicationContext as HisabGhorApp).container }
}

fun <VM : ViewModel> hisabViewModelFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
