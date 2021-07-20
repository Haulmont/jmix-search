/*
 * Copyright 2021 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.search.index;

import org.elasticsearch.common.settings.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Allows to configure Elasticsearch index settings.
 * <p>
 * Settings can be configured for all search indexes ({@link IndexSettingsConfigurationContext#commonSettingsBuilder()})
 * or for index related to specific entity ({@link IndexSettingsConfigurationContext#entitySettingsBuilder(Class)}).
 */
public class IndexSettingsConfigurationContext {

    protected Settings.Builder commonSettingsBuilder;
    protected Map<Class<?>, Settings.Builder> specificSettingsBuilders;

    public IndexSettingsConfigurationContext() {
        commonSettingsBuilder = Settings.builder();
        specificSettingsBuilders = new HashMap<>();
    }

    /**
     * Provides builder to set settings for all search indexes.
     * <p>
     * Use builder's 'put' methods to set settings values.
     *
     * @return ES index settings builder
     */
    public Settings.Builder commonSettingsBuilder() {
        return commonSettingsBuilder;
    }

    /**
     * Provides builder to set settings for index related to provided entity.
     * Value explicitly set for specific index overrides common value.
     * <p>
     * Use builder's 'put' methods to set settings values.
     *
     * @param entityClass entity class
     * @return ES index settings builder
     */
    public Settings.Builder entitySettingsBuilder(Class<?> entityClass) {
        return specificSettingsBuilders.computeIfAbsent(entityClass, key -> Settings.builder());
    }

    /**
     * Creates final settings for index related to provided entity as a combination of common and explicit settings.
     *
     * @param entityClass entity class
     * @return ES index settings
     */
    public Settings buildEffectiveSettingsForEntity(Class<?> entityClass) {
        Settings commonSettings = buildCommonSettings();
        Settings entitySettings = buildSpecificSettingsForEntity(entityClass);
        return Settings.builder()
                .put(commonSettings)
                .put(entitySettings)
                .build();
    }

    protected Settings buildCommonSettings() {
        return commonSettingsBuilder.build();
    }

    protected Settings buildSpecificSettingsForEntity(Class<?> entityClass) {
        return Optional.ofNullable(specificSettingsBuilders.get(entityClass)).orElse(Settings.builder()).build();
    }
}
