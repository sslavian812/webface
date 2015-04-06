package models

import java.io.{InputStreamReader, BufferedReader}

import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

import org.apache.http.client.methods.HttpPost





case class DisplayUser(link: String, snippet:String)

object DisplayUser {

  implicit val formats = org.json4s.DefaultFormats

  val prefix = "http://vk.com/id"
  val search_prefix = "http://localhost:9200/vk/user/_search"

  val index_prefix = "http://localhost:9200/vk"
  val search = "/_search"

  val json_request_part1 = """{
                           "query": {
                               "multi_match": {
                                   "fields": [
                                       "text",
                                       "description",
                                       "name",
                                       "university_name",
                                       "first_name",
                                       "last_name"
                                   ],
                                   "query" :""""
  val json_request_part2 = """"
                                     }
                                 },
                                 "highlight": {
                                     "pre_tags" : ["<b>"],
                                     "post_tags" : ["</b>"],
                                     "fields": {
                                         "*": {}
                                     }
                                 },
                                 "fields" : ["_parent", "_source"]
                             }"""

  def getTop10: List[DisplayUser] =
  {
    val s = Source.fromURL(search_prefix + "?sort=followers_count:desc&size=10&fields=id,followers_count").mkString
    val parsed: JValue = parse(s)

    val parsedUsers  = parsed \ "hits" \ "hits" // json array of users

    val listUsers : List[JValue] = parsedUsers.children.toList

    val displayUsers = listUsers.map(obj => DisplayUser(
      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
    ))
    displayUsers
  }


  def getByQuery(query: String): List[DisplayUser] =
  {
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val s = Source.fromURL(search_prefix + "?q=" + encodedQuery
      + "&sort=followers_count:desc" + "&size=10" + "&fields=id,followers_count").mkString

    val parsed: JValue = parse(s)

    val parsedUsers  = parsed \ "hits" \ "hits" // json array of users

    val listUsers : List[JValue] = parsedUsers.children.toList

    val displayUsers = listUsers.map(obj => DisplayUser(
      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
    ))
    displayUsers
  }


  def getUsersAndSnippets(query: String): List[DisplayUser] =
  {
    val post = new HttpPost(index_prefix + search)
    post.setHeader("Content-type", "application/json")
    post.setEntity(new StringEntity(json_request_part1 + query + json_request_part2, "UTF-8"))

    val client = new DefaultHttpClient
    val response = client.execute(post)


    val rd: BufferedReader = new BufferedReader(new InputStreamReader(response.getEntity.getContent, "UTF-8"))
    val builder = new StringBuilder()
    try {
      var line = rd.readLine
      builder.append(line)
//      while (line != null) {
//        builder.append(line + "\n")
//        line = rd.readLine
//      }
    } finally {
      rd.close
    }
    val s = builder.toString


    val parsed: JValue = parse(s)

    val parsedUsers  = parsed \ "hits" \ "hits" // json array of users

    val listHits : List[JValue] = parsedUsers.children.toList

    var responseUsers: List[DisplayUser] = List.empty

    for (hit <- listHits)
    {
      var link = "http://vk.com/id"
      var snippet = ""
      if((hit \ "_type").values  == "user")
        link ++= (hit \ "_id").values.toString
      else
        link ++= (hit \ "fields" \ "_parent").values.toString

      val jsonObj = (hit\ "highlight")

      val hlFields = (hit \ "highlight").children

      println(hlFields)

      val hl2 = hlFields.map(e => e.children).flatMap(e => e)
      println(hl2)

      hl2.foreach(e => snippet ++=" ... " + e.extract[String] + " ... ")

      snippet = snippet.trim().replaceAll("<br>", " ")
      snippet = snippet.trim().replaceAll(" +", " ")
      responseUsers ::= DisplayUser(link, snippet)
    }

    responseUsers
  }

}