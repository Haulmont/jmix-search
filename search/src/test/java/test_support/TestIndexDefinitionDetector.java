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

package test_support;

import io.jmix.search.index.mapping.processor.IndexDefinitionDetector;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.List;
import java.util.stream.Collectors;

public class TestIndexDefinitionDetector extends IndexDefinitionDetector {

    protected final List<String> detectableClassNames;

    public TestIndexDefinitionDetector(List<Class<?>> detectableClasses) {
        this.detectableClassNames = detectableClasses.stream().map(Class::getName).collect(Collectors.toList());
    }

    @Override
    public boolean isCandidate(@NotNull MetadataReader metadataReader) {
        return super.isCandidate(metadataReader) && isShouldBeDetected(metadataReader);
    }

    protected boolean isShouldBeDetected(MetadataReader metadataReader) {
        return detectableClassNames.contains(metadataReader.getClassMetadata().getClassName());
    }
}
