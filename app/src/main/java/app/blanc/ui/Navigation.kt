package app.blanc.ui

/** Which slot the drawer is picking an app for, or plain launch mode. */
sealed interface DrawerMode {
    data object Launch : DrawerMode

    /** Assign the chosen app to home slot [index] (index == current size appends). */
    data class AssignHome(val index: Int) : DrawerMode
}

/** Top-level screens. Blanc has no real back stack: back always returns Home. */
sealed interface Screen {
    data object Home : Screen
    data class Drawer(val mode: DrawerMode) : Screen
    data object Settings : Screen
}
