package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MovieRecapScreen
import com.example.ui.theme.CineRecapTheme
import com.example.viewmodel.MovieRecapViewModel

class MainActivity : ComponentActivity() {

    private val recapViewModel: MovieRecapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineRecapTheme {
                MovieRecapScreen(viewModel = recapViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recapViewModel.stopAudio()
    }
}
