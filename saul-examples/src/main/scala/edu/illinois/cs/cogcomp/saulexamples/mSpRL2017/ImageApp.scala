package edu.illinois.cs.cogcomp.saulexamples.mSpRL2017

import java.io.File

import edu.illinois.cs.cogcomp.saulexamples.data.{CLEFImageReader, WriteToFile}
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.Helpers.SpRLXmlReader
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLClassifiers.{ImageClassifierWeka, ImageSVMClassifier}
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.mSpRLConfigurator._
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalPopulateData._

/** Created by Umar Manzoor on 29/12/2016.
  */

import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalPopulateData._
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._

object ImageApp extends App {

  populateRoleDataFromAnnotatedCorpus()
  populateTripletGroundTruth()

 print( tripletVisionMapping(triplets().head))

}
