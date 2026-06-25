package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.HistoryEntity
import com.example.myapplication.viewmodel.ScientificViewModel

@Composable
fun ScientificScreen(viewModel: ScientificViewModel) {
    val resultState by viewModel.result.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val isDeg by viewModel.degreeMode.observeAsState(false)
    val history by viewModel.history.observeAsState(emptyList<HistoryEntity>())
    val symbolicResult by viewModel.symbolicResult.observeAsState()

    var expression by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp)
    ) {
        // Expression Display
        Text(
            text = expression,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            fontSize = 28.sp,
            color = Color.White,
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace
        )

        // Result Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val displayResult = when {
                    isLoading -> "计算中..."
                    symbolicResult != null -> symbolicResult!!
                    errorMessage != null -> errorMessage!!
                    resultState != null -> {
                        val res = resultState!!
                        if (res.isSuccess) "= ${viewModel.formatResult(res)}" else res.error?.userMessage ?: "错误"
                    }
                    else -> ""
                }
                
                Text(
                    text = displayResult,
                    fontSize = 22.sp,
                    color = Color(0xFFFF9F0A),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDeg) "DEG" else "RAD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9F0A),
                        modifier = Modifier
                            .background(Color(0x20FF9F0A))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { viewModel.toggleDegreeMode() }, modifier = Modifier.weight(1f)) {
                Text(if (isDeg) "DEG" else "RAD", fontSize = 11.sp)
            }
            Button(onClick = { viewModel.simplify(expression) }, modifier = Modifier.weight(1f)) {
                Text("简化", fontSize = 11.sp)
            }
            Button(onClick = { viewModel.differentiate(expression) }, modifier = Modifier.weight(1f)) {
                Text("求导", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history) { item ->
                Text(
                    text = "${item.expression} = ${item.result}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
