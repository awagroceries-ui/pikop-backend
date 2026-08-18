package com.ng.pikop.core.di

import com.ng.pikop.core.kyc.DojahKycRepository
import com.ng.pikop.core.kyc.KycManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KycModule {

    @Binds
    @Singleton
    abstract fun bindKycManager(
        dojahKycRepository: DojahKycRepository
    ): KycManager
}
