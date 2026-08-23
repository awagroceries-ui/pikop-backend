package com.ng.pikop.core.di

import android.content.Context
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideApiService(tokenManager: TokenManager): ApiService {
        return ApiService.create(tokenManager)
    }

    @Provides
    @Named("dojahAppId")
    fun provideDojahAppId(): String = "6a82e5e060cd04847e04ce3d"

    @Provides
    @Named("dojahPublicKey")
    fun provideDojahPublicKey(): String = "test_pk_QJ6fDpLqE8itI3pbxTzLcheYa"

    @Provides
    @Named("premblyPublicKey")
    fun providePremblyPublicKey(): String = "live_pk_1b88a3b62d2a401b9ae2b925636ed455"

    @Provides
    @Named("premblyConfigId")
    fun providePremblyConfigId(): String = "YOUR_CONFIG_ID_HERE" // GET THIS FROM PREMBLY DASHBOARD
}
