import json
from nltk.corpus import wordnet
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
with open('nfL6.json',encoding="utf-8") as f:
    data = json.load(f)

f = open("data","w",encoding="utf-8")
for doc in data:
    query = doc['question'].lower()
    for char in ['.', '?', '\'', '\"', '\\', '[', ']', '!', ';', ',', '(', ')','\u016b','\u0103','\u014d','\u012d','\u0259']:
        query = query.replace(char, "")
    words = word_tokenize(query)
    stop_words = set(stopwords.words("english"))
    filtered_sentence = []
    for word in words:
        if word not in stop_words:
            filtered_sentence.append(word)
    query = ' '.join(filtered_sentence)
    f.write(query + "\n")
    for answer in doc['nbestanswers']:
        query = answer.lower()
        for char in ['.', '?', '\'', '\"', '\\', '[', ']', '!', ';', ',', '(', ')','\u016b','\u0103','\u014d','\u012d','\u0259']:
            query = query.replace(char, "")
        words = word_tokenize(query)
        stop_words = set(stopwords.words("english"))
        filtered_sentence = []
        for word in words:
            if word not in stop_words:
               filtered_sentence.append(word)
        query = ' '.join(filtered_sentence)
        f.write(query + "\n")
f.close()