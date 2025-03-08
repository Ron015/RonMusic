package com.ron.music.models

import com.ron.innertube.models.YTItem
import com.ron.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
