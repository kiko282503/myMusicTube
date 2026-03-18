package com.musictube.player.service;

/**
 * HTTP Downloader implementation for NewPipe Extractor.
 * NewPipe requires this bridge to make its internal HTTP requests.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 \t2\u00020\u0001:\u0001\tB\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/musictube/player/service/NewPipeDownloader;", "Lorg/schabi/newpipe/extractor/downloader/Downloader;", "client", "Lokhttp3/OkHttpClient;", "(Lokhttp3/OkHttpClient;)V", "execute", "Lorg/schabi/newpipe/extractor/downloader/Response;", "request", "Lorg/schabi/newpipe/extractor/downloader/Request;", "Companion", "app_debug"})
public final class NewPipeDownloader extends org.schabi.newpipe.extractor.downloader.Downloader {
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "NewPipeDownloader";
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.musictube.player.service.NewPipeDownloader instance;
    @org.jetbrains.annotations.NotNull()
    public static final com.musictube.player.service.NewPipeDownloader.Companion Companion = null;
    
    private NewPipeDownloader(okhttp3.OkHttpClient client) {
        super();
    }
    
    @java.lang.Override()
    @kotlin.jvm.Throws(exceptionClasses = {org.schabi.newpipe.extractor.exceptions.ReCaptchaException.class, java.lang.Exception.class})
    @org.jetbrains.annotations.NotNull()
    public org.schabi.newpipe.extractor.downloader.Response execute(@org.jetbrains.annotations.NotNull()
    org.schabi.newpipe.extractor.downloader.Request request) throws org.schabi.newpipe.extractor.exceptions.ReCaptchaException, java.lang.Exception {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\u0006R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/musictube/player/service/NewPipeDownloader$Companion;", "", "()V", "TAG", "", "instance", "Lcom/musictube/player/service/NewPipeDownloader;", "getInstance", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.musictube.player.service.NewPipeDownloader getInstance() {
            return null;
        }
    }
}