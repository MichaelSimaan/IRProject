import argparse
import enchant

parser = argparse.ArgumentParser()
parser.add_argument("-q", type=str)
args = parser.parse_args()
d = enchant.Dict("en_US")

line = args.q
words = line.split()
finalQueryWords = []
found = False
for word in words:
    if len(word) < 7 or "-" in word:
        finalQueryWords.append(word)
        continue
    word = word.strip()
    for i, char in enumerate(word):
        if i > 2 and i < len(word) - 2:
            firstWord = word[:i]
            secondWord = word[i:]
            if (d.check(firstWord) or d.check(firstWord.title())) and (d.check(secondWord) or d.check(secondWord.title())):
                found = True
                finalQueryWords.append(firstWord)
                finalQueryWords.append(secondWord)
    finalQueryWords.append(word)

if not found:
    for word in words:
        if "=" in word:
            sides = word.split("=")
            for side in sides:
                finalQueryWords.append(side)

finalQuery = ' '.join(finalQueryWords)
print(finalQuery)