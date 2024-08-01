package com.shopwallet.ituchallenger;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("search")
    Call<List<Website>> searchWebsites(@Query("query") String query);
}
