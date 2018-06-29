from nltk import sent_tokenize, word_tokenize
import os

import enchant
import re
from collections import Counter
import argparse
import enchant

parser = argparse.ArgumentParser()
parser.add_argument("-q", type=str)
args = parser.parse_args()

def words(text): return re.findall(r'\w+', text.lower())

englishwords = 'python/AllEnglishWords.txt'
WORDS = Counter(words(open(englishwords).read()))

def P(word, N=sum(WORDS.values())):
    "Probability of `word`."
    return WORDS[word] / N

def correction(word):
    "Most probable spelling correction for word."
    return max(candidates(word), key=P)

def candidates(word):
    "Generate possible spelling corrections for word."
    return (known([word]) or known(edits1(word)) or known(edits2(word)) or [word])

def known(words):
    "The subset of `words` that appear in the dictionary of WORDS."
    return set(w for w in words if w in WORDS)

def edits1(word):
    "All edits that are one edit away from `word`."
    letters    = 'abcdefghijklmnopqrstuvwxyz'
    splits     = [(word[:i], word[i:])    for i in range(len(word) + 1)]
    deletes    = [L + R[1:]               for L, R in splits if R]
    transposes = [L + R[1] + R[0] + R[2:] for L, R in splits if len(R)>1]
    replaces   = [L + c + R[1:]           for L, R in splits if R for c in letters]
    inserts    = [L + c + R               for L, R in splits for c in letters]
    return set(deletes + transposes + replaces + inserts)

def edits2(word):
    "All edits that are two edits away from `word`."
    return (e2 for e1 in edits1(word) for e2 in edits1(e1))

question = args.q
d = enchant.Dict("en_US")
allWords = []
words = word_tokenize(question)
for word in words:
    if word.isalpha() and not d.check(word.lower()) and not d.check(word.title()) and not d.check(word.capitalize()) and len(word) > 2 and not word[0].isupper():
        word = correction(word)
    allWords.append(word)


fixedQuestion = ' '.join(allWords)
print(fixedQuestion)
