package models

import java.io.{InputStreamReader, BufferedReader}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.io.Source
import org.apache.http.client.methods.HttpPost


case class DisplayUser(link: String, snippet: String)

object DisplayUser {

  implicit val formats = org.json4s.DefaultFormats

  val prefix = "http://vk.com/id"
  val search_prefix = "http://localhost:9200/vk/user/_search"
  val index_prefix = "http://localhost:9200/vk"
  val search = "/_search"
  val link = "http://vk.com/id"

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

  val json_sort_part1 = """{
    "query": {
        "has_child": {
            "type": ["post", "group"],
            "score_mode": "sum",
            "query": {
                "multi_match": {
                    "fields": [
                        "text",
                        "description",
                        "name"
                    ],
                    "query" : """"
  val json_sort_part2 = """"
                }
            }
        }
    },
    "fields" : []
    }"""

  def getTop10: List[DisplayUser] = {
    val s = Source.fromURL(search_prefix + "?sort=followers_count:desc&size=10&fields=id,followers_count").mkString
    val parsed: JValue = parse(s)

    val parsedUsers = parsed \ "hits" \ "hits" // json array of users

    val listUsers: List[JValue] = parsedUsers.children.toList

    val displayUsers = listUsers.map(obj => DisplayUser(
      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
    ))
    displayUsers
  }


  @Deprecated
  def getByQuery(query: String): List[DisplayUser] = {
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val s = Source.fromURL(search_prefix + "?q=" + encodedQuery
      + "&sort=followers_count:desc" + "&size=10" + "&fields=id,followers_count").mkString

    val parsed: JValue = parse(s)

    val parsedUsers = parsed \ "hits" \ "hits" // json array of users

    val listUsers: List[JValue] = parsedUsers.children.toList

    val displayUsers = listUsers.map(obj => DisplayUser(
      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
    ))
    displayUsers
  }


  def makePOSTRequestWithData(list : List[String]) : String =
  {
    val entity = list.reduce((a,b) => a+b)
    val post = new HttpPost(index_prefix + search)
    post.setHeader("Content-type", "application/json")
    post.setEntity(new StringEntity(entity, "UTF-8"))

    val client = new DefaultHttpClient
    val response = client.execute(post)


    val rd: BufferedReader = new BufferedReader(new InputStreamReader(response.getEntity.getContent, "UTF-8"))
    val builder = new StringBuilder()
    try {
      val line = rd.readLine
      builder.append(line)
      //      while (line != null) {
      //        builder.append(line + "\n")
      //        line = rd.readLine
      //      }
    } finally {
      rd.close
    }

    builder.toString
  }

  def getUsersAndSnippets(query: String): List[DisplayUser] = {
    var s = makePOSTRequestWithData(List(json_request_part1, query, json_request_part2))
    var parsed: JValue = parse(s)
    val listHits: List[JValue] = (parsed \ "hits" \ "hits").children.toList

    val snippetsToUsersMap = scala.collection.mutable.Map[String, String]()

    for (hit <- listHits) {
      var userId = ""
      var snippet = ""
      if ((hit \ "_type").values == "user")
        userId = (hit \ "_id").values.toString
      else
        userId = (hit \ "fields" \ "_parent").values.toString

      val hlFields = (hit \ "highlight").children
      val hl2 = hlFields.map(e => e.children).flatMap(e => e)

      hl2.foreach(e => snippet ++= " ... " + e.extract[String] + " ... ")

      snippet = snippet.trim().replaceAll("<br>", " ")
      snippet = snippet.trim().replaceAll(" +", " ")

      var old = ""
      if(snippetsToUsersMap.contains(userId))
        old = snippetsToUsersMap(userId)
      snippetsToUsersMap(userId) = old + " " + snippet
    }
    // map (id -> snippet) created

    s = makePOSTRequestWithData(List(json_sort_part1, query, json_sort_part2))
    parsed = parse(s)
    val listUsers: List[JValue] = (parsed \ "hits" \ "hits").children.toList

    var responseUsers: List[DisplayUser] = List.empty

    val ids = listUsers.map(
      user => (user \ "_id").extract[String]).filter(
        u => snippetsToUsersMap.contains(u)
      )
    // got relevant ids in correct order

    responseUsers = ids.map(user =>
      DisplayUser(
        link + user,
        snippetsToUsersMap(user)
      )
    )

    responseUsers
  }

}