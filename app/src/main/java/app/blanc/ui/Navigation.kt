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

    /** The swipe-left collection of fixed, glassy 4x4 app Spaces. */
    data object Spaces : Screen

    data class Drawer(val mode: DrawerMode) : Screen

    /** The compact quick panel shown on home long-press. */
    data object QuickSettings : Screen
}
