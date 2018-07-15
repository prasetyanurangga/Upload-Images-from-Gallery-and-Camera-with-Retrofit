package com.nuvola.carikost.uploadimageretrofit.service;


import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitService {

    @POST("/cari_kost/api/kirim_foto")
    Call<ResponseBody> event_store(@Body RequestBody file);
}
