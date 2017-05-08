package org.freelectron.leobel.testlwa;

import android.app.Application;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import org.freelectron.leobel.testlwa.services.AlexaService;
import org.freelectron.leobel.testlwa.services.AlexaServiceImpl;
import org.freelectron.leobel.testlwa.services.PreferenceService;
import org.freelectron.leobel.testlwa.services.PreferenceServiceImpl;
import org.freelectron.leobel.testlwa.services.SecurityInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by leobel on 2/17/17.
 */

@Module
public class ServicesModule {

    @Singleton
    @Provides
    public PreferenceService providesPreferenceService(SharedPreferences sharedPreferences, ObjectMapper objectMapper){
        return new PreferenceServiceImpl(sharedPreferences, objectMapper);
    }

    @Singleton @Provides
    public ObjectMapper providesObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    @Singleton @Provides
    public OkHttpClient providesHttpClient(Application context, PreferenceService preferenceService){
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();


//            HttpLoggingInterceptor networkInterceptor = new HttpLoggingInterceptor();
//            networkInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//            httpClientBuilder.addInterceptor(networkInterceptor);

        // Security Interceptor
        SecurityInterceptor securityInterceptor = new SecurityInterceptor(preferenceService);
        httpClientBuilder.addInterceptor(securityInterceptor);

        List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);
        OkHttpClient httpClient = httpClientBuilder
                .protocols(protocols)
                .readTimeout(0, TimeUnit.MINUTES)
                .connectTimeout(0, TimeUnit.MINUTES)
                .build();
        return httpClient;
    }

    @Singleton @Provides @Named("ALEXA_API")
    public Retrofit providesRetrofit(ObjectMapper objectMapper, OkHttpClient httpClient){
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.ALEXA_BASE_API_URL + BuildConfig.ALEXA_API_VERSION + "/")
                .client(httpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    @Singleton @Provides
    public AlexaService providesAlexaService(@Named("ALEXA_API") Retrofit retrofit, ObjectMapper objectMapper){
        return new AlexaServiceImpl(retrofit, objectMapper);
    }
}
