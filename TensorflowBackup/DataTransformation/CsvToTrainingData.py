import csv


def toTrainingData():

    input = open('../TestData/diabetes-sequences.csv', "r")
    reader = csv.reader(input)

    output = open('../TestData/diabetes-trainingdata.txt', "w")

    patientColumn = 2

    sequenceBuffer = []

    previousPatientNumber = 1
    for row in reader:

        currentPatientNumber = row[patientColumn]

        if str(currentPatientNumber) != str(previousPatientNumber):
            previousPatientNumber = currentPatientNumber

            writeSequence(sequenceBuffer, output)

            sequenceBuffer = []


        sequenceBuffer.append(row)


    input.close()
    output.close()


def writeSequence(sequence, output):
    skipGramWindowSize = 5

    beforePadding = "START"
    betweenPadding = "BETWEEN"
    afterPadding = "END"

    for i in range(1, skipGramWindowSize):
        output.write(beforePadding + " ")
    output.write(beforePadding)

    for state in sequence:
        output.write(" " + "[" + ','.join([str(x) for x in state]) + "]")

    output.write(" " + afterPadding)
    for i in range(1, skipGramWindowSize):
        output.write(" " + afterPadding)

    #output.write(" " + betweenPadding)
    #for i in range(1, skipGramWindowSize):
    #    output.write(" " + betweenPadding)
    output.write(" ")


if __name__ == "__main__":
    toTrainingData()