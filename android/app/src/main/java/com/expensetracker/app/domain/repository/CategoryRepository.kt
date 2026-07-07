package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.CategoryDao
import com.expensetracker.app.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRepository {
    suspend fun getAllRoot(): List<Category>
    suspend fun getChildren(parentId: Long): List<Category>
    suspend fun search(query: String): List<Category>
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun delete(id: Long)
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : CategoryRepository {

    private val categoryDao: CategoryDao = database.categoryDao()

    override suspend fun getAllRoot(): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.getAllRoot().map { it.toModel() }
    }

    override suspend fun getChildren(parentId: Long): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.getChildren(parentId).map { it.toModel() }
    }

    override suspend fun search(query: String): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.search(query).map { it.toModel() }
    }

    override suspend fun insert(category: Category): Long = withContext(Dispatchers.IO) {
        categoryDao.insert(category.toEntity())
    }

    override suspend fun update(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        categoryDao.deleteById(id)
    }
}
