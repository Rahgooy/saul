package edu.illinois.cs.cogcomp.saulexamples.nlp.SemanticRoleLabeling

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.{ Constituent, Relation, TextAnnotation }
import edu.illinois.cs.cogcomp.core.datastructures.trees.Tree
import edu.illinois.cs.cogcomp.core.utilities.DummyTextAnnotationGenerator
import edu.illinois.cs.cogcomp.lbjava.infer.{ FirstOrderConstant, FirstOrderConstraint }
import edu.illinois.cs.cogcomp.saul.classifier.{ ConstrainedClassifier, Learnable, SparseNetworkLBP }
import edu.illinois.cs.cogcomp.saul.constraint.ConstraintTypeConversion._
import edu.illinois.cs.cogcomp.saul.datamodel.DataModel
import edu.illinois.cs.cogcomp.saulexamples.ExamplesConfigurator
import edu.illinois.cs.cogcomp.saulexamples.nlp.CommonSensors._
import edu.illinois.cs.cogcomp.saulexamples.nlp.SemanticRoleLabeling.SRLSensors._
import edu.illinois.cs.cogcomp.saul.classifier.ClassifierUtils
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConversions._

class SRLConstraintsTest extends FlatSpec with Matchers {

  object TestTextAnnotation extends DataModel {
    val predicates = node[Constituent]((x: Constituent) => x.getTextAnnotation.getCorpusId + ":" + x.getTextAnnotation.getId + ":" + x.getSpan)

    val arguments = node[Constituent]((x: Constituent) => x.getTextAnnotation.getCorpusId + ":" + x.getTextAnnotation.getId + ":" + x.getSpan)

    val relations = node[Relation]((x: Relation) => "S" + x.getSource.getTextAnnotation.getCorpusId + ":" + x.getSource.getTextAnnotation.getId + ":" + x.getSource.getSpan +
      "D" + x.getTarget.getTextAnnotation.getCorpusId + ":" + x.getTarget.getTextAnnotation.getId + ":" + x.getTarget.getSpan)

    val sentences = node[TextAnnotation]((x: TextAnnotation) => x.getCorpusId + ":" + x.getId)

    val trees = node[Tree[Constituent]]

    val stringTree = node[Tree[String]]

    val tokens = node[Constituent]((x: Constituent) => x.getTextAnnotation.getCorpusId + ":" + x.getTextAnnotation.getId + ":" + x.getSpan)

    val sentencesToTrees = edge(sentences, trees)
    val sentencesToStringTree = edge(sentences, stringTree)
    val sentencesToTokens = edge(sentences, tokens)
    val sentencesToRelations = edge(sentences, relations)
    val relationsToPredicates = edge(relations, predicates)
    val relationsToArguments = edge(relations, arguments)

    sentencesToRelations.addSensor(textAnnotationToRelation _)
    sentencesToRelations.addSensor(textAnnotationToRelationMatch _)
    relationsToArguments.addSensor(relToArgument _)
    relationsToPredicates.addSensor(relToPredicate _)
    sentencesToStringTree.addSensor(textAnnotationToStringTree _)
    val posTag = property(predicates, "posC") {
      x: Constituent => getPOS(x)
    }
    val argumentLabelGold = property(relations, "l") {
      r: Relation => r.getRelationName
    }
  }
  import TestTextAnnotation._
  object argumentTypeLearner extends Learnable[Relation](relations) {
    def label = argumentLabelGold
    override lazy val classifier = new SparseNetworkLBP() //{
    //      override def discreteValue(o: Object): String = argumentLabelGold(o.asInstanceOf[Relation])
    //      override def scores(o: Object):ScoreSet = {
    //        val result :ScoreSet= new ScoreSet()
    //        result.put(discreteValue(o), 1);
    //
    //        return result;
    //      }
    //      override def allowableValues(): Array[String]={
    //        val a= Array("A0", "AM-TMP","A1")
    //        a
    //      }
    //    }
  }

  object testConstraints {
    import TestTextAnnotation._
    val noDuplicate = ConstrainedClassifier.constraint[TextAnnotation] {
      // Predicates have atmost one argument of each type i.e. there is no two arguments of the same type for each predicate
      val values = Array("A0", "A1", "A2", "A3", "A4", "A5", "AA")
      var a: FirstOrderConstraint = null
      x: TextAnnotation => {
        a = new FirstOrderConstant(true)
        (sentences(x) ~> sentencesToRelations ~> relationsToPredicates).foreach {
          y =>
            {
              val argCandList = (predicates(y) ~> -relationsToPredicates).toList
              for (t1 <- 0 until argCandList.size - 1) {
                val b = new FirstOrderConstant(values.contains(argumentTypeLearner.classifier.getLabeler.discreteValue(argCandList.get(t1))))
                for (t2 <- t1 + 1 until argCandList.size) {
                  a = a and new FirstOrderConstant(!argumentTypeLearner.classifier.getLabeler.discreteValue(argCandList.get(t1)).equals(argumentTypeLearner.classifier.getLabeler.discreteValue(argCandList.get(t2))))
                }
                a = (b ==> a)
              }
            }
        }
        a
      }
    }
  }

  val viewsToAdd = Array(ViewNames.LEMMA, ViewNames.POS, ViewNames.SHALLOW_PARSE, ViewNames.PARSE_GOLD, ViewNames.SRL_VERB)
  val ta: TextAnnotation = DummyTextAnnotationGenerator.generateAnnotatedTextAnnotation(viewsToAdd, true)
  import TestTextAnnotation._
  import testConstraints._
  sentencesToTokens.addSensor(textAnnotationToTokens _)
  sentences.populate(Seq(ta))
  val predicateTrainCandidates = tokens.getTrainingInstances.filter((x: Constituent) => posTag(x).startsWith("IN"))
    .map(c => c.cloneForNewView(ViewNames.SRL_VERB))
  predicates.populate(predicateTrainCandidates)
  val XuPalmerCandidateArgsTraining = predicates.getTrainingInstances.flatMap(x => xuPalmerCandidate(x, (sentences(x.getTextAnnotation) ~> sentencesToStringTree).head))
  sentencesToRelations.addSensor(textAnnotationToRelationMatch _)
  relations.populate(XuPalmerCandidateArgsTraining)

  ClassifierUtils.LoadClassifier(ExamplesConfigurator.SRL_JAR_MODEL_PATH.value, argumentTypeLearner)
  "manually defined has codes" should "avoid duplications in edges and reverse edges" in {
    predicates().size should be((relations() ~> relationsToPredicates).size)
    (predicates() ~> -relationsToPredicates).size should be(relations().size)
    (predicates(predicates().head) ~> -relationsToPredicates).size should be(4)
  }

  "this constraint" should "be false" in {
    noDuplicate(ta).evaluate() should be(true)
  }
}