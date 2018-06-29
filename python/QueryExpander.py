import json
from nltk.stem import WordNetLemmatizer, SnowballStemmer
import argparse

def similar(key, innerKey):
    shortWord = ""
    longWord = ""
    count = 0
    if len(key) < len(innerKey):
        shortWord = key
        longWord = innerKey
    else:
        longWord = key
        shortWord = innerKey
    if shortWord in longWord:
        return True
    for i, char in enumerate(shortWord):
        if shortWord[i] == longWord[i]:
            count = count + 1
    minLength = len(shortWord)
    if count / minLength > 0.8:
        return True
    return False

lemmatizer = WordNetLemmatizer()
ps = SnowballStemmer("english")
parser = argparse.ArgumentParser()
parser.add_argument("-m", type=str)
args = parser.parse_args()
wordsMap = args.m
wordsObj = json.loads(wordsMap)
filteredWords = {}

for key, value in wordsObj.items():
    if value > 1:
        filteredWords[key] = value

sortedWords = [(k, filteredWords[k]) for k in sorted(filteredWords, key=filteredWords.get, reverse=True)]
for i, word in enumerate(sortedWords):
    key = word[0]
    value = word[1]
    for j, innerWord in enumerate(sortedWords):
        innerKey = innerWord[0]
        innerValue = innerWord[1]
        if innerKey != key and value > 0 and (lemmatizer.lemmatize(key) == lemmatizer.lemmatize(innerKey)
                                              or ps.stem(key) == ps.stem(innerKey) or similar(key, innerKey)):
            value = value + innerValue
            sortedWords[i] = (key, value)
            sortedWords[j] = (innerKey, 0)

wordsDict = {}
for word in sortedWords:
    key = word[0]
    value = word[1]
    if word[1] > 1:
        wordsDict[key] = value

wordsVec = [(k, wordsDict[k]) for k in sorted(wordsDict, key=wordsDict.get, reverse=True)]

finalWordsDict = {}
for word in wordsVec:
    finalWordsDict[ps.stem(word[0])] = word[1]

print(finalWordsDict)
