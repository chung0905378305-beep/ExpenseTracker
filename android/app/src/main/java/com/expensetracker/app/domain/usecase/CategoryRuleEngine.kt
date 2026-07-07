package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CategoryRuleEngine @Inject constructor(
    private val categoryRepo: CategoryRepository
) {
    suspend fun suggestCategory(note: String, merchant: String?): Category? = withContext(Dispatchers.IO) {
        val allCategories = categoryRepo.getAllRoot() + categoryRepo.getAllRoot().flatMap { root ->
            categoryRepo.getChildren(root.id)
        }
        val searchText = (note + " " + (merchant ?: "")).lowercase()

        var bestMatch: Category? = null
        var bestScore = 0

        for (category in allCategories) {
            val keywords = category.keywords ?: emptyList()
            for (keyword in keywords) {
                if (searchText.contains(keyword.lowercase())) {
                    val score = keyword.length
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = category
                    }
                }
            }
        }

        bestMatch
    }
}
