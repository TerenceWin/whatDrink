package com.whatdrink.app.ui.screens.map

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class PlaceType { VENDING_MACHINE, CONVENIENCE_STORE }

data class NearbyPlace(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String?,
    val type: PlaceType
)

@Serializable
private data class OverpassResponse(val elements: List<OverpassElement>)

@Serializable
private data class OverpassElement(
    val id: Long,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tags: Map<String, String> = emptyMap()
)

private val client = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// Japan bounding box: south, west, north, east
private const val JAPAN_BOUNDS = "bbox:30.0,129.0,45.5,146.0"

suspend fun fetchNearbyPlaces(lat: Double, lon: Double, radiusMeters: Int = 1500): List<NearbyPlace> {
    val query = """
        [out:json][$JAPAN_BOUNDS][timeout:25];
        (
          node["amenity"="vending_machine"](around:$radiusMeters,$lat,$lon);
          node["shop"="convenience"](around:$radiusMeters,$lat,$lon);
        );
        out body;
    """.trimIndent()

    val response: OverpassResponse = client.get("https://overpass-api.de/api/interpreter") {
        parameter("data", query)
    }.body()

    return response.elements.map { el ->
        val type = if (el.tags["amenity"] == "vending_machine")
            PlaceType.VENDING_MACHINE else PlaceType.CONVENIENCE_STORE
        NearbyPlace(
            id = el.id,
            lat = el.lat,
            lon = el.lon,
            name = el.tags["name"] ?: el.tags["brand"],
            type = type
        )
    }
}
