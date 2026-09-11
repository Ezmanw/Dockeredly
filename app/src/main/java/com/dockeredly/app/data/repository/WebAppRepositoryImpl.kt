package com.dockeredly.app.data.repository

import com.dockeredly.app.data.database.WebAppDao
import com.dockeredly.app.data.database.WebAppEntity
import com.dockeredly.app.domain.model.IconSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.WebApp
import com.dockeredly.app.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebAppRepositoryImpl(private val dao: WebAppDao) : WebAppRepository {

    override fun observeWebApps(): Flow<List<WebApp>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeWebApp(id: String): Flow<WebApp?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getWebApp(id: String): WebApp? = dao.getById(id)?.toDomain()

    override suspend fun createWebApp(
        id: String,
        name: String,
        url: String,
        description: String,
        engine: RenderEngine,
        iconSource: IconSource,
    ): WebApp {
        val nextOrder = dao.maxSortOrder() + 1
        val entity = WebAppEntity(
            id = id,
            name = name,
            url = url,
            description = description,
            engine = engine.name,
            iconType = iconSource.typeTag(),
            iconSourceUrl = iconSource.sourceUrlOrNull(),
            iconLocalPath = iconSource.localPathOrNull(),
            sortOrder = nextOrder,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null,
            profileId = id,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun updateWebApp(
        id: String,
        name: String,
        url: String,
        description: String,
        engine: RenderEngine,
        iconSource: IconSource,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                name = name,
                url = url,
                description = description,
                engine = engine.name,
                iconType = iconSource.typeTag(),
                iconSourceUrl = iconSource.sourceUrlOrNull(),
                iconLocalPath = iconSource.localPathOrNull(),
            ),
        )
    }

    override suspend fun deleteWebApp(id: String) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    override suspend fun deleteAllWebApps() = dao.deleteAll()

    override suspend fun reorderWebApps(orderedIds: List<String>) = dao.reorder(orderedIds)

    override suspend fun markLaunched(id: String) =
        dao.updateLastUsed(id, System.currentTimeMillis())

    private fun IconSource.typeTag(): String = when (this) {
        is IconSource.WebsiteIcon -> "WEBSITE"
        is IconSource.UserImage -> "USER_IMAGE"
        IconSource.Fallback -> "FALLBACK"
    }

    private fun IconSource.sourceUrlOrNull(): String? =
        (this as? IconSource.WebsiteIcon)?.sourceUrl

    private fun IconSource.localPathOrNull(): String? = when (this) {
        is IconSource.WebsiteIcon -> localPath
        is IconSource.UserImage -> localPath
        IconSource.Fallback -> null
    }

    private fun WebAppEntity.toDomain(): WebApp = WebApp(
        id = id,
        name = name,
        url = url,
        description = description,
        engine = runCatching { RenderEngine.valueOf(engine) }.getOrDefault(RenderEngine.default()),
        iconSource = when (iconType) {
            "WEBSITE" -> iconLocalPath?.let { IconSource.WebsiteIcon(iconSourceUrl.orEmpty(), it) } ?: IconSource.Fallback
            "USER_IMAGE" -> iconLocalPath?.let { IconSource.UserImage(it) } ?: IconSource.Fallback
            else -> IconSource.Fallback
        },
        sortOrder = sortOrder,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        profileId = profileId,
    )
}
