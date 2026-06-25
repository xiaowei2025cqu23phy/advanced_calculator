package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentMatrixBinding
import com.example.myapplication.viewmodel.MatrixViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * UI-only layer. Matrix operations delegated to MatrixViewModel + MatrixRepository.
 */
@AndroidEntryPoint
class MatrixFragment : Fragment() {

    private var _binding: FragmentMatrixBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MatrixViewModel
    private var focusedInput: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatrixBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MatrixViewModel::class.java]

        // Observe result
        viewModel.result.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    binding.tvMatrixResult.text = it.data
                } else {
                    binding.tvMatrixResult.text = it.error?.userMessage ?: "未知错误"
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.tvMatrixResult.text = "计算中..."
        }

        // Track focus for keyboard input
        binding.etMatrixA.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) focusedInput = v as EditText }
        binding.etMatrixB.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) focusedInput = v as EditText }

        // Keyboard setup
        binding.kbView.apply {
            onInput = { text -> focusedInput?.append(text) }
            onBackspace = {
                val t = focusedInput?.text?.toString() ?: ""
                if (t.isNotEmpty()) focusedInput?.setText(t.substring(0, t.length - 1))
            }
            onClear = { focusedInput?.setText("") }
            onEquals = { focusedInput?.append("\n") }
        }

        // Prevent system keyboard
        binding.etMatrixA.showSoftInputOnFocus = false
        binding.etMatrixA.setOnTouchListener { v, event -> v.onTouchEvent(event); true }
        binding.etMatrixB.showSoftInputOnFocus = false
        binding.etMatrixB.setOnTouchListener { v, event -> v.onTouchEvent(event); true }

        // Matrix separator buttons
        binding.btnMSepComma.setOnClickListener { focusedInput?.append(",") }
        binding.btnMSepSpace.setOnClickListener { focusedInput?.append(" ") }
        binding.btnMSepNl.setOnClickListener { focusedInput?.append("\n") }

        // Operation buttons → delegate to ViewModel
        binding.btnDeterminant.setOnClickListener { calc(MatrixViewModel.Op.DET) }
        binding.btnInverse.setOnClickListener { calc(MatrixViewModel.Op.INV) }
        binding.btnTranspose.setOnClickListener { calc(MatrixViewModel.Op.TRANS) }
        binding.btnTrace.setOnClickListener { calc(MatrixViewModel.Op.TRACE) }
        binding.btnAddMatrix.setOnClickListener { calc(MatrixViewModel.Op.ADD) }
        binding.btnSubMatrix.setOnClickListener { calc(MatrixViewModel.Op.SUB) }
        binding.btnMulMatrix.setOnClickListener { calc(MatrixViewModel.Op.MUL) }
    }

    private fun calc(op: MatrixViewModel.Op) {
        val a = binding.etMatrixA.text.toString().trim()
        val b = binding.etMatrixB.text.toString().trim()
        viewModel.calculate(op, a, b)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
