package crawler

import crawler.Spark.sc

import org.json4s._
import org.json4s.jackson.JsonMethods._


/**
 * usage: path iterations start_user_id
 */
object UsersCollecting {

  implicit val formats = DefaultFormats
  val usersPerIteration = 10

  def main(args: Array[String]): Unit = {

    val storageDirectory =
    if(args.length >= 1)
      args(0)
    else
      "D:\\tmp\\downloading"

    val iterations : Int=
    if(args.length >= 2)
      args(1).toInt
    else
      1

    val startId : String =
    if(args.length >= 3)
      args(2)
    else
      "1"

    var idsToLoad = sc.parallelize(List(startId))
    var loadingQueue = sc.parallelize(List[String]())
    var loadedIds = sc.parallelize(List[String]())
    ElasticSearchHelper.init()


    for( i <- 0 until iterations) {
      val currentUsers = idsToLoad.map(u => VkUser.addExtraInformation(User(u)))
      // load Users

      loadedIds = loadedIds.union(currentUsers.map(_.id))
      println("=======" + i + "-th iteration")

      val friends = currentUsers.flatMap(_.friends).distinct()
      loadingQueue = friends.map(_.toString).subtract(loadedIds)

      println("####### before substracting")
      println("total: "+ loadingQueue.count())
      loadingQueue = loadingQueue.subtract(idsToLoad)
      println("####### after substracting")
      println("total: "+loadingQueue.count())

      idsToLoad = sc.parallelize(loadingQueue.take(usersPerIteration))
      // update downloading queue

      //currentUsers.foreach(u => VkUser.storeUser(u, storageDirectory))
      currentUsers.foreach(u =>  ElasticSearchHelper.addUser(u))

      println(usersPerIteration + " more users added to index")
    }

    println("success!")
  }
}
