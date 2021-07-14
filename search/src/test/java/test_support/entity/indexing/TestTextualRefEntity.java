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

package test_support.entity.indexing;

import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.entity.annotation.OnDeleteInverse;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import test_support.entity.BaseEntity;

import javax.persistence.*;
import java.util.List;

@JmixEntity
@Table(name = "TEST_TEXTUAL_REF_ENTITY")
@Entity(name = "test_TextualRefEntity")
public class TestTextualRefEntity extends BaseEntity {
    @InstanceName
    @Column(name = "NAME")
    private String name;

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @OnDelete(DeletePolicy.UNLINK)
    @JoinColumn(name = "ONE_TO_ONE_REF_ID")
    @OneToOne(fetch = FetchType.LAZY)
    private TestTextualSubRefEntity oneToOneRef;

    @OnDeleteInverse(DeletePolicy.UNLINK)
    @OnDelete(DeletePolicy.UNLINK)
    @OneToMany(mappedBy = "manyToOneRef")
    private List<TestTextualSubRefEntity> oneToManyRef;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "oneToOneRef")
    private TestTextualRootEntity inverseOneToOneRef;

    @JoinColumn(name = "MANY_TO_ONE_REF_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private TestTextualRootEntity manyToOneRef;

    public TestTextualRootEntity getManyToOneRef() {
        return manyToOneRef;
    }

    public void setManyToOneRef(TestTextualRootEntity manyToOneRef) {
        this.manyToOneRef = manyToOneRef;
    }

    public TestTextualRootEntity getInverseOneToOneRef() {
        return inverseOneToOneRef;
    }

    public void setInverseOneToOneRef(TestTextualRootEntity inverseOneToOneRef) {
        this.inverseOneToOneRef = inverseOneToOneRef;
    }

    public List<TestTextualSubRefEntity> getOneToManyRef() {
        return oneToManyRef;
    }

    public void setOneToManyRef(List<TestTextualSubRefEntity> oneToManyRef) {
        this.oneToManyRef = oneToManyRef;
    }

    public TestTextualSubRefEntity getOneToOneRef() {
        return oneToOneRef;
    }

    public void setOneToOneRef(TestTextualSubRefEntity oneToOneRef) {
        this.oneToOneRef = oneToOneRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}