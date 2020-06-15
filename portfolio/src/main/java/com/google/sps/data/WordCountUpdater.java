// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.data;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

/** Class that handles the updates of word counts upon addition or deletion of comment. */
public class WordCountUpdater {

  public static enum UpdateOp {
    ADD_WORDS,
    REMOVE_WORDS
  }

  private static final int TRANSACTION_RETRIES = 4;
  private DatastoreService datastore;

  public WordCountUpdater() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  public void updateWordCount(String comment, UpdateOp op) {
    String[] words = comment.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");

    for (String word : words) {
      Entity wordEntity;
      Key wordKey = KeyFactory.createKey("word", word + "_word");
      Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
      try {
        wordEntity = datastore.get(txn, wordKey);
      } catch (com.google.appengine.api.datastore.EntityNotFoundException e) {
        // In case word is not yet in datastore...
        wordEntity = new Entity(wordKey);
        wordEntity.setProperty("count", 0L);
      }

      wordEntity.setProperty("count", getNewCount((Long) wordEntity.getProperty("count"), op));
      datastore.put(txn, wordEntity);

      int retry = 0;
      while (true) {
        try {
          txn.commit();
          break;
        } catch (Exception e) {
          System.out.println("Transaction to update count did not commit for word: " + word);
          if (retry++ == TRANSACTION_RETRIES) {
            System.out.println("Giving up trying to commit count for word: " + word);
            return;
          }
        }
      }
    }
  }

  public static long getNewCount(long count, UpdateOp op) {
    switch (op) {
      case ADD_WORDS:
        return count + 1;
      case REMOVE_WORDS:
        return count - 1;
    }
    // This should never happen.
    return -1;
  }
}
