package com.nuvola.carikost.uploadimageretrofit.helper;



import com.nuvola.carikost.uploadimageretrofit.Conts;
import com.nuvola.carikost.uploadimageretrofit.service.RetrofitService;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UplodHelper {


        public static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        public static Retrofit.Builder builder = new Retrofit.Builder().baseUrl(Conts.base).addConverterFactory(GsonConverterFactory.create());

        public static RetrofitService createService(Class<RetrofitService> serviceClass)
        {
            Retrofit retrofit = builder.client(httpClient.build()).build();
            return retrofit.create(serviceClass);
        }
}

