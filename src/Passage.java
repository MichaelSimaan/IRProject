import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class Passage {

    private String docID;
    private double docScore;
    private double score;
    private int startOffset;
    private int endOffset;
    private String text;
    private final Map<String, List<Interval>> queryTerms = new HashMap<>();

    public String getDocID() {
        return docID;
    }

    public Passage setDocID(String docID) {
        this.docID = docID;
        return this;
    }

    public double getDocScore() {
        return docScore;
    }

    public Passage setDocScore(double docScore) {
        this.docScore = docScore;
        return this;
    }

    public double getScore() {
        return score;
    }

    public Passage setScore(double score) {
        this.score = score;
        return this;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public Passage setStartOffset(int startOffset) {
        this.startOffset = startOffset;
        return this;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public Passage setEndOffset(int endOffset) {
        this.endOffset = endOffset;
        return this;
    }

    public String getText() {
        return text;
    }

    public Passage setText(String text) {
        this.text = text;
        return this;
    }

    public Map<String, List<Interval>> getQueryTerms() {
        return queryTerms;
    }

    public Passage addTerm(String term, Interval offsets) {
        List<Interval> termOffsets = queryTerms.get(term);
        if (termOffsets == null) {
            termOffsets = new ArrayList<>();
            queryTerms.put(term, termOffsets);
        }
        termOffsets.add(offsets);
        return this;
    }

    public static class Interval {
        public final int start;
        public final int end;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return Utils.format("(%d,%d)", start, end);
        }
    }

}
