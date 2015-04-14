package crawler


import java.io.{PrintWriter, FileWriter, BufferedWriter, File}
import org.apache.spark.rdd.RDD
import scala.io.Source
import scala.util.Random

object Run {

  case class LP(label: Int, features: List[Double])

  def main(args: Array[String]): Unit = {
    storeResult()
    splitResult()
  }

def splitResult() = {
  val i = Random.shuffle(Source
    .fromFile("/home/epahomov/test2/processed.txt")
    .getLines()
    .toList)
  val train = i.take((i.size * 0.8).toInt)
  val validate = i.takeRight((i.size * 0.2).toInt)

  val trainFile = new PrintWriter(new File("/home/epahomov/test2/features.txt"))
  trainFile.print(train.mkString("\n"))
  trainFile.close()

  val validateFile = new PrintWriter(new File("/home/epahomov/test2/features_test.txt"))
  validateFile.print(validate.mkString("\n"))
  validateFile.close()

}

def storeResult() = {

  val results = Source
    .fromFile("/home/epahomov/ipython/train.csv")
    .getLines() /*++
    Source
      .fromFile("/Users/epahomov/ipython/validation.csv")
      .getLines())*/
    .filter(s => !s.contains("DEF"))
    .map(s => (s.split(';')(0) -> s.split(';')(2).toInt))
    .toMap

  var users: RDD[User] = Spark.sc.objectFile[User]("/user/epahomov/downloading/1")
  val neededUsers = users.collect().map(_.generalInformation.idLong).toSet
  for (i <- 2 to 3) {
    users = users ++ Spark.sc.objectFile[User]("/user/epahomov/downloading/" + i)
  }
  /* users
     .filter(_.groupInformation.groups.size == 0)
     .collect()
     .foreach(i => println(i.generalInformation.idLong))

   println(users.filter(_ => true).count() + " " + users.filter(_.groupInformation.groups.size > 0).count())
   System.exit(0)*/
  var forMatrixNet = ClusterTest.clusterCreation(users)


  val results2 = getMatched
    .flatMap(s => if (results.contains(s._2)) {
    Some((s._1.id, results.get(s._2).get))
  } else None)
    .map(s => s._1 -> s._2)
    .toMap

  /* val forMatrixNet = rez
     .flatMap(s => try (Some(VkUser.addExtraInformation(s._1), s._2)) catch {
     case e: Exception => None
   })*/
  val r = forMatrixNet
    .filter(i => neededUsers.contains(i._1.generalInformation.idLong))
    .map(user => (user._1, results2.getOrElse(user._1.id, 0), user._2))
    .map(s => (getFeauterSet(s._1, s._3), s._2))
    .map(s => LP(s._2, s._1))

  val toList: List[LP] = r.collect().toList

  println("0 is - " + toList.count(_.label == 0))
  println("1 is - " + toList.count(_.label == 1))

  storeInputFile(toList, new File("/home/epahomov/test2/processed.txt"))
}

def getMatched: Iterator[(User, String)] = {
  Source
    .fromFile("/home/epahomov/ipython/matched.csv")
    .getLines()
    .map(s => (s.split(',')(0), s.split(',')(1)))
    .flatMap(s => if (s._1.contains("vk")) Some(
    if (s._1.contains(".ru/"))
      s._1.split(".ru/")(1)
    else
      s._1.split(".com/")(1), s._2)
  else None)
    .map(s => (User(s._1), s._2))
}

def getFeauterSet(user: User, clusterFeature: Array[(Double, Int)]) = {
  val list = List[Double](
    user.generalInformation.yearOfBirth, //0
    user.generalInformation.monthOfBirth, //1
    user.generalInformation.sex, //2
    user.generalInformation.uidIsChanged, //3
    user.generalInformation.city, //4
    user.generalInformation.has_mobile, //5
    user.generalInformation.can_see_all_posts, //6
    user.generalInformation.twitter, //7
    user.generalInformation.can_see_audio, //8
    user.generalInformation.site, //9
    user.generalInformation.occupation, //10
    user.generalInformation.university, //11
    user.generalInformation.graduation_year, //12
    user.generalInformation.education_form, //13
    user.generalInformation.relation, //14
    user.generalInformation.country, //15
    user.sub_user_count, // 16
    user.groupInformation.sub_groups_count, // 17
    user.followers_count, // 18
    user.wall_count, // 19
    user.albums_count // 20
  )
  val buffer = list.toBuffer
  clusterFeature
    .sortBy(_._2)
    .foreach(i => buffer.append(i._1))
  buffer.toList
}

def storeInputFile(input: List[LP], trainFile: File) {
  val writer: BufferedWriter = new BufferedWriter(new FileWriter(trainFile))
  for (i <- input) {
    var str: String = "1\t" + i.label + "\turl\t-"
    for (feature <- i.features.toArray) {
      str += "\t" + feature
    }
    writer.write(str + "\n")
  }
  writer.close
}

}
