package com.expensetracker.app.ui.navigation

sealed class Screen(val route: String) {
    data object Transactions : Screen("transactions")
    data object Stats : Screen("stats")
    data object Assets : Screen("assets")
    data object Profile : Screen("profile")
    data class TransactionDetail(val transactionId: Long) : Screen("transaction_detail/$transactionId")
    data class HoldingDetail(val holdingId: Long) : Screen("holding_detail/$holdingId")
    data class EditTransaction(val transactionId: Long? = null) : Screen("edit_transaction/${transactionId ?: -1}")
    data object AddSubscription : Screen("add_subscription")
    data object AIAssistant : Screen("ai_assistant")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when {
                route == Transactions.route -> Transactions
                route == Stats.route -> Stats
                route == Assets.route -> Assets
                route == Profile.route -> Profile
                route?.startsWith("transaction_detail/") == true -> {
                    val id = route.removePrefix("transaction_detail/").toLongOrNull() ?: 0L
                    TransactionDetail(id)
                }
                route?.startsWith("holding_detail/") == true -> {
                    val id = route.removePrefix("holding_detail/").toLongOrNull() ?: 0L
                    HoldingDetail(id)
                }
                route?.startsWith("edit_transaction/") == true -> {
                    val id = route.removePrefix("edit_transaction/").toLongOrNull()
                    EditTransaction(if (id == -1L) null else id)
                }
                route == AddSubscription.route -> AddSubscription
                route == AIAssistant.route -> AIAssistant
                else -> Transactions
            }
        }
    }
}
