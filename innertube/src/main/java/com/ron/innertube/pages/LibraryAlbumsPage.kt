package com.ron.innertube.pages

import com.ron.innertube.models.Album
import com.ron.innertube.models.AlbumItem
import com.ron.innertube.models.Artist
import com.ron.innertube.models.ArtistItem
import com.ron.innertube.models.MusicResponsiveListItemRenderer
import com.ron.innertube.models.MusicTwoRowItemRenderer
import com.ron.innertube.models.PlaylistItem
import com.ron.innertube.models.SongItem
import com.ron.innertube.models.YTItem
import com.ron.innertube.models.oddElements
import com.ron.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
