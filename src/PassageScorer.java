/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public interface PassageScorer {

    /**
     * A {@link Comparator} which compares passages by their score, document score, offsets and lastly by their document
     * ID.
     */
    static Comparator<Passage> PASSAGE_COMPARATOR = (p1, p2) -> {
        // Higher-scoring passages come first.
        int cmp = Double.compare(p2.getScore(), p1.getScore());
        if (cmp != 0) {
            return cmp;
        }

        // Passages have the same score, prefer passages from higher-scoring docs.
        cmp = Double.compare(p2.getDocScore(), p1.getDocScore());
        if (cmp != 0) {
            return cmp;
        }

        // Passages belong to equal-scoring documents, prefer passages that appear first.
        cmp = Integer.compare(p1.getStartOffset(), p2.getStartOffset());
        if (cmp != 0) {
            return cmp;
        }

        // Last tie-break on document ID (so that sort is always consistent).
        return p1.getDocID().compareTo(p2.getDocID());
    };

    /**
     * A {@link PassageScorer} which sets a passage's score to the score of the document from which it was originated.
     */
    static PassageScorer BY_DOC_SCORE = (passages) -> passages.forEach(p -> p.setScore(p.getDocScore()));

    /**
     * A {@link PassageScorer} which scores a passage by the total number of query terms it contains multiplied by the
     * score of the document from which it was originated.
     */
    static PassageScorer DOC_SCORE_AND_QUERY_TF = (passages) -> passages.forEach(p -> p.setScore(p.getDocScore() *
            p.getQueryTerms().values().stream()
                    .mapToInt(List::size)
                    .sum()));

    /** Scores a list of passages. */
    void score(List<Passage> passages) throws IOException;

}
