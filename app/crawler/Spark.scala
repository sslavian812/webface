package crawler

import org.apache.spark.{SparkContext, SparkConf}

object Spark {

//  val yarn = false

  val sc = {

    //Set up spark

    val conf = new SparkConf()

    conf.setAppName("needls2-crawler").setMaster("local")
//    conf.setMaster("local[2]")
//    conf.set("spark.executor.memory", "1g")

//    if (yarn)
//      conf.setMaster("yarn-client")
//    else
//      conf.setMaster("local")

//    conf.set("spark.yarn.executor.memoryOverhead", "4000")
//    conf.set("spark.executor.instances", "90")
//    conf.set("spark.driver.port", "3061")
//    conf.set("spark.broadcast.port", "3062")
//    conf.set("spark.replClassServer.port", "3063")
//    conf.set("park.blockManager.port", "3064")
//    conf.set("spark.executor.port", "3065")
//    conf.set("spark.fileserver.port", "3066")
//    conf.set("spark.executor.memory", "2g")
//    conf.set("spark.executor.cores", "4")

    val spark = new SparkContext(conf)

//    if (yarn)
//      spark.addJar(SparkContext.jarOfClass(this.getClass).get)

    spark
  }

}
