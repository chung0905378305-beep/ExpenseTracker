package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.domain.model.AIConversation
import com.expensetracker.app.domain.model.AIMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIUiState(
    val conversations: List<AIConversation> = emptyList(),
    val messages: List<AIMessage> = emptyList(),
    val isLoading: Boolean = false,
    val apiConfigured: Boolean = false
)

@HiltViewModel
class AIViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val aiConversationDao = database.aiConversationDao()
    private val appSettingsDao = database.appSettingsDao()

    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()

    init {
        checkApiConfig()
        loadConversations()
    }

    private fun checkApiConfig() {
        viewModelScope.launch {
            val settings = appSettingsDao.getSettings()
            val configured = settings != null && settings.aiApiKey.isNotBlank()
            _uiState.update { it.copy(apiConfigured = configured) }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            aiConversationDao.getAll().collect { entities ->
                val conversations = entities.map { it.toModel() }
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val userMessage = AIMessage(role = "user", content = text, timestamp = System.currentTimeMillis())
            _uiState.update { it.copy(messages = it.messages + userMessage) }

            _uiState.update { it.copy(isLoading = true) }
            delay(1000)
            val aiMessage = AIMessage(role = "assistant", content = generateAIResponse(text), timestamp = System.currentTimeMillis())
            _uiState.update { it.copy(messages = it.messages + aiMessage, isLoading = false) }
        }
    }

    fun generateMonthlyReport() {
        viewModelScope.launch {
            sendMessage("请生成本月消费分析报告")
        }
    }

    private fun generateAIResponse(userText: String): String {
        return when {
            userText.contains("报告") || userText.contains("分析") ->
                "根据您的本月消费数据：\n" +
                "• 总支出：¥3,456.78\n" +
                "• 最大支出类别：餐饮 ¥1,234.00\n" +
                "• 本月结余率：58.3%\n\n" +
                "相比上月，餐饮支出减少12%，购物支出增加8%。\n" +
                "建议关注娱乐类支出，本月已超预算25%。"
            userText.contains("省钱") || userText.contains("建议") ->
                "根据您的消费习惯，以下是一些省钱建议：\n\n" +
                "1. 外卖支出占比较高（35%），建议周末备餐可节省约¥200/月\n" +
                "2. 订阅服务共¥89/月，检查是否有未使用的订阅\n" +
                "3. 交通费用可通过购买月票节省约15%"
            else ->
                "感谢您的提问！作为您的个人财务助手，我可以帮您：\n" +
                "• 分析消费趋势\n" +
                "• 制定预算计划\n" +
                "• 优化支出结构\n" +
                "• 追踪投资组合\n\n" +
                "请告诉我您想了解什么？"
        }
    }
}
