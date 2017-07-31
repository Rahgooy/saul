package edu.illinois.cs.cogcomp.saulexamples.mSpRL2017

import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.Helpers._
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._
import edu.illinois.cs.cogcomp.saulexamples.nlp.BaseTypes._
import edu.illinois.cs.cogcomp.saulexamples.nlp.LanguageBaseTypeSensors.documentToSentenceGenerating
import mSpRLConfigurator._

import scala.collection.JavaConversions._

/** Created by Taher on 2017-02-12.
  */

object MultiModalPopulateData {

  lazy val xmlReader = new SpRLXmlReader(if (isTrain) trainFile else testFile)
  lazy val imageReader = new ImageReaderHelper(imageDataPath, trainFile, testFile, isTrain)

  def populateRoleDataFromAnnotatedCorpus(populateNullPairs: Boolean = true): Unit = {
    if (isTrain && onTheFlyLexicon) {
      LexiconHelper.createSpatialIndicatorLexicon(xmlReader)
    }
    documents.populate(xmlReader.getDocuments, isTrain)
    sentences.populate(xmlReader.getSentences, isTrain)

    if (populateNullPairs) {
      phrases.populate(List(dummyPhrase), isTrain)
    }

    val phraseInstances = (if (isTrain) phrases.getTrainingInstances.toList else phrases.getTestingInstances.toList)
      .filter(_.getId != dummyPhrase.getId)

    xmlReader.setRoles(phraseInstances)

    if(populateImages) {
      images.populate(imageReader.getImageList, isTrain)
      segments.populate(imageReader.getSegmentList, isTrain)
      segmentRelations.populate(imageReader.getImageRelationList, isTrain)
    }
  }

  def populatePairDataFromAnnotatedCorpus(indicatorClassifier: Phrase => Boolean,
                                          populateNullPairs: Boolean = true
                                         ): Unit = {

    val phraseInstances = (if (isTrain) phrases.getTrainingInstances.toList else phrases.getTestingInstances.toList)
      .filter(_.getId != dummyPhrase.getId)

    val candidateRelations = CandidateGenerator.generatePairCandidates(phraseInstances, populateNullPairs, indicatorClassifier)
    pairs.populate(candidateRelations, isTrain)

    val relations = if (isTrain) pairs.getTrainingInstances.toList else pairs.getTestingInstances.toList
    xmlReader.setPairTypes(relations, populateNullPairs)
  }

  def partof(l1: String, l2: String): Boolean = {
    var found = false
    for(f <- l1.split(" ")) {
      for(s <- l2.split(" ")) {
        if(f==s) {
          found = true
        }
      }
    }
    found
  }
  def populateTripletGroundTruth() {
    val groundTruthTriplets = if(isTrain) new SpRLXmlReader(trainFile).getTripletsWithArguments() else new SpRLXmlReader(testFile).getTripletsWithArguments()

    val instances = if (isTrain) phrases.getTrainingInstances else phrases.getTestingInstances


    groundTruthTriplets.foreach(t => {
      var fullID = ""
      val gsen = t.getParent.getId
      val tra = instances.filter(i => {
        val sen = phrases(i) <~ sentenceToPhrase
        ((i.getText.trim == t.getArgument(0).getText.trim ||
          partof(i.getText.trim, t.getArgument(0).getText.trim))
          && gsen == sen.head.getId)
      })
      if (tra.nonEmpty) {
        t.setArgumentId(0, tra.head.getId)
        fullID = tra.head.getId
      }
      val sp = instances.filter(i => {
        val sen = phrases(i) <~ sentenceToPhrase
        ((i.getText.trim == t.getArgument(1).getText.trim ||
          partof(i.getText.trim, t.getArgument(1).getText.trim))
          && gsen == sen.head.getId)
      })
      if(sp.nonEmpty) {
        t.setArgumentId(1, sp.head.getId)
        fullID = fullID + "_" + sp.head.getId
      }
      val lm = instances.filter(i => {
        val sen = phrases(i) <~ sentenceToPhrase
        var lmText = t.getArgument(2).getText
        if(lmText!=null)
          ((i.getText.trim == lmText.trim ||
            partof(i.getText.trim, t.getArgument(2).getText.trim))
            && gsen == sen.head.getId)
        else
          false
      })
      if(lm.nonEmpty) {
        t.setArgumentId(2, lm.head.getId)
        fullID = fullID + "_" + lm.head.getId
      }
      else {
        t.setArgumentId(2, dummyPhrase.getId)
        fullID = fullID + "_" + dummyPhrase.getId
      }
      t.setId(fullID)
    })
    triplets.populate(groundTruthTriplets, isTrain)

    xmlReader.setTripletRelationTypes(groundTruthTriplets)

  }

  def populateAllTripletsFromPhrases() : Unit = {

    val allTriplets = CandidateGenerator.generateAllTripletCandidate()

    triplets.populate(allTriplets, isTrain)

    xmlReader.setTripletRelationTypes(allTriplets)
  }

  def populateTripletDataFromAnnotatedCorpus(
                                              trClassifier: (Relation) => String,
                                              spClassifier: (Phrase) => String,
                                              lmClassifier: (Relation) => String
                                            ): Unit = {

    val candidateRelations = CandidateGenerator.generateTripletCandidates(
      trClassifier,
      spClassifier,
      lmClassifier,
      isTrain
    )

    triplets.populate(candidateRelations, isTrain)

    xmlReader.setTripletRelationTypes(candidateRelations)
  }

  def populateDataFromPlainTextDocuments(documentList: List[Document],
                                         indicatorClassifier: Phrase => Boolean,
                                         populateNullPairs: Boolean = true
                                        ): Unit = {

    val isTrain = false
    documents.populate(documentList, isTrain)
    sentences.populate(documentList.flatMap(d => documentToSentenceGenerating(d)), isTrain)
    if (populateNullPairs) {
      phrases.populate(List(dummyPhrase), isTrain)
    }
    val candidateRelations = CandidateGenerator.generatePairCandidates(phrases().toList, populateNullPairs, indicatorClassifier)
    pairs.populate(candidateRelations, isTrain)
  }
}

