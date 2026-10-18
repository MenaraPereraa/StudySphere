package lk.menara.studysphere.util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OkHTTPClient {

    private static volatile OkHttpClient instance;

    private OkHTTPClient() { }

    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (OkHTTPClient.class) {
                if (instance == null) {
                    instance = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return instance;
    }
}