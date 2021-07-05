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

package test_support;/*
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

import io.jmix.core.DataManager;
import io.jmix.core.IdSerialization;
import io.jmix.core.Metadata;
import io.jmix.core.Stores;
import io.jmix.core.annotation.JmixModule;
import io.jmix.data.impl.liquibase.JmixLiquibase;
import io.jmix.data.impl.liquibase.LiquibaseChangeLogProcessor;
import io.jmix.search.index.impl.StartupIndexSynchronizer;
import io.jmix.search.index.queue.IndexingQueueManager;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import test_support.change_tracking.TestRootEntityIndexDefinition;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Configuration
@JmixModule
@Import({BaseSearchTestConfiguration.class})
public class EntityChangeTrackingTestConfiguration {

    @Autowired
    protected AutowireCapableBeanFactory beanFactory;

    @Bean("search_StartupIndexSynchronizer")
    @Primary
    public StartupIndexSynchronizer startupIndexSynchronizer() {
        return new TestNoopStartupIndexSynchronizer();
    }

    @Bean
    public List<Class<?>> testAutoDetectableIndexDefinitionClasses() {
        return Collections.singletonList(TestRootEntityIndexDefinition.class);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseChangeLogProcessor processor) {
        JmixLiquibase liquibase = new JmixLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLogContent(processor.createMasterChangeLog(Stores.MAIN));
        return liquibase;
    }

    @Bean
    public TestIndexingQueueItemsTracker testIndexingQueueItemsTracker(IdSerialization idSerialization) {
        return new TestIndexingQueueItemsTracker(idSerialization);
    }


    @Bean("search_JpaIndexingQueueManager")
    @Primary
    public IndexingQueueManager indexingQueueManager() {
        return beanFactory.createBean(TestJpaIndexingQueueManager.class);
    }

    @Bean
    public TestEntityCreator testEntityCreator(Metadata metadata, DataManager dataManager) {
        return new TestEntityCreator(metadata, dataManager);
    }
}
