package models

import java.io.{InputStreamReader, BufferedReader}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.apache.http.client.methods.HttpPost


case class DisplayUser(link: String, snippet: String)

object DisplayUser {
  implicit val formats = org.json4s.DefaultFormats

  val INDEX_PREFIX = "http://localhost:9200/vk"
  val SEARCH = "/_search"
  val LINK = "http://vk.com/id"
  val USER = "user"

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

//  @Deprecated
//  def getTop10: List[DisplayUser] = {
//    val s = Source.fromURL(search_prefix + "?sort=followers_count:desc&size=10&fields=id,followers_count").mkString
//    val parsed: JValue = parse(s)
//
//    val parsedUsers = parsed \ "hits" \ "hits" // json array of users
//
//    val listUsers: List[JValue] = parsedUsers.children.toList
//
//    val displayUsers = listUsers.map(obj => DisplayUser(
//      link = prefix + (obj \ "fields" \ "id")(0).values.toString,
//      snippet = "followers: " + (obj \ "fields" \ "followers_count")(0).values.toString
//    ))
//    displayUsers
//  }


  /**
   * Makes HTTP POST request with String Entity formed by concatenation of all elements of list
   * @param list Strings to be concatenated to form entity
   * @param url Strings url to be requested
   * @return String, representing response
   */
  def makePOSTRequestWithData(list: List[String], url: String): String = {
    val entity = list.reduce((a, b) => a + b)
    val post = new HttpPost(url)
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

  /**
   * Forms a list of DisplayUser objects, creates links and snippets.
   * @param query a query string
   * @return list of DisplayUser objects with links and snippets
   */
  def getUsersAndSnippets(query: String): List[DisplayUser] = {
    var s = makePOSTRequestWithData(List(json_request_part1, query, json_request_part2),
      INDEX_PREFIX + SEARCH)
    var parsed: JValue = parse(s)
    val listHits: List[JValue] = (parsed \ "hits" \ "hits").children.toList

    val snippetsToUsersMap = scala.collection.mutable.Map[String, String]()

    for (hit <- listHits) {
      var userId = ""
      var snippet = ""
      if (USER.equals((hit \ "_type").values))
        userId = (hit \ "_id").values.toString
      else
        userId = (hit \ "fields" \ "_parent").values.toString

      val hlFields = (hit \ "highlight").children.map(e => e.children).flatMap(e => e)
      hlFields.foreach(e => snippet ++= e.extract[String] + "... ")
      snippet = snippet.trim().replaceAll("<br>", " ")
      snippet = snippet.trim().replaceAll(" +", " ")

      var old = ""
      if (snippetsToUsersMap.contains(userId))
        old = snippetsToUsersMap(userId)
      snippetsToUsersMap(userId) = old + " " + snippet
    }
    // map (id -> snippet) created

    s = makePOSTRequestWithData(List(json_sort_part1, query, json_sort_part2), INDEX_PREFIX + SEARCH)
    parsed = parse(s)
    val listUsers: List[JValue] = (parsed \ "hits" \ "hits").children.toList


    // we don't want to show users without snippets:
    val ids = listUsers.map(
      user => (user \ "_id").extract[String]).filter(
        u => snippetsToUsersMap.contains(u)
      )
    // got relevant ids in correct order

    var responseUsers: List[DisplayUser] = List.empty

    //TODO:  possible performance loss: multiple assignments in "foreach":

    // arranging the most relevant users and it's snippets in sorted order
    ids.foreach(userID => {
      responseUsers = responseUsers :+ DisplayUser(
        LINK + userID,
        snippetsToUsersMap(userID)
      )
      snippetsToUsersMap.remove(userID)
    })

    // appending other founded users and it's snippets in any order
    snippetsToUsersMap.forall(e => {
      responseUsers = responseUsers :+ DisplayUser(
        LINK + e._1,
        e._2
      )
      true      // <----TODO: seems to be workaround
    })

    responseUsers
  }
}