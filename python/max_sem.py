from gensim import corpora, models, similarities
import warnings, argparse, nltk, math
from nltk.corpus import wordnet ,stopwords
from nltk.tokenize import word_tokenize, sent_tokenize
import string
warnings.filterwarnings(action='ignore', category=UserWarning, module='gensim')



parser = argparse.ArgumentParser()
parser.add_argument("-sl",type=str)
parser.add_argument("-ss",type=str)
parser.add_argument("-model",type=str)
args = parser.parse_args()
model = models.Word2Vec.load(args.model)
sl = word_tokenize(args.sl)
ss = word_tokenize(args.ss)
max=0
data = []
for slword in sl:
    max = 0
    for word in ss:
        var = 0
        try:
            var = math.fabs(model.wv.similarity(slword,word))
        except Exception as e:
            continue
        if(var > max):
            max = var
    data.append(max)
for r in data:
    print(str(r))