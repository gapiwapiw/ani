package app.ani

import android.app.Application
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import app.ani.data.Repo
import app.ani.data.Resolver
import app.ani.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

/** Process-wide singletons. Created in [AniApplication] so services can use them after process death. */
object Graph {
    lateinit var app: Application
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val client: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    lateinit var store: Store
        private set
    lateinit var resolver: Resolver
        private set
    lateinit var repo: Repo
        private set

    /** Downloaded HLS data (segments, playlists). Shared by the download manager and offline playback. */
    @get:androidx.annotation.OptIn(UnstableApi::class)
    val cache: SimpleCache by lazy {
        SimpleCache(File(app.filesDir, "dlcache"), NoOpCacheEvictor(), StandaloneDatabaseProvider(app))
    }

    /** Used by Coil: adds the headers the resolver declares for image hosts. */
    val imageClient: OkHttpClient by lazy {
        client.newBuilder().addInterceptor { chain ->
            val b = chain.request().newBuilder()
            for ((k, v) in resolver.imageHeaders) b.header(k, v)
            chain.proceed(b.build())
        }.build()
    }

    fun init(app: Application) {
        this.app = app
        store = Store(File(app.filesDir, "state.json"), scope)
        resolver = Resolver(app, client)
        repo = Repo(resolver)
        scope.launch(Dispatchers.IO) { runCatching { resolver.init() } }
    }
}
