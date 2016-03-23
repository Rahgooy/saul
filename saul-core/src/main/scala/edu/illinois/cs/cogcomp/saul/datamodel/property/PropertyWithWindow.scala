package edu.illinois.cs.cogcomp.saul.datamodel.property

import java.util

import edu.illinois.cs.cogcomp.lbjava.classify.{ Classifier, FeatureVector }
import edu.illinois.cs.cogcomp.saul.datamodel.DataModel
import edu.illinois.cs.cogcomp.saul.datamodel.property.features.DataSensitiveLBJFeature
import edu.illinois.cs.cogcomp.saul.datamodel.property.features.discrete.{ DiscreteGenProperty, DiscreteProperty }
import edu.illinois.cs.cogcomp.saul.datamodel.property.features.real.{ RealGenProperty, RealProperty }

import scala.reflect.ClassTag

class PropertyWithWindow[T <: AnyRef](
  var dataModel: DataModel,
  val before: Int,
  val after: Int,
  val filters: Iterable[T => Any],
  val properties: List[Property[T]]
)(implicit val tag: ClassTag[T]) extends TypedProperty[T, List[_]] with DataModelSensitiveProperty[T] {

  // TODO: need to work on the mapping such that.
  override val sensor: (T) => List[_] = {
    t: T =>
      val ent = dataModel.getNodeWithType[T]

      val winds = ent.getWithWindow(t, before, after)
      // Now we have a windows of option items.

      properties.flatMap(property =>
        winds.map {
          case Some(x) => Some(property.sensor(x))
          case _ => None
        })
  }

  override def setDM(dm: DataModel): Unit = {
    super.setDM(dm)
    this.hiddenProperties = this.rebuildHiddenProperties(dataModel: DataModel)
  }

  var hiddenProperties: List[Property[T]] = rebuildHiddenProperties(this.dataModel)

  def rebuildHiddenProperties(dm: DataModel): List[Property[T]] = {
    {
      val ent = dm.getNodeWithType[T]

      properties.toList.flatMap {
        knowProperty: Property[T] =>
          {
            {
              type OUTPUT_TYPE = knowProperty.S

              (before to after).map {
                idx =>
                  {
                    {

                      val newName = s"WindowsClassifierAtPosition${idx}<=${knowProperty.name}"
                      //									println(newName)
                      knowProperty match {

                        case da: DiscreteProperty[T] => {
                          {
                            val newMappingFunction: T => String = {
                              t: T =>
                                {
                                  {
                                    ent.getWithRelativePosition(t, idx, filters) match {
                                      case Some(target) => {
                                        {
                                          da.sensor(target)
                                        }
                                      }
                                      case _ => {
                                        {
                                          "***BLANK***"
                                        }
                                      }
                                    }
                                  }
                                }
                            }
                            new DiscreteProperty[T](newName, newMappingFunction, da.range)
                          }
                        }
                        case dga: DiscreteGenProperty[T] => {
                          {
                            val newMappingFunction: T => List[String] = {
                              t: T =>
                                {

                                  ent.getWithRelativePosition(t, idx, filters) match {
                                    case Some(target) =>
                                      dga.sensor(target)
                                    case _ => Nil
                                  }
                                }
                            }
                            new DiscreteGenProperty[T](newName, newMappingFunction)
                          }
                        }
                        case ra: RealProperty[T] => {
                          {
                            val newMappingFunction: T => Double = {
                              t: T =>
                                {
                                  {
                                    //								ent.Nil
                                    ent.getWithRelativePosition(t, idx, filters) match {
                                      case Some(target) => {
                                        {
                                          ra.sensor(target)
                                        }
                                      }
                                      case _ => {
                                        {
                                          0
                                        }
                                      }
                                    }
                                  }
                                }
                            }
                            new RealProperty[T](newName, newMappingFunction)
                          }
                        }
                        case rga: RealGenProperty[T] => {
                          val newMappingFunction: T => List[Double] = {
                            t: T =>
                              {
                                ent.getWithRelativePosition(t, idx, filters) match {
                                  case Some(target) => {
                                    rga.sensor(target)
                                  }
                                  case _ => {
                                    Nil
                                  }
                                }
                              }
                          }
                          new RealGenProperty[T](newName, newMappingFunction)
                        }
                        case _ => {
                          throw new Exception("Can't combine classifier with ranges")
                        }
                      }

                    }
                  }
              }.toList
            }
          }
      }
    }
  }

  override def addToFeatureVector(t: T, fv: FeatureVector): FeatureVector = {
    // All it need to do is calling the curated classifiers.
    hiddenProperties.foreach(_.addToFeatureVector(t, fv))
    fv
  }

  override def addToFeatureVector(t: T, fv: FeatureVector, name: String): FeatureVector = {
    // All it need to do is calling the curated classifiers.
    hiddenProperties.foreach(_.addToFeatureVector(t, fv))
    fv
  }

  override val name: String = {
    s"WindowProperty($before,$after}_Of${this.properties.map(_.name).mkString("|")}})"
  }

  val o = this

  // TODO: use the real classifiers
  override def makeClassifierWithName(n: String): Classifier = new DataSensitiveLBJFeature() {

    val parent = o

    def rebuidWithDM(dm: DataModel) = this.parent.rebuildHiddenProperties(dm)

    this.containingPackage = "LBP_Package"
    this.name = n

    override def getOutputType: String = {
      "mixed%"
    }

    def classify(__example: AnyRef): FeatureVector = {

      val t: T = __example.asInstanceOf[T]
      val __result: FeatureVector = new FeatureVector()

      parent.hiddenProperties.foreach(_.addToFeatureVector(t, __result))

      __result
    }

    override def classify(examples: Array[AnyRef]): Array[FeatureVector] = {
      super.classify(examples)
    }

    override def getCompositeChildren: util.LinkedList[_] = {
      val result: util.LinkedList[Classifier] = new util.LinkedList[Classifier]()
      parent.properties.foreach(x => {
        result.add(x.classifier)
      })
      result
    }

    override var datamodel: DataModel = dataModel
  }

}
