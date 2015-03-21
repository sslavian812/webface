package models

import org.json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

case class DisplayUser(link: String, snippet:String)

object DisplayUser {

  val prefix = "http://vk.com/id"

  def getTop10: List[DisplayUser] =
  {
    val s = Source.fromURL("http://localhost:9200/users/user/_search?sort=followers_count:desc&size=10&fields=id,followers_count").mkString
    val parsed: JValue = parse(s)

    var parsedUsers  = parsed \ "hits" \ "hits" // json array of users


    var listUsers : List[JValue] = parsedUsers.children.toList

    val displayUsers = listUsers.map(obj => DisplayUser(
      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
    ))
    displayUsers
  }
}