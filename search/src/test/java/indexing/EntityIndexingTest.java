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

package indexing;

import com.fasterxml.jackson.databind.JsonNode;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.IdSerialization;
import io.jmix.core.Metadata;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.search.index.EntityIndexer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import test_support.*;
import test_support.entity.indexing.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {IndexingTestConfiguration.class}
)
public class EntityIndexingTest {

    @Autowired
    protected EntityIndexer entityIndexer;
    @Autowired
    protected TestBulkRequestsTracker bulkRequestsTracker;
    @Autowired
    protected DataManager dataManager;
    @Autowired
    protected Metadata metadata;
    @Autowired
    protected SystemAuthenticator authenticator;
    @Autowired
    protected IdSerialization idSerialization;

    @BeforeEach
    public void setUp() {
        bulkRequestsTracker.clear();
        authenticator.begin();
    }

    @AfterEach
    public void tearDown() {
        authenticator.end();
    }


    @Test
    @DisplayName("Indexing of entity with UUID primary key")
    public void indexUuidPk() {
        TestUuidPkEntity entity = metadata.create(TestUuidPkEntity.class);
        entity.setName("UUID PK entity");
        dataManager.save(entity);

        JsonNode jsonNode = TestJsonUtils.readJsonFromFile("indexing/test_content_uuid_pk");
        TestBulkRequestIndexActionValidationData expectedIndexAction = new TestBulkRequestIndexActionValidationData(
                "search_index_test_uuidpkentity",
                idSerialization.idToString(Id.of(entity)),
                jsonNode
        );
        TestBulkRequestValidationData expectedData = new TestBulkRequestValidationData(
                Collections.singletonList(expectedIndexAction),
                Collections.emptyList());

        entityIndexer.index(entity);
        List<BulkRequest> bulkRequests = bulkRequestsTracker.getBulkRequests();

        TestBulkRequestValidationResult result = TestBulkRequestValidator.validate(Collections.singletonList(expectedData), bulkRequests);
        Assert.assertFalse(result.toString(), result.hasFailures());
    }

    @Test
    @DisplayName("Indexing of entity with Long primary key")
    public void indexLongPk() {
        TestLongPkEntity entity = metadata.create(TestLongPkEntity.class);
        entity.setName("Long PK entity");
        dataManager.save(entity);

        JsonNode jsonNode = TestJsonUtils.readJsonFromFile("indexing/test_content_long_pk");
        TestBulkRequestIndexActionValidationData expectedIndexAction = new TestBulkRequestIndexActionValidationData(
                "search_index_test_longpkentity",
                idSerialization.idToString(Id.of(entity)),
                jsonNode
        );
        TestBulkRequestValidationData expectedData = new TestBulkRequestValidationData(
                Collections.singletonList(expectedIndexAction),
                Collections.emptyList()
        );

        entityIndexer.index(entity);
        List<BulkRequest> bulkRequests = bulkRequestsTracker.getBulkRequests();

        TestBulkRequestValidationResult result = TestBulkRequestValidator.validate(Collections.singletonList(expectedData), bulkRequests);
        Assert.assertFalse(result.toString(), result.hasFailures());
    }

    @Test
    @DisplayName("Indexing of entity with String primary key")
    public void indexStringPk() {
        TestStringPkEntity entity = metadata.create(TestStringPkEntity.class);
        entity.setName("String PK entity");
        entity.setId("string_pk_1");
        dataManager.save(entity);

        JsonNode jsonNode = TestJsonUtils.readJsonFromFile("indexing/test_content_string_pk");
        TestBulkRequestIndexActionValidationData expectedIndexAction = new TestBulkRequestIndexActionValidationData(
                "search_index_test_stringpkentity",
                idSerialization.idToString(Id.of(entity)),
                jsonNode
        );
        TestBulkRequestValidationData expectedData = new TestBulkRequestValidationData(Collections.singletonList(expectedIndexAction), Collections.emptyList());

        entityIndexer.index(entity);
        List<BulkRequest> bulkRequests = bulkRequestsTracker.getBulkRequests();

        TestBulkRequestValidationResult result = TestBulkRequestValidator.validate(Collections.singletonList(expectedData), bulkRequests);
        Assert.assertFalse(result.toString(), result.hasFailures());
    }

    @Test
    @DisplayName("Indexing of entity with Composite primary key")
    public void indexCompositePk() {
        TestCompositeKey compositeKey = metadata.create(TestCompositeKey.class);
        compositeKey.setPkName("pkName");
        compositeKey.setPkVersion(1L);
        TestCompositePkEntity entity = metadata.create(TestCompositePkEntity.class);
        entity.setName("Composite PK entity");
        entity.setId(compositeKey);
        dataManager.save(entity);

        JsonNode jsonNode = TestJsonUtils.readJsonFromFile("indexing/test_content_composite_pk");
        TestBulkRequestIndexActionValidationData expectedIndexAction = new TestBulkRequestIndexActionValidationData(
                "search_index_test_compositepkentity",
                idSerialization.idToString(Id.of(entity)),
                jsonNode
        );
        TestBulkRequestValidationData expectedData = new TestBulkRequestValidationData(Collections.singletonList(expectedIndexAction), Collections.emptyList());

        entityIndexer.index(entity);
        List<BulkRequest> bulkRequests = bulkRequestsTracker.getBulkRequests();

        TestBulkRequestValidationResult result = TestBulkRequestValidator.validate(Collections.singletonList(expectedData), bulkRequests);
        Assert.assertFalse(result.toString(), result.hasFailures());
    }

    @Test
    @DisplayName("Indexing of entity with various textual properties")
    public void indexTextualContent() {
        TestTextualSubRefEntity oneToOneSubRef = metadata.create(TestTextualSubRefEntity.class);
        oneToOneSubRef.setName("oneToOneSubRef");
        TestTextualSubRefEntity oneToManySubRef1 = metadata.create(TestTextualSubRefEntity.class);
        oneToManySubRef1.setName("oneToManySubRef1");
        TestTextualSubRefEntity oneToManySubRef2 = metadata.create(TestTextualSubRefEntity.class);
        oneToManySubRef2.setName("oneToManySubRef2");
        TestTextualSubRefEntity oneToManySubRef3 = metadata.create(TestTextualSubRefEntity.class);
        oneToManySubRef3.setName("oneToManySubRef3");

        TestTextualRefEntity oneToOneRef = metadata.create(TestTextualRefEntity.class);
        oneToOneRef.setName("oneToOneRef");
        oneToOneRef.setOneToOneRef(oneToOneSubRef);
        oneToOneRef.setOneToManyRef(Arrays.asList(oneToManySubRef1, oneToManySubRef2));
        oneToManySubRef1.setManyToOneRef(oneToOneRef);
        oneToManySubRef2.setManyToOneRef(oneToOneRef);

        TestTextualRefEntity oneToManyRef1 = metadata.create(TestTextualRefEntity.class);
        oneToManyRef1.setName("oneToManyRef1");
        oneToManyRef1.setOneToOneRef(oneToOneSubRef);
        oneToManyRef1.setOneToManyRef(Arrays.asList(oneToManySubRef1, oneToManySubRef2));
        oneToManySubRef1.setManyToOneRef(oneToManyRef1);
        oneToManySubRef2.setManyToOneRef(oneToManyRef1);

        TestTextualRefEntity oneToManyRef2 = metadata.create(TestTextualRefEntity.class);
        oneToManyRef2.setName("oneToManyRef2");
        oneToManyRef2.setOneToManyRef(Collections.singletonList(oneToManySubRef3));
        oneToManySubRef3.setManyToOneRef(oneToManyRef2);

        TestTextualRootEntity rootEntity = metadata.create(TestTextualRootEntity.class);
        rootEntity.setName("rootEntity");
        rootEntity.setOneToOneRef(oneToOneRef);
        rootEntity.setOneToManyRef(Arrays.asList(oneToManyRef1, oneToManyRef2));
        oneToManyRef1.setManyToOneRef(rootEntity);
        oneToManyRef2.setManyToOneRef(rootEntity);

        dataManager.save(
                rootEntity,
                oneToOneRef, oneToManyRef1, oneToManyRef2,
                oneToOneSubRef, oneToManySubRef1, oneToManySubRef2, oneToManySubRef3
        );

        JsonNode jsonNode = TestJsonUtils.readJsonFromFile("indexing/test_content_textual_properties");
        TestBulkRequestIndexActionValidationData expectedIndexAction = new TestBulkRequestIndexActionValidationData(
                "search_index_test_textualrootentity",
                idSerialization.idToString(Id.of(rootEntity)),
                jsonNode
        );
        TestBulkRequestValidationData expectedData = new TestBulkRequestValidationData(
                Collections.singletonList(expectedIndexAction),
                Collections.emptyList());

        entityIndexer.index(rootEntity);
        List<BulkRequest> bulkRequests = bulkRequestsTracker.getBulkRequests();

        TestBulkRequestValidationResult result = TestBulkRequestValidator.validate(Collections.singletonList(expectedData), bulkRequests);
        Assert.assertFalse(result.toString(), result.hasFailures());
    }
}
