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

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import test_support.entity.TestEnum;
import test_support.entity.TestReferenceEntity;
import test_support.entity.TestRootEntity;
import test_support.entity.TestSubReferenceEntity;

import java.util.Arrays;
import java.util.Date;

public class TestEntityCreator {

    protected final Metadata metadata;
    protected final DataManager dataManager;

    public TestEntityCreator(Metadata metadata, DataManager dataManager) {
        this.metadata = metadata;
        this.dataManager = dataManager;
    }

    public TestRootEntityCreator createTestRootEntity() {
        return new TestRootEntityCreator();
    }

    public TestReferenceEntityCreator createTestReferenceEntity() {
        return new TestReferenceEntityCreator();
    }

    public TestSubReferenceEntityCreator createTestSubReferenceEntity() {
        return new TestSubReferenceEntityCreator();
    }

    public class TestRootEntityCreator {

        private final TestRootEntity instance;

        private TestRootEntityCreator() {
            instance = metadata.create(TestRootEntity.class);
            instance.setName("Test Root Entity");
        }

        public TestRootEntityCreator setName(String name) {
            this.instance.setName(name);
            return this;
        }

        public TestRootEntityCreator setTextValue(String textValue) {
            this.instance.setTextValue(textValue);
            return this;
        }

        public TestRootEntityCreator setEnumValue(TestEnum enumValue) {
            this.instance.setEnumValue(enumValue);
            return this;
        }

        public TestRootEntityCreator setIntValue(Integer intValue) {
            this.instance.setIntValue(intValue);
            return this;
        }

        public TestRootEntityCreator setDateValue(Date dateValue) {
            this.instance.setDateValue(dateValue);
            return this;
        }

        public TestRootEntityCreator setOneToOneAssociation(TestReferenceEntity reference) {
            this.instance.setOneToOneAssociation(reference);
            return this;
        }

        public TestRootEntityCreator setOneToManyAssociation(TestReferenceEntity... references) {
            this.instance.setOneToManyAssociation(Arrays.asList(references));
            return this;
        }

        public TestRootEntity save() {
            return dataManager.save(instance);
        }
    }

    public class TestReferenceEntityCreator {
        private final TestReferenceEntity instance;

        private TestReferenceEntityCreator() {
            instance = metadata.create(TestReferenceEntity.class);
            instance.setName("Test Reference Entity");
        }

        public TestReferenceEntityCreator setName(String name) {
            this.instance.setName(name);
            return this;
        }

        public TestReferenceEntityCreator setTextValue(String textValue) {
            this.instance.setTextValue(textValue);
            return this;
        }

        public TestReferenceEntityCreator setEnumValue(TestEnum enumValue) {
            this.instance.setEnumValue(enumValue);
            return this;
        }

        public TestReferenceEntityCreator setIntValue(Integer intValue) {
            this.instance.setIntValue(intValue);
            return this;
        }

        public TestReferenceEntityCreator setDateValue(Date dateValue) {
            this.instance.setDateValue(dateValue);
            return this;
        }

        public TestReferenceEntityCreator setOneToOneAssociation(TestSubReferenceEntity reference) {
            this.instance.setOneToOneAssociation(reference);
            return this;
        }

        public TestReferenceEntityCreator setOneToManyAssociation(TestSubReferenceEntity... references) {
            this.instance.setOneToManyAssociation(Arrays.asList(references));
            return this;
        }

        public TestReferenceEntity save() {
            return dataManager.save(instance);
        }
    }

    public class TestSubReferenceEntityCreator {
        private final TestSubReferenceEntity instance;

        private TestSubReferenceEntityCreator() {
            instance = metadata.create(TestSubReferenceEntity.class);
            instance.setName("Test Sub-Reference Entity");
        }

        public TestSubReferenceEntityCreator setName(String name) {
            this.instance.setName(name);
            return this;
        }

        public TestSubReferenceEntityCreator setTextValue(String textValue) {
            this.instance.setTextValue(textValue);
            return this;
        }

        public TestSubReferenceEntityCreator setEnumValue(TestEnum enumValue) {
            this.instance.setEnumValue(enumValue);
            return this;
        }

        public TestSubReferenceEntityCreator setIntValue(Integer intValue) {
            this.instance.setIntValue(intValue);
            return this;
        }

        public TestSubReferenceEntityCreator setDateValue(Date dateValue) {
            this.instance.setDateValue(dateValue);
            return this;
        }

        public TestSubReferenceEntity save() {
            return dataManager.save(instance);
        }
    }
}
