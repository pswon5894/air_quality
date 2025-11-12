package com.cc.air_quality.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitConnection {
    companion object{
        private const val BASE_URL = "https://api.airvisual.com/v2/"
        private var INSTANCE: Retrofit? = null

        fun getInstace() : Retrofit{
            if(INSTANCE == null){
                INSTANCE = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return INSTANCE!!
            //!!확정연산자 null이면 에러 반환
        }
    }
}

//레트로핏 라이브러리 구성요소
//인터페이스, http메서드 정의
//레트로핏 클래스, 레트로핏 객체 생서
//데이터 클래스, Json 데이터를 담는