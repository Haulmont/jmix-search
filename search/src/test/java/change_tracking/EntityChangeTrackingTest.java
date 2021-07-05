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

package change_tracking;

import io.jmix.core.DataManager;
import io.jmix.search.index.queue.impl.IndexingOperation;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import test_support.EntityChangeTrackingTestConfiguration;
import test_support.TestEntityCreator;
import test_support.TestIndexingQueueItemsTracker;
import test_support.entity.TestReferenceEntity;
import test_support.entity.TestRootEntity;
import test_support.entity.TestSubReferenceEntity;

import java.util.Arrays;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {EntityChangeTrackingTestConfiguration.class}
)
public class EntityChangeTrackingTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    TestIndexingQueueItemsTracker indexingQueueItemsTracker;
    @Autowired
    TestEntityCreator entityCreator;

    @BeforeEach
    public void setUp() {
        indexingQueueItemsTracker.clear();
    }

    @Test
    @DisplayName("Creation of indexed entity leads to queue item enqueueing")
    public void createIndexedEntity() {
        TestRootEntity entity = entityCreator.createTestRootEntity().save();
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of indexed entity leads to queue item enqueueing")
    public void updateLocalPropertyOfIndexedEntity() {
        TestRootEntity entity = entityCreator.createTestRootEntity().save();
        entity.setTextValue("Some text value");
        dataManager.save(entity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.INDEX, 2);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Deletion of indexed entity leads to queue item enqueueing")
    public void deleteIndexedEntity() {
        TestRootEntity entity = entityCreator.createTestRootEntity().save();
        dataManager.remove(entity);
        boolean enqueuedIndex = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.INDEX, 1);
        boolean enqueuedDelete = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.DELETE, 1);
        Assert.assertTrue(enqueuedIndex && enqueuedDelete);
    }

    @Test
    @DisplayName("Creation of not-indexed doesn't lead to queue item enqueueing")
    public void createNotIndexedEntity() {
        TestReferenceEntity entity = entityCreator.createTestReferenceEntity().save();
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.INDEX, 0);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of not-indexed entity doesn't lead to queue item enqueueing")
    public void updateLocalPropertyOfNotIndexedEntity() {
        TestReferenceEntity entity = entityCreator.createTestReferenceEntity().save();
        entity.setTextValue("Some text value");
        dataManager.save(entity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(entity, IndexingOperation.INDEX, 0);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Adding of reference leads to queue item enqueueing")
    public void addOneToOneReference() {
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().save();
        indexingQueueItemsTracker.clear();

        rootEntity.setOneToOneAssociation(reference);
        dataManager.save(rootEntity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Changing of reference leads to queue item enqueueing")
    public void changeOneToOneReference() {
        TestReferenceEntity firstReference = entityCreator.createTestReferenceEntity().save();
        TestReferenceEntity secondReference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(firstReference).save();
        indexingQueueItemsTracker.clear();

        rootEntity.setOneToOneAssociation(secondReference);
        rootEntity = dataManager.save(rootEntity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Clearing of reference leads to queue item enqueueing")
    public void clearOneToOneReference() {
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        rootEntity.setOneToOneAssociation(null);
        rootEntity = dataManager.save(rootEntity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of indexed local property of reference leads to queue item enqueueing")
    public void updateIndexedLocalPropertyOfOneToOneReference() {
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        reference.setTextValue("Some text value");
        dataManager.save(reference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of not-indexed local property of reference doesn't lead to queue item enqueueing")
    public void updateNotIndexedLocalPropertyOfOneToOneReference() {
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        reference.setName("New Name");
        dataManager.save(reference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 0);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Adding of sub-reference leads to queue item enqueueing")
    public void addOneToOneSubReference() {
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        TestSubReferenceEntity subReference = entityCreator.createTestSubReferenceEntity().save();
        indexingQueueItemsTracker.clear();

        reference.setOneToOneAssociation(subReference);
        dataManager.save(reference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Changing of sub-reference leads to queue item enqueueing")
    public void changeOneToOneSubReference() {
        TestSubReferenceEntity firstSubReference = entityCreator.createTestSubReferenceEntity().save();
        TestSubReferenceEntity secondSubReference = entityCreator.createTestSubReferenceEntity().save();
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().setOneToOneAssociation(firstSubReference).save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        reference.setOneToOneAssociation(secondSubReference);
        dataManager.save(reference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Clearing of sub-reference leads to queue item enqueueing")
    public void clearOneToOneSubReference() {
        TestSubReferenceEntity subReference = entityCreator.createTestSubReferenceEntity().save();
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().setOneToOneAssociation(subReference).save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        reference.setOneToOneAssociation(null);
        dataManager.save(reference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of indexed local property of sub-reference leads to queue item enqueueing")
    public void updateIndexedLocalPropertyOfOneToOneSubReference() {
        TestSubReferenceEntity subReference = entityCreator.createTestSubReferenceEntity().save();
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().setOneToOneAssociation(subReference).save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        subReference.setTextValue("Some text value");
        dataManager.save(subReference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Update of not-indexed local property of sub-reference doesn't lead to queue item enqueueing")
    public void updateNotIndexedLocalPropertyOfOneToOneSubReference() {
        TestSubReferenceEntity subReference = entityCreator.createTestSubReferenceEntity().save();
        TestReferenceEntity reference = entityCreator.createTestReferenceEntity().setOneToOneAssociation(subReference).save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToOneAssociation(reference).save();
        indexingQueueItemsTracker.clear();

        subReference.setName("New Name");
        dataManager.save(subReference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 0);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Adding of references collection leads to queue item enqueueing")
    public void addOneToManyReferences() {
        TestReferenceEntity firstReference = entityCreator.createTestReferenceEntity().save();
        TestReferenceEntity secondReference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().save();
        indexingQueueItemsTracker.clear();

        rootEntity.setOneToManyAssociation(Arrays.asList(firstReference, secondReference));
        dataManager.save(rootEntity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    @Test
    @DisplayName("Changing of references collection leads to queue item enqueueing")
    public void changeOneToManyReferences() {
        TestReferenceEntity firstReference = entityCreator.createTestReferenceEntity().save();
        TestReferenceEntity secondReference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToManyAssociation(firstReference).save();
        indexingQueueItemsTracker.clear();

        rootEntity.setOneToManyAssociation(Arrays.asList(firstReference, secondReference));
        dataManager.save(rootEntity);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }

    /*@Test
    @DisplayName("Update of indexed local property of one-to-many reference leads to queue item enqueueing")
    public void updateIndexedLocalPropertyOfOneToManyReference() {
        TestReferenceEntity firstReference = entityCreator.createTestReferenceEntity().save();
        TestReferenceEntity secondReference = entityCreator.createTestReferenceEntity().save();
        TestRootEntity rootEntity = entityCreator.createTestRootEntity().setOneToManyAssociation(firstReference*//*, secondReference*//*).save();
        indexingQueueItemsTracker.clear();

        firstReference.setTextValue("Some text value");
        dataManager.save(firstReference);
        boolean enqueued = indexingQueueItemsTracker.containsQueueItemsForEntityAndOperation(rootEntity, IndexingOperation.INDEX, 1);
        Assert.assertTrue(enqueued);
    }*/
}
