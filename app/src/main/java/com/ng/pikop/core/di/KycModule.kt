package com.ng.pikop.core.di

import com.ng.pikop.core.kyc.KycManager
import com.ng.pikop.core.kyc.ResilientKycManager
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
        resilientKycManager: ResilientKycManager
    ): KycManager
}
