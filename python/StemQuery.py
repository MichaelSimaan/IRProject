import argparse
from nltk.stem import SnowballStemmer

ps = SnowballStemmer("english")
parser = argparse.ArgumentParser()
parser.add_argument("-q", type=str)
args = parser.parse_args()

line = args.q
words = line.split()
stemmedQuery = []
for word in words:
    word = ps.stem(word)
    stemmedQuery.append(word)

finalQuery = ' '.join(stemmedQuery)
print(finalQuery)