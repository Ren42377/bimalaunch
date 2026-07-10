package com.ren42377.bimalaunch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ren42377.bimalaunch.app.AppPreferences
import com.ren42377.bimalaunch.settings.SettingsSchema
import com.ren42377.bimalaunch.ui.screens.SettingsScreen
import com.ren42377.bimalaunch.ui.theme.BimalaunchTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BimalaunchTheme {
                val schema = SettingsSchema.fromNative()
                SettingsScreen(
                    schema = schema,
                    onBackClick = { finish() },
                    onTextChange = { key, value -> AppPreferences.setString(this, key, value) },
                    onChoiceChange = { key, value -> AppPreferences.setString(this, key, value) },
                    onSwitchChange = { key, value -> AppPreferences.setString(this, key, value.toString()) }
                )
            }
        }
    }
}
