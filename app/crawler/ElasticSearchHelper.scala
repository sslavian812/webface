package crawler

import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.bulk.{BulkResponse, BulkRequestBuilder}
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._

import org.elasticsearch.node.NodeBuilder._
import org.elasticsearch.node._
import org.elasticsearch.client._

/**
 * Created by viacheslav on 11.04.2015.
 */
object ElasticSearchHelper {
  val PROTOCOL = "http:://"
  val HOST = "localhost:9200"
  val INDEX = "vk"
  val USER = "user"
  val POST = "post"
  val GROUP = "group"

  var node: Node = null
  var client: Client = null

  /**
   * initialises helper. created node and gets client, which are private fields.
   */
  def init(): Unit = {
    node = nodeBuilder().node()
    client = node.client()
  }

  /**
   * process given user-data, and pushes User and it's childs documents to elasticsearch's index.
   * should be public method.
   * @param user downloaded and stored in object 'User' user data
   */
  def addUser(user: User): Unit = {
    println("Indexing user: " + user.id)
    val userBuilder: XContentBuilder = jsonBuilder()
      .startObject()
      .field("id", user.id)
      .field("sub_groups_count", user.groupInformation.sub_groups_count)
      //.field("groups", user.groupInformation.groups) // startAraay?
      .startArray("groups")
    user.groupInformation.groups.foreach(g => userBuilder.value(g))
    userBuilder.endArray()
      .field("sub_user_count", user.sub_user_count)
      .field("followers_count", user.followers_count)
      .field("wall_count", user.wall_count)
      .field("albums_count", user.albums_count)
      //.field("friends", user.friends)
      .startArray("friends")
    user.friends.foreach(f => userBuilder.value(f))
    userBuilder.endArray()
      //.field("subscriptions", user.subscriptions)
      .startArray("subscriptions")
    user.subscriptions.foreach(s => userBuilder.value(s))
    userBuilder.endArray()
      .field("university_name", user.university_name)
      .field("first_name", user.first_name)
      .field("last_name", user.last_name)

      .field("idLong", user.generalInformation.idLong)
      .field("yearOfBirth", user.generalInformation.yearOfBirth)
      .field("monthOfBirth", user.generalInformation.monthOfBirth)
      .field("sex", user.generalInformation.sex)
      .field("uidIsChanged", user.generalInformation.uidIsChanged)
      .field("city", user.generalInformation.city)
      .field("has_mobile", user.generalInformation.has_mobile)
      .field("can_see_all_posts", user.generalInformation.can_see_all_posts)
      .field("twitter", user.generalInformation.twitter)
      .field("can_see_audio", user.generalInformation.can_see_audio)
      .field("site", user.generalInformation.site)
      .field("occupation", user.generalInformation.occupation)
      .field("university", user.generalInformation.university)
      .field("graduation_year", user.generalInformation.graduation_year)
      .field("education_form", user.generalInformation.education_form)
      .field("relation", user.generalInformation.relation)
      .field("country", user.generalInformation.country)
      .endObject()

    val posts = user.wall_content
    val postsStrings = posts.map(post => jsonBuilder()
      .startObject()
      .field("id", post.id)
      .field("text", post.text)
      .field("likes", post.likes)
      .field("reposts", post.reposts)
      .field("owner_id", post.owner_id)
      .endObject()
    )

    val groups = user.groupInformation.groups_detail
    val gropsStrings = groups.map(group => jsonBuilder()
      .startObject()
      .field("id", group.id)
      .field("name", group.name)
      .field("description", group.description)
      .field("members_count", group.members_count)
      .endObject()
    )

    //println(userBuilder.string())

    indexData(List(userBuilder), USER)
    indexData(postsStrings, POST, user.id)
    indexData(gropsStrings, GROUP, user.id)
  }

  /**
   * makes request and posts data to be indexed.
   * should be private method.
   * @param data
   * @param dataType  <tex>\in</tex> {USER, POST, GROUP}
   */
  def indexData(data: List[XContentBuilder], dataType: String, parent: String = ""): Unit = {
    if (data.size == 0) {
      println("no data of type " + dataType + "will be indexed: no data provided")
      return
    }
    if (USER.equals(dataType)) {
      val response: IndexResponse = client.prepareIndex(INDEX, USER)
        .setSource(data(0))
        .execute()
        .actionGet()
    }
    else {
      val bulkRequest: BulkRequestBuilder = client.prepareBulk()
      data.foreach(e =>
        bulkRequest.add(client.prepareIndex(INDEX, dataType) // routing problem
          .setSource(e).setParent(parent)
        ))
      val bulkResponse: BulkResponse = bulkRequest.execute().actionGet()

      if (bulkResponse.hasFailures) {
        println("a bunch of " + dataType + " added to index. failures: " + bulkResponse.hasFailures.toString)
        println(bulkResponse.buildFailureMessage())
      }
    }
  }
}
