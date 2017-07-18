package edu.illinois.cs.cogcomp.saulexamples.mSpRL2017

import edu.illinois.cs.cogcomp.saulexamples.data.{CLEFImageReader, WriteToFile}
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLClassifiers.{ImageClassifierWeka, ImageSVMClassifier}
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._

import scala.collection.JavaConversions._

/** Created by Umar Manzoor on 29/12/2016.
  */
object ImageApp extends App {

  val readFullData = false
  val writer = new WriteToFile("Train-Referit");
  val CLEFDataset = new CLEFImageReader("data/mSprl/saiapr_tc-12", "data/mSprl/newSprl2017_train.xml", "data/mSprl/newSprl2017_gold.xml", readFullData)

  val imageListTrain = CLEFDataset.trainingImages
  val segmentListTrain = CLEFDataset.trainingSegments
  val relationListTrain = CLEFDataset.trainingRelations

  images.populate(imageListTrain)
  segments.populate(segmentListTrain)
  segmentRelations.populate(relationListTrain)

  val imageListTest = CLEFDataset.testImages
  val segementListTest = CLEFDataset.testSegments
  val relationListTest = CLEFDataset.testRelations

  images.populate(imageListTest, false)
  segments.populate(segementListTest, false)
  segmentRelations.populate(relationListTest, false)

  ImageSVMClassifier.learn(5)
  //ImageSVMClassifier.test(segementListTest)

  images() prop imageRelations

  //  ImageClassifierWeka.learn(5)
//  ImageClassifierWeka.test(segementListTest)

}
/*
val totalseg = segmentListTrain.length
var matchseg = 0

for(s <- segmentListTrain) {
val segmentText = s.getSegmentConcept
var once = 0;
writer.WriteTextln("------------------------------------")
writer.WriteTextln(s.getAssociatedImageID + "_" + s.getSegmentId)
writer.WriteTextln("Segment Concept ->" + s.getSegmentConcept())
writer.WriteTextln("Segment Ontology ->" + s.ontologyConcepts.toString())
writer.WriteTextln("Refer it Text ->" + s.referitText.toString())
writer.WriteTextln("---Direct word to word Mapping------")
for (sText <- s.referitText) {
if (!segmentText.contains("-")) {
writer.WriteText(segmentText + " " + sText + "->")
val similarly = MultiModalSpRLSensors.getGoogleSimilarity(segmentText.toLowerCase(), sText.toLowerCase())
if (similarly >= 0.40 && once == 0) {
matchseg += 1
once = 1
}
writer.WriteTextln(similarly.toString())
}
else {
writer.WriteTextln("---Splitting the word---------------")
val segWords = segmentText.split("-")
for(sw <- segWords) {
writer.WriteText(sw + " " + sText + "->")
val similarly = MultiModalSpRLSensors.getGoogleSimilarity(sw.toLowerCase(), sText.toLowerCase())
if (similarly >= 0.40 && once == 0) {
matchseg += 1
once = 1
}
writer.WriteTextln(similarly.toString())
}
}
}
/*    // Use Ontology for Mapping
    if(once==0) {
      writer.WriteTextln("---Using Segment Ontology-----------")
      for (sText <- s.referitText) {
        for (o <- s.ontologyConcepts) {
          writer.WriteText(o + " " + sText + "->")
          val similarly = MultiModalSpRLSensors.getGoogleSimilarity(o.toLowerCase(), sText.toLowerCase())
          if (similarly >= 0.40 && once == 0) {
            matchseg += 1
            once = 1
          }
          writer.WriteTextln(similarly.toString())
        }
      }
    }*/
}
writer.WriteText("Total Segments ->" + totalseg)
writer.WriteText("Matched Concepts ->" + matchseg)
writer.closeWriter()
*/
