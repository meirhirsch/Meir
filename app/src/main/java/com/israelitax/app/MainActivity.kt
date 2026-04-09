package com.israelitax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.israelitax.app.ui.screens.ResultsScreen
import com.israelitax.app.ui.screens.ReviewScreen
import com.israelitax.app.ui.screens.UploadScreen
import com.israelitax.app.ui.theme.IsraeliAmericaTaxTheme
import com.israelitax.app.ui.viewmodels.TaxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsraeliAmericaTaxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: TaxViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "upload"
                    ) {
                        composable("upload") {
                            UploadScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onNavigateToReview = { navController.navigate("review") }
                            )
                        }
                        composable("review") {
                            ReviewScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onNavigateToResults = {
                                    navController.navigate("results") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("results") {
                            ResultsScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onStartOver = {
                                    viewModel.resetSession()
                                    navController.navigate("upload") {
                                        popUpTo("upload") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
