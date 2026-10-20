package com.shohankhan.hisabghor.di

import android.content.Context

/**
 * Minimal manual dependency container (avoids extra DI frameworks).
 * Feature repositories are added here as the app grows.
 */
class AppContainer(@Suppress("unused") private val appContext: Context)
