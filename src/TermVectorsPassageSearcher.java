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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class TermVectorsPassageSearcher implements PassageSearcher {

    private final IndexSearcher searcher;
    private final String field;
    private final double overlapRatio;
    private final PassageScorer passageScorer;

    /**
     * Sole constructor.
     *
     * @param searcher
     *            the {@link IndexSearcher} to use for searching documents.
     * @param field
     *            the field from which to extract candidate passages.
     * @param overlapRatio
     *            the percentage of overlapping characters between passages.
     * @param passageScorer
     *            the {@link PassageScorer} to use for scoring passages.
     */
    public TermVectorsPassageSearcher(IndexSearcher searcher, String field, double overlapRatio,
            PassageScorer passageScorer) {
        this.searcher = searcher;
        this.field = field;
        this.overlapRatio = overlapRatio;
        this.passageScorer = passageScorer;
    }

    @SuppressWarnings("resource")
    @Override
    public List<Passage> search(Query query, TopDocs topDocs, int numPassages, int passageLength) throws IOException {
        // Extract query terms.
        final Set<Term> queryTerms = new HashSet<>();
        query.createWeight(searcher, false, 1.0f).extractTerms(queryTerms);

        // Generate candidate passages.
        final IndexReader reader = searcher.getIndexReader();
        final List<Passage> candidatePassages = new ArrayList<>();
        for (final ScoreDoc sd : topDocs.scoreDocs) {
            final Document doc = searcher.doc(sd.doc);
            final String docID = doc.get("id");
            final String text = doc.get(App.FILTERED_BODY_FIELD);
            // Generate candidate passages for document.
            final List<Passage> docPassages = new ArrayList<>();
            int start = 0;
            while (start < text.length()) {
                final int end = start + Math.min(passageLength, text.length() - start);
                docPassages.add(new Passage()
                        .setDocID(docID)
                        .setDocScore(sd.score)
                        .setText(text.substring(start, end))
                        .setStartOffset(start)
                        .setEndOffset(end));
                start += passageLength * (1.0 - overlapRatio);
            }
            // Search for query terms' occurrence within passages, using document term vectors.
            final Terms terms = reader.getTermVector(sd.doc, field);
            if (terms == null) {
                throw new IllegalStateException(
                		Utils.format("Document [%d] does not have term vectors indexed for field [%s]", sd.doc, field));
            }
            final TermsEnum termsEnum = terms.iterator();
            for (final Term qTerm : queryTerms) {
                if (!qTerm.field().equals(field)) {
                    continue; // query term does not belong to the TV field.
                }
                if (!termsEnum.seekExact(qTerm.bytes())) {
                    continue; // query term not found in document.
                }
                final PostingsEnum postings = termsEnum.postings(null);
                postings.nextDoc(); // only one document
                // Iterate over the positions.
                for (int i = 0; i < postings.freq(); i++) {
                    postings.nextPosition(); // Advance to the next position.
                    final int startOffset = postings.startOffset();
                    final int endOffset = postings.endOffset();
                    for (final Passage docPassage : docPassages) {
                        if (docPassage.getStartOffset() <= startOffset && endOffset <= docPassage.getEndOffset()) {
                            // Term belongs to this passage, add it.
                            docPassage.addTerm(qTerm.text(), new Passage.Interval(startOffset, endOffset));
                        } else if (docPassage.getStartOffset() > endOffset) {
                            // Optimization, since the doc passages are sorted in increasing order, if the passage's
                            // offsets are beyond the scope of this term, no point evaluating other passages.
                            break;
                        }
                    }
                }
            }
            candidatePassages.addAll(docPassages);
        }

        // Score and sort the passages.
        passageScorer.score(candidatePassages);
        Collections.sort(candidatePassages, PassageScorer.PASSAGE_COMPARATOR);

        // Return the top-scoring passages.
        return candidatePassages.stream().limit(numPassages).collect(Collectors.toList());
    }

}
