package com.nikhil.journal.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nikhil.journal.ui.detail.EntryDetailScreen
import com.nikhil.journal.ui.detail.EntryDetailViewModel
import com.nikhil.journal.ui.edit.EditEntryScreen
import com.nikhil.journal.ui.edit.EditEntryViewModel
import com.nikhil.journal.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val DETAIL = "entry/{${EntryDetailViewModel.ARG_ENTRY_ID}}"
    const val EDIT = "edit?${EditEntryViewModel.ARG_ENTRY_ID}={${EditEntryViewModel.ARG_ENTRY_ID}}"

    fun detail(id: Long) = "entry/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?${EditEntryViewModel.ARG_ENTRY_ID}=$id"
}

@Composable
fun JournalNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewEntry = { navController.navigate(Routes.edit()) },
                onOpenEntry = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(EntryDetailViewModel.ARG_ENTRY_ID) { type = NavType.LongType }),
        ) { entry ->
            EntryDetailScreen(
                onBack = { navController.popIfCurrent(entry) },
                onEdit = { navController.navigate(Routes.edit(it)) },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(EditEntryViewModel.ARG_ENTRY_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            EditEntryScreen(onDone = { navController.popIfCurrent(entry) })
        }
    }
}

/** Pops [entry] only if it is still on top, so double taps / late callbacks can't pop twice. */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) popBackStack()
}
