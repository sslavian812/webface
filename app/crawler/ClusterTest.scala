package crawler


import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.{ListBuffer}
import scala.util.Random
import crawler.Spark.sc

object ClusterTest {

  def main(args: Array[String]): Unit = {

    val users: RDD[User] =
      sc.parallelize(Array(
        User(id = "1", groupInformation = GroupInformation(groups = List(7, 8, 9, 10, 11))),
        User(id = "2", groupInformation = GroupInformation(groups = List(7, 8, 9, 10, 13))),
        User(id = "3", groupInformation = GroupInformation(groups = List(7, 8, 9, 10, 12))),
        User(id = "4", groupInformation = GroupInformation(groups = List(11, 12, 13, 14))),
        User(id = "5", groupInformation = GroupInformation(groups = List(11, 12, 13, 14))),
        User(id = "6", groupInformation = GroupInformation(groups = List(11, 12, 13, 14)))
      ))

    clusterCreation(users).collect
      .foreach(u => {
      println(u._1.id)
      u._2.foreach(p => println("\t" + p))
    })

  }

  val numberOfCluster = 700

  def clusterCreation(users: RDD[User]) = {


    val s = (0 to numberOfCluster - 1)
      .toList
      .map(i => (Math.abs(Random.nextLong), (SomeVertex(), getColor(i))))

    val take = Random.shuffle(users
      .flatMap(_.groupInformation.groups)
      .groupBy(i => i)
      .map(i => (i._1, i._2.size))
      .sortBy(_._2, false)
      .map(_._1)
      .take(3000)
      .toSeq
    )
      .take(numberOfCluster)

    println("s size = " + s.size)
    println("take size = " + take.size)

    val fakeEdges = (take
      .zip(s)
      .map(i => (i._1, i._2._1, 1.0)) ++
      take
        .zip(s)
        .map(i => (i._2._1, i._1, 1.0)))
      .map(i => Edge(i._1, i._2, i._3))


    val vertex = (users.map(user => (user.generalInformation.idLong, (SomeVertex(user = user), getColor()))) ++
      users.flatMap(user => user.groupInformation.groups).map(g => (g, (SomeVertex(group = g), getColor()))) ++
      sc.parallelize(s))

    val edges = (users.flatMap(user =>
      user.groupInformation.groups.map(group => (group, user.generalInformation.idLong)))
      .map(e => Edge(e._1, e._2, 1.0)) ++
      users.flatMap(user =>
        user.groupInformation.groups.map(group => (group, user.generalInformation.idLong)))
        .map(e => Edge(e._2, e._1, 1.0)) ++
      /*users.flatMap(user =>
        user.friends.map(friend => (friend, getVertexIdById(user.id))))
        .map(e => Edge(e._2, e._1, 0.0)) ++
      */ sc.parallelize(fakeEdges))

    //vertex.cache()
    //edges.cache()

    val graph = Graph(vertex, edges)
    graph.cache()

    /*    graph
          .vertices
          .collect()
          .foreach(i => {
          println(i._1)
          // i._2._2.colors.foreach(j => println("\t" + j))
        })

        graph
          .edges
          .collect()
          .foreach(i => println(i.srcId + " " + i.dstId))*/


    val result = Pregel.apply(graph, getColor(), maxIterations = 40)({
      (userId, vertex, incomeColor) =>
        (vertex._1,
          mergeColors(getPartOfColor(0.9, vertex._2), incomeColor))
    }, {
      triplet =>
        Iterator.single(triplet.dstId, getPartOfColor(0.1 * triplet.attr, triplet.srcAttr._2))
    }, mergeColors)

    val pr = result
      .vertices
      .filter(v => v._2._1.user != null)
      .map(v => (v._2._1.user, v._2._2))


/*    println("Attention")
    pr
      .filter(_._2.colors.sum > 0)
      .collect()
      .foreach(i => println(i._1.id + " " + i._2.colors.sum))
    println("No more attention")*/

    val max = pr.map(_._2).reduce(colorMax)

    pr
      .map(v => (v._1, normalize(v._2, max)))
      .map(v => (v._1, v._2.colors.zipWithIndex))

  }

  def getVertexIdById(s: String) = {
    try {
      val processed = if (s.contains("id")) s.substring(2) else s
      processed.toLong
    } catch {
      case e: Exception =>
        Math.abs(s.hashCode.toLong)
    }
  }

  def getColor(): Color = {
    val buffer = ListBuffer[Double]()
    for (i <- 0 to numberOfCluster - 1) {
      buffer.append(0.0)
    }
    Color(buffer.toArray)
  }

  def getColor(id: Int): Color = {
    val buffer = ListBuffer[Double]()
    for (i <- 0 to numberOfCluster - 1) {
      if (i == id)
        buffer.append(10000.0)
      else
        buffer.append(0.0)
    }
    Color(buffer.toArray)
  }

  def mergeColors(color1: Color, color2: Color): Color = {
    val arrayBuffer = ListBuffer[Double]()
    for (i <- 0 to numberOfCluster - 1) {
      arrayBuffer.append(color1.colors(i) + color2.colors(i))
    }
    Color(arrayBuffer.toArray)
  }

  def colorMax(color1: Color, color2: Color): Color = {
    val arrayBuffer = ListBuffer[Double]()
    for (i <- 0 to numberOfCluster - 1) {
      arrayBuffer.append(Math.max(color1.colors(i), color2.colors(i)))
    }
    Color(arrayBuffer.toArray)
  }

  def normalize(color1: Color, max: Color): Color = {
    val arrayBuffer = ListBuffer[Double]()
    for (i <- 0 to numberOfCluster - 1) {
      arrayBuffer.append(color1.colors(i) / max.colors(i))
    }
    Color(arrayBuffer.toArray)
  }

  def getPartOfColor(part: Double, color: Color) = {
    val arrayBuffer = ListBuffer[Double]()
    for (i <- color.colors) {
      arrayBuffer.append(i * part)
    }
    Color(arrayBuffer.toArray)
  }

  case class Color(colors: Array[Double])

  case class SomeVertex(user: User = null.asInstanceOf[User], group: Long = null.asInstanceOf[Long])

}
