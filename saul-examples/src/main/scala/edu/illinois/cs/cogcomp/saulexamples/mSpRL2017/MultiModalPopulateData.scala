package edu.illinois.cs.cogcomp.saulexamples.mSpRL2017

import java.io.{File, IOException, PrintWriter}

import edu.illinois.cs.cogcomp.saulexamples.data.CLEFImageReader
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._
import edu.illinois.cs.cogcomp.saulexamples.nlp.BaseTypes._
import edu.illinois.cs.cogcomp.saulexamples.nlp.LanguageBaseTypeSensors.getCandidateRelations
import edu.illinois.cs.cogcomp.saulexamples.nlp.SpatialRoleLabeling.Dictionaries
import edu.illinois.cs.cogcomp.saulexamples.nlp.Xml.NlpXmlReader
import edu.illinois.cs.cogcomp.saulexamples.nlp.XmlMatchings
import edu.illinois.cs.cogcomp.saulexamples.vision.{Image, Segment, SegmentRelation}
import scala.collection.JavaConversions._

import scala.io.Source

/**
  * Created by Taher on 2017-02-12.
  */
object DataProportion extends Enumeration {
  type DataProportion = Value
  val Train, Test, Both = Value
}

object MultiModalPopulateData {

  import DataProportion._

  val trTag = "TRAJECTOR"
  val lmTag = "LANDMARK"
  val spTag = "SPATIALINDICATOR"
  val relationTag = "RELATION"
  private lazy val CLEFDataSet = new CLEFImageReader("data/mSprl/saiapr_tc-12", false)
  private lazy val trainReader = createXmlReader(true)
  private lazy val testReader = createXmlReader(false)

  def populateData(isTrain: Boolean, proportion: DataProportion, populateImageData: Boolean = true) = {

    documents.populate(getDocumentList(proportion), isTrain)
    sentences.populate(getSentenceList(proportion), isTrain)
    val tokenInstances = if (isTrain) tokens.getTrainingInstances.toList else tokens.getTestingInstances.toList

    if (populateImageData) {
      images.populate(getImageList(proportion), isTrain)
      segments.populate(getSegmentList(proportion), isTrain)
      segmentRelations.populate(getImageRelationList(proportion), isTrain)
    }
    setTokenRoles(tokenInstances, proportion)

    val trCandidates = getTrajectorCandidates(tokenInstances, isTrain)
    trCandidates.foreach(_.addPropertyValue("TR-Candidate", "true"))

    val lmCandidates = getLandmarkCandidates(tokenInstances, isTrain)
    lmCandidates.foreach(_.addPropertyValue("LM-Candidate", "true"))

    val firstArgCandidates = null :: trCandidates.toSet.union(lmCandidates.toSet).toList

    val spCandidates = getIndicatorCandidates(tokenInstances, isTrain)
    spCandidates.foreach(_.addPropertyValue("SP-Candidate", "true"))

    val candidateRelations = getCandidateRelations(firstArgCandidates, spCandidates)

    setRelationTypes(candidateRelations, proportion)

    pairs.populate(candidateRelations, isTrain)
  }

  private def setRelationTypes(candidateRelations: List[Relation], proportion: DataProportion): Unit = {

    val goldTrajectorRelations = getGoldTrajectorPairs(proportion)
    val goldLandmarkRelations = getGoldLandmarkPairs(proportion)

    candidateRelations.foreach(_.setProperty("RelationType", "None"))

    setLmSpRelationTypes(goldLandmarkRelations, candidateRelations)
    setTrSpRelationTypes(goldTrajectorRelations, candidateRelations)

    reportRelationStats(candidateRelations, goldTrajectorRelations, goldLandmarkRelations)
  }

  private def setTrSpRelationTypes(goldTrajectorRelations: List[Relation], candidateRelations: List[Relation]): Unit = {

    goldTrajectorRelations.foreach(r => {
      val c = candidateRelations
        .find(x =>
          ((r.getArgumentId(0) == null && x.getArgumentId(0) == null) ||
            (x.getArgumentId(0) != null && x.getArgument(0).getPropertyValues(s"${trTag}_id").contains(r.getArgumentId(0)))
            ) &&
            x.getArgument(1).getPropertyValues(s"${spTag}_id").contains(r.getArgumentId(1)))

      if (c.nonEmpty) {
        if (c.get.getProperty("RelationType") == "TR-SP") {
          println(s"warning: candidate already marked as TR-SP via ${c.get.getId}. duplicate relation: ${r.getId}")
        } else {
          if (c.get.getProperty("RelationType") == "TR-SP") {
            println(s"warning: overriding LM-SP relation ${c.get.getId} by TR-SP relation: ${r.getId}")
          }
          c.get.setProperty("RelationType", "TR-SP")
          c.get.setId(r.getId)
        }
      } else {
        println(s"cannot find TR-SP candidate relation for ${r.getId}")
      }
    })
  }

  private def setLmSpRelationTypes(goldLandmarkRelations: List[Relation], candidateRelations: List[Relation]): Unit = {

    goldLandmarkRelations.foreach(r => {
      val c = candidateRelations
        .find(x =>
          ((r.getArgumentId(0) == null && x.getArgumentId(0) == null) ||
            (x.getArgumentId(0) != null && x.getArgument(0).getPropertyValues(s"${lmTag}_id").contains(r.getArgumentId(0)))
            ) &&
            x.getArgument(1).getPropertyValues(s"${spTag}_id").contains(r.getArgumentId(1)))

      if (c.nonEmpty) {
        if (c.get.getProperty("RelationType") == "LM-SP") {
          println(s"warning: candidate already marked as LM-SP via ${c.get.getId}. duplicate relation: ${r.getId}")
        } else {
          c.get.setProperty("RelationType", "LM-SP")
          c.get.setId(r.getId)
        }
      } else {
        println(s"cannot find LM-SP candidate relation for ${r.getId}")
      }
    })
  }

  def getIndicatorCandidates(tokenInstances: List[Token], isTrain: Boolean): List[Token] = {

    val spLex = List("behind", "standing", "underneath", "in", "below", "outside", "before", "lying", "walking",
      "above", "to", "around", "at", "through", "distant", "over", "on", "leaning", "with", "from", "next", "leading",
      "under", "between", "sitting", "along", "inside", "of", "right", "attached", "left", "lined", "close",
      "supported", "side", "goes", "surrounded")
    //getSpatialIndicatorLexicon(tokenInstances, 2, isTrain)
    val spPosTagLex = List("IN", "VBG", "JJ", "TO")
    // getRolePosTagLexicon(tokenInstances, spTag, 10, isTrain)
    val spCandidates = tokenInstances
      .filter(x => spLex.contains(x.getText.toLowerCase) ||
        spPosTagLex.contains(pos(x)) ||
        Dictionaries.isPreposition(x.getText))
    reportRoleStats(tokenInstances, spCandidates, spTag)
    spCandidates
  }

  def getLandmarkCandidates(tokenInstances: List[Token], isTrain: Boolean): List[Token] = {

    val lmPosTagLex = List("PRP", "NN", "PRP$", "JJ", "NNS", "CD")
    //getRolePosTagLexicon(tokenInstances, lmTag, 5, isTrain)
    val lmCandidates = tokenInstances.filter(x => lmPosTagLex.contains(pos(x)))
    reportRoleStats(tokenInstances, lmCandidates, lmTag)
    lmCandidates
  }

  def getTrajectorCandidates(tokenInstances: List[Token], isTrain: Boolean): List[Token] = {

    val trPosTagLex = List("NN", "JJR", "PRP$", "VBG", "JJ", "NNP", "NNS", "CD", "VBN", "VBD")
    //getRolePosTagLexicon(tokenInstances, trTag, 5, isTrain)
    val trCandidates = tokenInstances.filter(x => trPosTagLex.contains(pos(x)))
    reportRoleStats(tokenInstances, trCandidates, trTag)
    trCandidates
  }

  private def getGoldLandmarkPairs(proportion: DataProportion): List[Relation] = {

    // create pairs which first argument is landmark and second is indicator, and remove duplicates
    val nullLandmarkIds = getTags(lmTag, proportion).filter(_.getStart == -1).map(_.getId)
    val relations = getRelations("landmark_id", "spatial_indicator_id", proportion)
      .groupBy(x => x.getArgumentId(0) + "_" + x.getArgumentId(1))
      .map { case (_, list) => list.head }
      .toList
    relations.foreach(r => if (nullLandmarkIds.contains(r.getArgumentId(0))) r.setArgumentId(0, null))
    relations
  }

  private def getGoldTrajectorPairs(proportion: DataProportion): List[Relation] = {

    // create pairs which first argument is trajector and second is indicator, and remove duplicates
    val nullTrajectorIds = getTags(trTag, proportion).filter(_.getStart == -1).map(_.getId)
    val relations = getRelations("trajector_id", "spatial_indicator_id", proportion)
      .groupBy(x => x.getArgumentId(0) + "_" + x.getArgumentId(1))
      .map { case (_, list) => list.head }
      .toList
    relations.foreach(r => if (nullTrajectorIds.contains(r.getArgumentId(0))) r.setArgumentId(0, null))
    relations
  }

  private def getRelations(firstArgId: String, secondArgId: String, proportion: DataProportion): List[Relation] = {
    proportion match {
      case Train => getXmlReader(true).getRelations(relationTag, firstArgId, secondArgId).toList
      case Test => getXmlReader(false).getRelations(relationTag, firstArgId, secondArgId).toList
      case Both => getXmlReader(true).getRelations(relationTag, firstArgId, secondArgId).toList ++
        getXmlReader(false).getRelations(relationTag, firstArgId, secondArgId)
    }
  }

  private def getTags(tag: String, proportion: DataProportion): List[NlpBaseElement] = {

    proportion match {
      case Train => getXmlReader(true).getTagAsNlpBaseElement(tag).toList
      case Test => getXmlReader(false).getTagAsNlpBaseElement(tag).toList
      case Both => getXmlReader(true).getTagAsNlpBaseElement(tag).toList ++
        getXmlReader(false).getTagAsNlpBaseElement(tag)
    }
  }

  def setTokenRoles(tokenInstances: List[Token], proportion: DataProportion): Unit = {

    if (proportion != Test) {
      getXmlReader(true).addPropertiesFromTag(trTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
      getXmlReader(true).addPropertiesFromTag(lmTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
      getXmlReader(true).addPropertiesFromTag(spTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
    }

    if (proportion != Train) {
      getXmlReader(false).addPropertiesFromTag(trTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
      getXmlReader(false).addPropertiesFromTag(lmTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
      getXmlReader(false).addPropertiesFromTag(spTag, tokenInstances, XmlMatchings.xmlHeadwordMatching)
    }
  }

  def getSentenceList(proportion: DataProportion): List[Sentence] = {

    proportion match {
      case Train => getXmlReader(true).getSentences().toList
      case Test => getXmlReader(false).getSentences().toList
      case Both => getXmlReader(true).getSentences().toList ++ getXmlReader(false).getSentences()
    }
  }

  def getDocumentList(proportion: DataProportion): List[Document] = {

    proportion match {
      case Train => getXmlReader(true).getDocuments().toList
      case Test => getXmlReader(false).getDocuments().toList
      case Both => getXmlReader(true).getDocuments().toList ++ getXmlReader(false).getDocuments()
    }
  }

  def getImageRelationList(proportion: DataProportion): List[SegmentRelation] = {

    proportion match {
      case Train => CLEFDataSet.trainingRelations.toList
      case Test => CLEFDataSet.testRelations.toList
      case Both => CLEFDataSet.trainingRelations.toList ++ CLEFDataSet.testRelations
    }
  }

  def getSegmentList(proportion: DataProportion): List[Segment] = {

    proportion match {
      case Train => CLEFDataSet.trainingSegments.toList
      case Test => CLEFDataSet.testSegments.toList
      case Both => CLEFDataSet.trainingSegments.toList ++ CLEFDataSet.testSegments
    }
  }

  def getImageList(proportion: DataProportion): List[Image] = {

    proportion match {
      case Train => CLEFDataSet.trainingImages.toList
      case Test => CLEFDataSet.testImages.toList
      case Both => CLEFDataSet.trainingImages.toList ++ CLEFDataSet.testImages
    }
  }

  private def reportRoleStats(tokenInstances: List[Token], candidates: List[Token], tagName: String): Unit = {

    val instances = tokenInstances.filter(_.containsProperty(s"${tagName}_id"))
    val actual = instances.map(_.getPropertyValues(s"${tagName}_id").size()).sum
    val missingTokens = instances.toSet.diff(candidates.toSet).toList.map(_.getText.toLowerCase())
    val missing = actual - candidates.map(_.getPropertyValues(s"${tagName}_id").size()).sum

    println(s"Actual ${tagName}: $actual")
    println(s"Missing ${tagName} in the candidates: $missing ($missingTokens)")
  }

  private def reportRelationStats(candidateRelations: List[Relation], goldTrajectorRelations: List[Relation],
                                  goldLandmarkRelations: List[Relation]): Unit = {

    val missedTrSp = goldTrajectorRelations.size - candidateRelations.count(_.getProperty("RelationType") == "TR-SP")
    println("actual TR-SP:" + goldTrajectorRelations.size)
    println("Missing TR-SP in the candidates: " + missedTrSp)
    val missingTrRelations = goldTrajectorRelations
      .filterNot(r => candidateRelations.exists(x => x.getProperty("RelationType") == "TR-SP" && x.getId == r.getId))
      .map(_.getId)
    println("missing relations from TR-SP: " + missingTrRelations)

    val missedLmSp = goldLandmarkRelations.size - candidateRelations.count(_.getProperty("RelationType") == "LM-SP")
    println("actual LM-SP:" + goldLandmarkRelations.size)
    println("Missing LM-SP in the candidates: " + missedLmSp)
    val missingLmRelations = goldLandmarkRelations
      .filterNot(r => candidateRelations.exists(x => x.getProperty("RelationType") == "LM-SP" && x.getId == r.getId))
      .map(_.getId)
    println("missing relations from LM-SP: " + missingLmRelations)
  }

  private def getRolePosTagLexicon(tokenInstances: List[Token], tagName: String, minFreq: Int, isTrain: Boolean): List[String] = {

    val lexFile = new File(s"data/mSprl/${tagName.toLowerCase}PosTag.lex")
    if (isTrain) {
      val posTagLex = tokenInstances.filter(x => x.containsProperty(s"${tagName.toUpperCase}_id"))
        .map(x => pos(x)).groupBy(x => x).map { case (key, list) => (key, list.size) }.filter(_._2 >= minFreq)
        .keys.toList
      val writer = new PrintWriter(lexFile)
      posTagLex.foreach(p => writer.println(p))
      writer.close()
      posTagLex
    } else {
      if (!lexFile.exists())
        throw new IOException(s"cannot find ${lexFile.getAbsolutePath} file")
      Source.fromFile(lexFile).getLines().toList
    }
  }

  private def getSpatialIndicatorLexicon(tokenInstances: List[Token], minFreq: Int, isTrain: Boolean): List[String] = {

    val lexFile = new File("data/mSprl/spatialIndicator.lex")
    if (isTrain) {
      val sps = tokenInstances.filter(_.containsProperty("SPATIALINDICATOR_id"))
        .groupBy(_.getText.toLowerCase).map { case (key, list) => (key, list.size, list) }.filter(_._2 >= minFreq)
      val prepositionLex = sps.map(_._1).toList
      val writer = new PrintWriter(lexFile)
      prepositionLex.foreach(p => writer.println(p))
      writer.close()
      prepositionLex
    } else {
      if (!lexFile.exists())
        throw new IOException(s"cannot find ${lexFile.getAbsolutePath} file")
      Source.fromFile(lexFile).getLines().toList
    }
  }

  def getXmlReader(isTrain: Boolean): NlpXmlReader = {
    if (isTrain) trainReader else testReader
  }

  private def createXmlReader(isTrain: Boolean): NlpXmlReader = {
    val path = if (isTrain) "data/SpRL/2017/clef/train/sprl2017_train.xml" else "data/SpRL/2017/clef/gold/sprl2017_gold.xml"
    val reader = new NlpXmlReader(path, "SCENE", "SENTENCE", null, null)
    reader.setIdUsingAnotherProperty("SCENE", "DOCNO")
    reader
  }

}

