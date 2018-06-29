import argparse
from nltk import word_tokenize
from nltk import SnowballStemmer
import nltk

ps = SnowballStemmer("english")
parser = argparse.ArgumentParser()
parser.add_argument("-q", type=str)
args = parser.parse_args()

line = args.q.lower()
line = line.replace("\'", "")
line = word_tokenize(line)
pairs = nltk.pos_tag(line)
stemmedPairs = {}
for pair in pairs:
    word = pair[0]
    type = pair[1]
    if type.startswith("N"):
        type = "N"
    elif type.startswith("J"):
        type = "J"
    elif type.startswith("V"):
        type = "V"
    else:
        type = "O"
    word = ps.stem(word)
    stemmedPairs[word] = type

print(stemmedPairs)