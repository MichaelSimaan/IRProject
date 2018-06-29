## Introduction
Information retrieval using the “Yahoo Non-Factoid Question Dataset” taken from the CIIR website as our data. The goal of the project is to try and retrieve the answers of a given query/question using Lucene and other models that we learnt in the IR course lectured by Dr. Haggai Roitman.

## Requirments
- Java 8
- Python
    - nltk library
    - gensim library

## How to use
1. Edit Questions.txt file with your questions.  
    1.1 file formate: Enter each ID and question in a separate line. <ID*> \<Question>
2. Run app.java
3. After the application finishes running, an output.txt file will be generated at ./src/output.txt
4. Done!

\* The ID is only used for printing the results, you can choose the Question ID as you like (1 ,2 ,3 ...).

For more information please refer to the [ProjectACM PDF](https://github.com/MichaelSimaan/IRProject/raw/master/ProjectACM.pdf)
