package com.whatdrink.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
private data class YahooSearchResponse(
    val hits: List<YahooItem> = emptyList()
)

@Serializable
private data class YahooItem(
    val name: String? = null,
    val brand: YahooBrand? = null,
    val genreCategory: YahooGenreCategory? = null,
    val image: YahooImage? = null,
    val description: String? = null
)

@Serializable
private data class YahooBrand(val name: String? = null)

@Serializable
private data class YahooGenreCategory(val name: String? = null)

@Serializable
private data class YahooImage(val medium: String? = null)

// ── 给 Repository 用的干净数据结构 ──
data class YahooDrinkResult(
    val barcode: String,
    val nameEn: String,
    val nameJa: String,
    val brand: String,
    val category: String,
    val imageUrl: String,
    val descriptionEn: String,
    val descriptionJa: String
)

private const val YAHOO_APP_ID = "dmVyPTIwMjUwNyZpZD1BNEsyblFOQk11Jmhhc2g9WkRJeFlXSTVaRGswTTJZd1ptVmlZdw"

private val yahooClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

object YahooShoppingApi {
    suspend fun fetchByBarcode(barcode: String): YahooDrinkResult? {
        val response: YahooSearchResponse = yahooClient.get(
            "https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch"
        ) {
            parameter("appid", YAHOO_APP_ID)
            parameter("jan_code", barcode)
            parameter("results", 1)
        }.body()

        val item = response.hits.firstOrNull() ?: return null
        val name = item.name ?: return null

        return YahooDrinkResult(
            barcode = barcode,
            nameEn = name,
            nameJa = name,
            brand = item.brand?.name ?: "",
            category = item.genreCategory?.name ?: "",
            imageUrl = item.image?.medium ?: "",
            descriptionEn = item.description ?: "",
            descriptionJa = item.description ?: ""
        )
    }
}