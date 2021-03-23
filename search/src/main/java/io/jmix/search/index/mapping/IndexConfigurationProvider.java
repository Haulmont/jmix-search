/*
 * Copyright 2020 Haulmont.
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

package io.jmix.search.index.mapping;

import io.jmix.core.InstanceNameProvider;
import io.jmix.core.impl.scanning.JmixModulesClasspathScanner;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.search.index.IndexConfiguration;
import io.jmix.search.index.mapping.processor.AnnotatedIndexDefinitionProcessor;
import io.jmix.search.index.mapping.processor.IndexDefinitionDetector;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("search_IndexConfigurationProvider")
public class IndexConfigurationProvider {

    private static final Logger log = LoggerFactory.getLogger(IndexConfigurationProvider.class);

    protected final Registry registry;

    @Autowired
    public IndexConfigurationProvider(JmixModulesClasspathScanner classpathScanner,
                                      AnnotatedIndexDefinitionProcessor builder,
                                      InstanceNameProvider instanceNameProvider) {
        Set<String> classNames = classpathScanner.getClassNames(IndexDefinitionDetector.class);
        log.debug("Create Index Configurations");

        Registry registry = new Registry(instanceNameProvider);
        classNames.stream().map(builder::createIndexConfiguration).forEach(registry::registerIndexDefinition);

        this.registry = registry;
    }

    /**
     * Gets all {@link IndexConfiguration} registered in application
     * @return all {@link IndexConfiguration}
     */
    public Collection<IndexConfiguration> getIndexConfigurations() {
        return registry.getIndexDefinitions();
    }

    /**
     * Gets {@link IndexConfiguration} registered for provided entity name.
     * @param entityName entity name
     * @return {@link IndexConfiguration}
     */
    @Nullable
    public IndexConfiguration getIndexDefinitionByEntityName(String entityName) {
        return registry.getIndexDefinitionByEntityName(entityName);
    }

    /**
     * Gets {@link IndexConfiguration} registered for provided index name.
     * @param indexName index name
     * @return {@link IndexConfiguration}
     */
    @Nullable
    public IndexConfiguration getIndexDefinitionByIndexName(String indexName) {
        return registry.getIndexDefinitionByIndexName(indexName);
    }

    /**
     * Checks if provided entity is declared to be indexed directly (not as a part of another entity).
     * @param entityName entity name
     * @return true if entity is indexed, false otherwise
     */
    public boolean isDirectlyIndexed(String entityName) {
        return registry.hasDefinitionForEntity(entityName);
    }

    /**
     * Checks if provided entity is involved in index process directly or as a part of another entity.
     * @param entityClass entity java class
     * @return true if entity is involved in index process, false otherwise
     */
    public boolean isAffectedEntityClass(Class<?> entityClass) {
        return registry.isEntityClassRegistered(entityClass);
    }

    /**
     * Gets metadata of entities dependent on updated main entity and its changed properties.
     * @param entityClass java class of main entity
     * @param changedProperties changed property of main entity
     * @return dependent entities grouped by their {@link MetaClass}.
     * For every meta class group there are set of properties representing dependency-to-main references
     */
    public Map<MetaClass, Set<MetaPropertyPath>> getDependenciesMetaDataForUpdate(Class<?> entityClass, Set<String> changedProperties) {
        log.debug("Get dependencies metadata for class {} with changed properties: {}", entityClass, changedProperties);
        Map<String, Set<MetaPropertyPath>> backRefProperties = registry.getBackRefPropertiesForUpdate(entityClass);
        if (MapUtils.isEmpty(backRefProperties)) {
            return Collections.emptyMap();
        }

        Map<MetaClass, Set<MetaPropertyPath>> result = new HashMap<>();

        changedProperties.stream()
                .flatMap(changedProperty -> {
                    Set<MetaPropertyPath> metaPropertyPaths = backRefProperties.get(changedProperty);
                    return metaPropertyPaths == null ? Stream.empty() : metaPropertyPaths.stream();
                })
                .forEach(property -> {
                    MetaClass metaClass = property.getMetaClass();
                    Set<MetaPropertyPath> metaPropertyPaths = result.computeIfAbsent(metaClass, k -> new HashSet<>());
                    metaPropertyPaths.add(property);
                });
        return result;
    }

    /**
     * Gets metadata of entities dependent on deleted main entity.
     * @param deletedEntityClass java class of main entity
     * @return dependent entities grouped by their {@link MetaClass}.
     * For every meta class group there are set of properties representing dependency-to-main references
     */
    public Map<MetaClass, Set<MetaPropertyPath>> getDependenciesMetaDataForDelete(Class<?> deletedEntityClass) {
        log.debug("Get dependencies metadata for class {} deletion", deletedEntityClass);
        Set<MetaPropertyPath> backRefPropertiesDelete = registry.getBackRefPropertiesForDelete(deletedEntityClass);
        if (CollectionUtils.isEmpty(backRefPropertiesDelete)) {
            return Collections.emptyMap();
        }

        Map<MetaClass, Set<MetaPropertyPath>> result = new HashMap<>();

        backRefPropertiesDelete.forEach(property -> {
            MetaClass metaClass = property.getMetaClass();
            Set<MetaPropertyPath> metaPropertyPaths = result.computeIfAbsent(metaClass, k -> new HashSet<>());
            metaPropertyPaths.add(property);
        });

        return result;
    }

    protected static class PropertyTrackingInfo {

        protected final Class<?> trackedClassUpdate; //todo change both tracked class to their entity names?
        protected final Class<?> trackedClassDelete;
        protected final String localPropertyName;
        protected final MetaPropertyPath backRefGlobalPropertyUpdate;
        protected final MetaPropertyPath backRefGlobalPropertyDelete;

        private PropertyTrackingInfo(Class<?> trackedClassUpdate,
                                     @Nullable Class<?> trackedClassDelete,
                                     String localPropertyName,
                                     @Nullable MetaPropertyPath backRefGlobalPropertyUpdate,
                                     @Nullable MetaPropertyPath backRefGlobalPropertyDelete) {
            this.trackedClassUpdate = trackedClassUpdate;
            this.trackedClassDelete = trackedClassDelete;
            this.localPropertyName = localPropertyName;
            this.backRefGlobalPropertyUpdate = backRefGlobalPropertyUpdate;
            this.backRefGlobalPropertyDelete = backRefGlobalPropertyDelete;
        }

        public static PropertyTrackingInfo of(MetaClass rootClass, MetaPropertyPath propertyPath) {
            Class<?> trackedClassUpdate = propertyPath.getMetaProperty().getDomain().getJavaClass();
            Class<?> trackedClassDelete = propertyPath.getRange().isClass() ? propertyPath.getRangeJavaClass() : null;
            String localPropertyName = propertyPath.getMetaProperty().getName();
            MetaPropertyPath backRefGlobalPropertyDelete = propertyPath.getRange().isClass() ? propertyPath : null;
            MetaPropertyPath backRefGlobalPropertyUpdate = null;

            if (propertyPath.getMetaProperties().length > 1) {
                MetaProperty[] metaProperties = propertyPath.getMetaProperties();
                MetaProperty[] newProperties = Arrays.copyOf(metaProperties, metaProperties.length - 1);
                backRefGlobalPropertyUpdate = new MetaPropertyPath(rootClass, newProperties);
            }

            return new PropertyTrackingInfo(trackedClassUpdate,
                    trackedClassDelete,
                    localPropertyName,
                    backRefGlobalPropertyUpdate,
                    backRefGlobalPropertyDelete
            );
        }

        public Class<?> getTrackedClassUpdate() {
            return trackedClassUpdate;
        }

        public String getLocalPropertyName() {
            return localPropertyName;
        }

        @Nullable
        public MetaPropertyPath getBackRefGlobalPropertyUpdate() {
            return backRefGlobalPropertyUpdate;
        }

        @Nullable
        public Class<?> getTrackedClassDelete() {
            return trackedClassDelete;
        }

        @Nullable
        public MetaPropertyPath getBackRefGlobalPropertyDelete() {
            return backRefGlobalPropertyDelete;
        }

        @Override
        public String toString() {
            return "PropertyTrackingInfo{" +
                    "trackedClassUpdate=" + trackedClassUpdate +
                    ", trackedClassDelete=" + trackedClassDelete +
                    ", localPropertyName='" + localPropertyName + '\'' +
                    ", backRefGlobalPropertyUpdate=" + backRefGlobalPropertyUpdate +
                    ", backRefGlobalPropertyDelete=" + backRefGlobalPropertyDelete +
                    '}';
        }
    }

    private static class Registry {
        private final InstanceNameProvider instanceNameProvider;

        private final Map<String, IndexConfiguration> indexDefinitionsByEntityName = new HashMap<>();
        private final Map<String, IndexConfiguration> indexDefinitionsByIndexName = new HashMap<>();
        private final Map<Class<?>, Map<String, Set<MetaPropertyPath>>> referentiallyAffectedPropertiesForUpdate = new HashMap<>();
        private final Map<Class<?>, Set<MetaPropertyPath>> referentiallyAffectedPropertiesForDelete = new HashMap<>();
        private final Set<Class<?>> registeredEntityClasses = new HashSet<>();

        public Registry(InstanceNameProvider instanceNameProvider) {
            this.instanceNameProvider = instanceNameProvider;
        }

        void registerIndexDefinition(IndexConfiguration indexConfiguration) {
            registerInMainRegistries(indexConfiguration);

            indexConfiguration.getMapping().getFields().values()
                    .stream()
                    .filter(f -> {
                        return !f.isStandalone();//todo
                    })
                    .forEach(this::processEntityFieldDescriptor);
        }

        IndexConfiguration getIndexDefinitionByEntityName(String entityName) {
            return indexDefinitionsByEntityName.get(entityName);
        }

        IndexConfiguration getIndexDefinitionByIndexName(String indexName) {
            return indexDefinitionsByIndexName.get(indexName);
        }

        Collection<IndexConfiguration> getIndexDefinitions() {
            return indexDefinitionsByEntityName.values();
        }

        Map<String, Set<MetaPropertyPath>> getBackRefPropertiesForUpdate(Class<?> entityClass) {
            return referentiallyAffectedPropertiesForUpdate.get(entityClass);
        }

        Set<MetaPropertyPath> getBackRefPropertiesForDelete(Class<?> entityClass) {
            return referentiallyAffectedPropertiesForDelete.get(entityClass);
        }

        boolean hasDefinitionForEntity(String entityName) {
            return indexDefinitionsByEntityName.containsKey(entityName);
        }

        boolean isEntityClassRegistered(Class<?> entityClass) {
            return registeredEntityClasses.contains(entityClass);
        }

        private void registerInMainRegistries(IndexConfiguration indexConfiguration) {
            String entityName = indexConfiguration.getEntityName();
            if (indexDefinitionsByEntityName.containsKey(entityName)) {
                log.warn("Multiple Index Definitions are detected for entity '{}'", entityName);
            } else {
                indexDefinitionsByEntityName.put(entityName, indexConfiguration);
                indexDefinitionsByIndexName.put(indexConfiguration.getIndexName(), indexConfiguration);
                registeredEntityClasses.addAll(indexConfiguration.getAffectedEntityClasses());
            }
        }

        private void processEntityFieldDescriptor(MappingFieldDescriptor fieldDescriptor) {
            List<MetaPropertyPath> effectiveProperties;
            if (fieldDescriptor.getMetaPropertyPath().getRange().isClass()) {
                // Extend properties with instance-name-affected properties for simple 'refEntity' field declaration case
                effectiveProperties = createExtendedPropertiesForClassField(fieldDescriptor);
            } else {
                effectiveProperties = Collections.singletonList(fieldDescriptor.getMetaPropertyPath());
            }
            log.debug("Effective properties = {}", effectiveProperties);

            List<PropertyTrackingInfo> propertyTrackingInfoList = effectiveProperties.stream()
                    .flatMap(p -> createPropertyTrackingInfoList(p.getMetaClass(), p).stream())
                    .collect(Collectors.toList());
            log.debug("Properties tracking info = {}", propertyTrackingInfoList);

            propertyTrackingInfoList.forEach(this::processPropertyTrackingInfo);
        }

        private List<MetaPropertyPath> createExtendedPropertiesForClassField(MappingFieldDescriptor fieldDescriptor) {
            Collection<MetaProperty> instanceNameRelatedProperties = instanceNameProvider.getInstanceNameRelatedProperties(
                    fieldDescriptor.getMetaPropertyPath().getRange().asClass(), true
            );
            log.debug("Instance Name related properties: {}", instanceNameRelatedProperties);
            MetaProperty[] metaProperties = fieldDescriptor.getMetaPropertyPath().getMetaProperties();

            return instanceNameRelatedProperties.stream()
                    .map(instanceNameRelatedProperty -> {
                        MetaProperty[] extendedPropertyArray = Arrays.copyOf(metaProperties, metaProperties.length + 1);
                        extendedPropertyArray[extendedPropertyArray.length - 1] = instanceNameRelatedProperty;
                        return new MetaPropertyPath(fieldDescriptor.getMetaPropertyPath().getMetaClass(), extendedPropertyArray);
                    })
                    .collect(Collectors.toList());
        }

        private void processPropertyTrackingInfo(PropertyTrackingInfo trackingInfo) {
            log.debug("Process Property Tracking Info: {}", trackingInfo);
            registerBackRefPropertyForUpdate(trackingInfo);
            registerBackRefPropertyForDelete(trackingInfo);
        }

        private void registerBackRefPropertyForUpdate(PropertyTrackingInfo trackingInfo) {
            Map<String, Set<MetaPropertyPath>> refTrackedProperties = referentiallyAffectedPropertiesForUpdate.computeIfAbsent(
                    trackingInfo.getTrackedClassUpdate(), k -> new HashMap<>()
            );
            Set<MetaPropertyPath> refPropertyPaths = refTrackedProperties.computeIfAbsent(
                    trackingInfo.getLocalPropertyName(), k -> new HashSet<>()
            );
            log.debug("Update info: Tracked Class = {}, Local Property = {}, Back Ref Global Property = {}",
                    trackingInfo.getTrackedClassUpdate(),
                    trackingInfo.getLocalPropertyName(),
                    trackingInfo.getBackRefGlobalPropertyUpdate());
            if (trackingInfo.getBackRefGlobalPropertyUpdate() != null) {
                log.debug("Add Update back-ref property");
                refPropertyPaths.add(trackingInfo.getBackRefGlobalPropertyUpdate());
            }
        }

        private void registerBackRefPropertyForDelete(PropertyTrackingInfo trackingInfo) {
            log.debug("Delete info: Tracked Class = {}, Back Ref Global Property = {}",
                    trackingInfo.getTrackedClassDelete(), trackingInfo.getBackRefGlobalPropertyDelete());
            if (trackingInfo.getTrackedClassDelete() != null && trackingInfo.getBackRefGlobalPropertyDelete() != null) {
                Set<MetaPropertyPath> refTrackedPropertiesDelete =
                        referentiallyAffectedPropertiesForDelete.computeIfAbsent(
                                trackingInfo.getTrackedClassDelete(), k -> new HashSet<>()
                        );
                log.debug("Add Delete back-ref property");
                refTrackedPropertiesDelete.add(trackingInfo.getBackRefGlobalPropertyDelete());
            }
        }

        private List<PropertyTrackingInfo> createPropertyTrackingInfoList(MetaClass rootClass, MetaPropertyPath propertyPath) {
            log.debug("Process property for MetaClass={}: {}", rootClass, propertyPath);
            List<PropertyTrackingInfo> result = new ArrayList<>();
            PropertyTrackingInfo propertyTrackingInfo = PropertyTrackingInfo.of(rootClass, propertyPath);
            result.add(propertyTrackingInfo);

            MetaPropertyPath refPropertyPath = propertyTrackingInfo.getBackRefGlobalPropertyUpdate();
            if (refPropertyPath != null) {
                result.addAll(createPropertyTrackingInfoList(rootClass, refPropertyPath));
            }

            return result;
        }
    }
}