/*
 * Copyright (C) 2017. OpenLattice, Inc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the owner of the copyright at support@openlattice.com
 *
 */
package com.openlattice.chronicle.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import retrofit2.Retrofit
import com.openlattice.chronicle.serialization.ChronicleJacksonConverterFactory
import com.openlattice.chronicle.serialization.ChronicleCallAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public object RetrofitBuilders {
    @JvmField
    public val mapper: ObjectMapper = ObjectMapper()

    init {
        mapper.registerModule(GuavaModule())
        mapper.registerModule(JodaModule())
        mapper.registerModule(JavaTimeModule())
        mapper.registerKotlinModule()
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        //        mapper.registerModule( new AfterburnerModule() );
    }

    @JvmStatic
    public fun okHttpClient(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
    }

    @JvmStatic
    public fun decorateWithRhizomeFactories(builder: Retrofit.Builder): Retrofit.Builder {
        return builder
            .addConverterFactory(ChronicleJacksonConverterFactory(mapper))
            .addCallAdapterFactory(ChronicleCallAdapterFactory())
    }

    @JvmStatic
    public fun createBaseChronicleRetrofitBuilder(baseUrl: String, httpClient: OkHttpClient): Retrofit.Builder {
        return Retrofit.Builder().baseUrl(baseUrl).client(httpClient)
    }
}