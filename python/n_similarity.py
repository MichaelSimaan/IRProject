from gensim import corpora, models, similarities
import warnings, argparse, nltk, math
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
warnings.filterwarnings(action='ignore', category=UserWarning, module='gensim')
parser = argparse.ArgumentParser()
parser.add_argument("-q",type=str)
parser.add_argument("-p",type=str)
parser.add_argument("-model",type=str)
args = parser.parse_args()
model = models.Word2Vec.load(args.model)
#sentence = ' '.join(sys.argv[1:])
query = args.q
answer = args.p

set = []
set2 = []
query = query.lower()
for char in ['.', '?', '\'', '\"', '\\', '[', ']', '!', ';', ':', ',', '(', ')', '\u016b', '\u0103', '\u014d', '\u012d', '\u0259']:
    query = query.replace(char, "")
words = word_tokenize(query)
stop_words = stopwords.words("english")
filtered_sentence = []
for word in words:
    if word not in stop_words:
        filtered_sentence.append(word)
query = ' '.join(filtered_sentence)
sentenceTokenized = nltk.word_tokenize(query)
for s in sentenceTokenized:
    try:
        word_vector = model.wv[str(s)]
    except Exception as e:
        continue
    set.append(s)

answer = answer.lower()
for char in ['.', '?', '\'', '\"', '\\', '[', ']', '!', ';', ':', ',', '(', ')', '\u016b', '\u0103', '\u014d', '\u012d', '\u0259']:
    answer = answer.replace(char, "")
words = word_tokenize(answer)
stop_words = stopwords.words("english")
filtered_sentence = []
for word in words:
    if word not in stop_words:
        filtered_sentence.append(word)
answer = ' '.join(filtered_sentence)
sentenceTokenized = nltk.word_tokenize(answer)
for s in sentenceTokenized:
    try:
        word_vector = model.wv[str(s)]
    except Exception as e:
        continue
    set2.append(s)
res = math.fabs(model.wv.n_similarity(set,set2))
print(res)