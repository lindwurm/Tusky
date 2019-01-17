package com.keylesspalace.tusky.network;

import com.keylesspalace.tusky.entity.SearchResults;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NotestockApi {

    @GET("api/v1/search.json")
    Call<SearchResults> search(@Query("q") String q);

}
