package com.example.myapplication

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.ComposeView
import com.example.myapplication.databinding.FragmentScientificBinding
import com.example.myapplication.ui.ScientificScreen
import com.example.myapplication.viewmodel.ScientificViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Modernized UI Layer using Jetpack Compose.
 */
@AndroidEntryPoint
class ScientificFragment : Fragment() {

    private lateinit var viewModel: ScientificViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val vm = ViewModelProvider(this@ScientificFragment)[ScientificViewModel::class.java]
                ScientificScreen(vm)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ScientificViewModel::class.java]
    }
}
