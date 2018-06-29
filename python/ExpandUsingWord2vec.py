from gensim import models
import string
from nltk.corpus import wordnet ,stopwords
from nltk.tokenize import word_tokenize, sent_tokenize
import sys
if len(sys.argv) < 2:
    quit()
model = models.Word2Vec.load(sys.argv[1])

w1 = sys.argv[2].lower()
n=1
if len(sys.argv) > 3:
    n = (int)(sys.argv[3])
stop_words = set(stopwords.words("english"))
exclude = set(string.punctuation)
exclude.add("''")
exclude.add("``")
exclude.add("..")
exclude.add("--")
exclude.add("...")
stop_words.add("'s")
stop_words.add("n't")
stop_words.add("'d")
sentence = sent_tokenize(w1)
allwords = []
for phrase in sentence:
    words = word_tokenize(phrase)
    for word in words:
        if word.lower() not in stop_words and word not in exclude:
            allwords.append(word.rstrip("."))
res = []
for word in allwords:
    res.append(word)
    try:
        tuples = model.most_similar(word)
    except Exception as e:
        continue
    for i in range(0,n):
        res.append(tuples[i][0])
print(' '.join(res))
