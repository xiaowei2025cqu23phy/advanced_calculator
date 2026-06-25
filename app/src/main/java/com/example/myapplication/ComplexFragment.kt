package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentComplexBinding
import com.example.myapplication.viewmodel.ComplexViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * UI-only layer. Complex operations delegated to ComplexViewModel + ComplexRepository.
 */
@AndroidEntryPoint
class ComplexFragment : Fragment() {

    private var _binding: FragmentComplexBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ComplexViewModel
    private var activeInput: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentComplexBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            TextView(context).apply {
                text = "加载失败: ${e.javaClass.simpleName}"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1C1C1E.toInt())
                gravity = 17
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return

        try {
            viewModel = ViewModelProvider(this)[ComplexViewModel::class.java]

            // Observe result
            viewModel.result.observe(viewLifecycleOwner) { result ->
                result?.let {
                    if (it.isSuccess) {
                        binding.tvComplexResult.text = it.data
                    } else {
                        binding.tvComplexResult.text = it.error?.userMessage ?: "未知错误"
                    }
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
                if (loading) binding.tvComplexResult.text = "计算中..."
            }

            // Prevent system keyboard
            preventSysKb(binding.etComplexA)
            preventSysKb(binding.etComplexB)
            preventSysKb(binding.etComplexSingle)

            // Keyboard setup
            binding.kbView.apply {
                onInput = { appendToActive(it) }
                onBackspace = { backspace() }
                onClear = { clearActive() }
            }

            // Operation buttons → delegate to ViewModel
            binding.btnCadd.setOnClickListener { calc("+") }
            binding.btnCsub.setOnClickListener { calc("-") }
            binding.btnCmul.setOnClickListener { calc("*") }
            binding.btnCdiv.setOnClickListener { calc("/") }
            
            binding.btnCrect.setOnClickListener {
                viewModel.rectToPolar(binding.etComplexA.text.toString())
            }
            binding.btnCpolar.setOnClickListener {
                viewModel.polarToRect(binding.etComplexA.text.toString())
            }
            binding.btnCrect2.setOnClickListener {
                viewModel.rectToPolar(binding.etComplexSingle.text.toString())
            }
            binding.btnCpolar2.setOnClickListener {
                viewModel.polarToRect(binding.etComplexSingle.text.toString())
            }
            binding.btnConj.setOnClickListener {
                viewModel.conjugate(binding.etComplexSingle.text.toString())
            }
            binding.btnArg.setOnClickListener {
                viewModel.argument(binding.etComplexSingle.text.toString())
            }

        } catch (e: Exception) {
            binding.tvComplexResult.text = "初始化错误: ${e.javaClass.simpleName}"
        }
    }

    private fun calc(op: String) {
        val a = binding.etComplexA.text.toString().trim()
        val b = binding.etComplexB.text.toString().trim()
        viewModel.calculate(a, b, op)
    }

    // ── Keyboard input helpers ──

    private fun appendToActive(text: String) {
        activeInput = getFocused()
        activeInput?.append(text)
    }

    private fun backspace() {
        val et = getFocused() ?: return
        val t = et.text.toString()
        if (t.isNotEmpty()) et.setText(t.substring(0, t.length - 1))
    }

    private fun clearActive() {
        getFocused()?.setText("")
    }

    private fun getFocused(): EditText? {
        return when {
            binding.etComplexA.hasFocus() -> binding.etComplexA
            binding.etComplexB.hasFocus() -> binding.etComplexB
            binding.etComplexSingle.hasFocus() -> binding.etComplexSingle
            else -> binding.etComplexA
        }
    }

    private fun preventSysKb(et: EditText) {
        et.showSoftInputOnFocus = false
        et.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
