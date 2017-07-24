package edu.illinois.cs.cogcomp.saulexamples.mSpRL2017

import java.io.File

import edu.illinois.cs.cogcomp.saulexamples.data.{CLEFImageReader, WriteToFile}
import edu.illinois.cs.cogcomp.saulexamples.nlp.BaseTypes.{Phrase, Sentence}
import edu.illinois.cs.cogcomp.saulexamples.nlp.LanguageBaseTypeSensors._

/** Created by Umar Manzoor on 29/12/2016.
  */

import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalPopulateData._
import edu.illinois.cs.cogcomp.saulexamples.mSpRL2017.MultiModalSpRLDataModel._

object ImageApp extends App {


  populateRoleDataFromAnnotatedCorpus()

  populateTripletGroundTruth()

  triplets().foreach(t => {
//    println(t.getArgumentId(0) + "-" + t.getArgument(0) + "_" + t.getArgumentId(1) + "-" + t.getArgument(1) + "_"
//      + t.getArgumentId(2) + "-" + t.getArgument(2))
    println(tripletVisionMapping(t))

  })

// print( tripletVisionMapping(triplets().head))

}
