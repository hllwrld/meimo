package com.stx.meimo.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stx.meimo.di.AppModule
import com.stx.meimo.ui.chat.ChatWebViewScreen
import com.stx.meimo.ui.chatlist.ChatListScreen
import com.stx.meimo.ui.chatlist.ChatListViewModel
import com.stx.meimo.ui.creator.CreatorScreen
import com.stx.meimo.ui.creator.CreatorViewModel
import com.stx.meimo.ui.home.HomeScreen
import com.stx.meimo.ui.home.HomeViewModel
import com.stx.meimo.ui.login.LoginScreen
import com.stx.meimo.ui.login.LoginViewModel
import com.stx.meimo.ui.profile.ProfileScreen
import com.stx.meimo.ui.profile.ProfileViewModel
import com.stx.meimo.ui.rewards.RewardsScreen
import com.stx.meimo.ui.rewards.RewardsViewModel
import com.stx.meimo.ui.search.SearchScreen
import com.stx.meimo.ui.search.SearchViewModel
import com.stx.meimo.ui.roledetail.RoleDetailScreen
import com.stx.meimo.ui.roledetail.RoleDetailViewModel
import com.stx.meimo.ui.webview.DebugWebViewScreen

@Composable
fun MeimoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onToggleHideImages: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val vm = viewModel<HomeViewModel> {
                HomeViewModel(AppModule.roleRepository)
            }
            HomeScreen(
                viewModel = vm,
                onRoleClick = { roleId ->
                    navController.navigate(Screen.RoleDetail(roleId).route)
                },
                onProfile = {
                    if (AppModule.authRepository.isLoggedIn) {
                        navController.navigate(Screen.Profile.route)
                    } else {
                        navController.navigate(Screen.Login.route)
                    }
                },
                onSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onDebugWebView = {
                    navController.navigate(Screen.DebugWebView.route)
                },
                onToggleHideImages = onToggleHideImages
            )
        }

        composable(Screen.ChatList.route) {
            val vm = viewModel<ChatListViewModel> {
                ChatListViewModel(AppModule.chatRepository)
            }
            ChatListScreen(
                viewModel = vm,
                onRoleChat = { roleId ->
                    navController.navigate(Screen.Chat(roleId).route)
                }
            )
        }

        composable(Screen.Creator.route) {
            val vm = viewModel<CreatorViewModel> {
                CreatorViewModel(AppModule.roleRepository)
            }
            CreatorScreen(
                viewModel = vm,
                onRoleClick = { roleId ->
                    navController.navigate(Screen.RoleDetail(roleId).route)
                }
            )
        }

        composable(Screen.Rewards.route) {
            val vm = viewModel<RewardsViewModel> {
                RewardsViewModel(AppModule.rewardRepository, AppModule.authRepository)
            }
            RewardsScreen(viewModel = vm)
        }

        composable(Screen.Login.route) {
            val vm = viewModel<LoginViewModel> {
                LoginViewModel(AppModule.authRepository)
            }
            LoginScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            val vm = viewModel<ProfileViewModel> {
                ProfileViewModel(AppModule.authRepository, AppModule.rewardRepository)
            }
            ProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RoleDetail.ROUTE,
            arguments = listOf(navArgument(Screen.RoleDetail.ARG_ROLE_ID) { type = NavType.LongType })
        ) {
            val roleId = it.arguments?.getLong(Screen.RoleDetail.ARG_ROLE_ID) ?: return@composable
            val vm = viewModel<RoleDetailViewModel> {
                RoleDetailViewModel(roleId, AppModule.roleRepository)
            }
            RoleDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onStartChat = { id ->
                    navController.navigate(Screen.Chat(id).route)
                }
            )
        }

        composable(
            route = Screen.Chat.ROUTE,
            arguments = listOf(navArgument(Screen.Chat.ARG_ROLE_ID) { type = NavType.LongType })
        ) {
            val roleId = it.arguments?.getLong(Screen.Chat.ARG_ROLE_ID) ?: return@composable
            ChatWebViewScreen(
                roleId = roleId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            val vm = viewModel<SearchViewModel> {
                SearchViewModel(AppModule.roleRepository)
            }
            SearchScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onRoleClick = { roleId ->
                    navController.navigate(Screen.RoleDetail(roleId).route)
                }
            )
        }

        composable(Screen.DebugWebView.route) {
            DebugWebViewScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title)
    }
}
