package com.ren42377.bimalaunch

internal enum class PermissionStage(val order: Int) {
    MEDIA(3),
    INSTALLER(4)
}

internal data class PermissionUiSnapshot(
    val stage: PermissionStage,
    val iconRes: Int,
    val title: String,
    val description: String,
    val buttonText: String
)

internal sealed interface StartupDestination {
    val order: Int
    val progress: Float

    data object LicenseLoading : StartupDestination {
        override val order = 0
        override val progress = 0.3f
    }

    data object LicenseInput : StartupDestination {
        override val order = 1
        override val progress = 0.4f
    }

    data class Permission(val snapshot: PermissionUiSnapshot) : StartupDestination {
        override val order = snapshot.stage.order
        override val progress = 0.75f
    }

    data object WebView : StartupDestination {
        override val order = 5
        override val progress = 1f
    }
}
