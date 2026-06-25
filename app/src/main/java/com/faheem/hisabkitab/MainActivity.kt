package com.faheem.hisabkitab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.faheem.hisabkitab.ui.PolishedLedgerApp
import com.faheem.hisabkitab.ui.theme.HisabKitabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HisabKitabTheme {
                PolishedLedgerApp()
            }
        }
    }
}
