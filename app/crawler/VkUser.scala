package crawler

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source


case class User(
                 id: String,
                 generalInformation: GeneralInformation = GeneralInformation(),
                 groupInformation: GroupInformation = GroupInformation(),
                 sub_user_count: Int = Int.MinValue,
                 followers_count: Int = Int.MinValue,
                 wall_count: Int = Int.MinValue,
                 albums_count: Int = Int.MinValue,
                 friends: List[Long] = List.empty,
                 subscriptions: List[Long] = List.empty,
                 wall_content: List[PostInformation] = List.empty,
                 university_name: String = "",
                 first_name: String = "",
                 last_name: String = ""
                 )

case class PostInformation(
                            id: Long,
                            text: String = "",
                            likes: Int = Int.MinValue,
                            reposts: Int = Int.MinValue,
                            owner_id: Long = 0
                            )


case class GroupInformation(
                             sub_groups_count: Int = Int.MinValue,
                             groups: List[Long] = List.empty,
                             groups_detail: List[Group] = List.empty
                             )

case class Group(
                  id: Long = 0,
                  name: String = "",
                  description: String = "",
                  members_count: Long = 0
                  )

case class GeneralInformation(
                               idLong: Long = 0,
                               yearOfBirth: Int = Int.MinValue,
                               monthOfBirth: Int = Int.MinValue,
                               sex: Int = Int.MinValue,
                               uidIsChanged: Int = 1,
                               city: Int = Int.MinValue,
                               has_mobile: Int = Int.MinValue,
                               can_see_all_posts: Int = Int.MinValue,
                               twitter: Int = Int.MinValue,
                               can_see_audio: Int = Int.MinValue,
                               site: Int = Int.MinValue,
                               occupation: Int = Int.MinValue,
                               university: Int = Int.MinValue,
                               graduation_year: Int = Int.MinValue,
                               education_form: Int = Int.MinValue,
                               relation: Int = Int.MinValue,
                               country: Int = Int.MinValue
                               )

object VkUser {

  val URL_BEGINNING = "https://api.vk.com/method/"
  implicit val formats = DefaultFormats

  def getUrlForMethod(method: String) = {
    URL_BEGINNING + method + "?"
  }

  def addParameter(parametr: String, value: String) = {
    parametr + "=" + value + "&"
  }

  def addExtraInformation(user: User) = {
    println("Downloading user " + user.id)
    var changedUser = user
    changedUser = tr(changedUser, { user => addGeneralInformation(user)})
    changedUser = tr(changedUser, { user => addSubscribtionInformation(user)})
    changedUser = tr(changedUser, { user => addFollowersInformation(user)})
    changedUser = tr(changedUser, { user => addWallInformation(user)})
    changedUser = tr(changedUser, { user => addPhotoInformation(user)})
    changedUser = tr(changedUser, { user => addFriendsInformation(user)})
    changedUser = tr(changedUser, { user => addGroupDetails(user)})
    changedUser
  }


  def addSubscribtionInformation(user: User) = {
    val s = Source.fromURL(getUrlForMethod("users.getSubscriptions")
      + addParameter("user_id", user.generalInformation.idLong.toString)).mkString
    val parsed: JValue = parse(s)

    var parsedUser = tr(user, {
      user =>
        user.copy(sub_user_count =
          (parsed \ "response" \ "users" \ "count").extract[Int])
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(groupInformation = user.groupInformation.copy(sub_groups_count =
          (parsed \ "response" \ "groups" \ "count").extract[Int]))
    })


    parsedUser = tr(parsedUser, {
      user =>
        user.copy(groupInformation = user.groupInformation.copy(groups =
          (parsed \ "response" \ "groups" \ "items").extract[List[Long]]))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(subscriptions =
          (parsed \ "response" \ "groups" \ "users").extract[List[Long]])
    })

    parsedUser
  }

  def addGroupDetails(user: User) = {
    val s = Source.fromURL(getUrlForMethod("groups.getById")
      + addParameter("gids", user.groupInformation.groups.mkString(","))
      + addParameter("fields", "description,name,gid,members_count")).mkString

    val parsed: JValue = parse(s)


    val arr = (parsed \ "response").extract[List[JValue]].map(el =>
      Group((el \ "gid").extract[Long],
        (el \ "name").extract[String],
        (el \ "description").extract[String],
        (el \ "members_count").extract[Long]))

    val parsedUser = tr(user, {
      user => user.copy(groupInformation = user.groupInformation.copy(groups_detail = arr))
    })

    parsedUser
  }


  def addFollowersInformation(user: User) = {
    val s = Source.fromURL(getUrlForMethod("users.getFollowers")
      + addParameter("user_id", user.generalInformation.idLong.toString)).mkString
    val parsed: JValue = parse(s)

    val parsedUser = tr(user, {
      user =>
        user.copy(followers_count =
          (parsed \ "response" \ "count").extract[Int])
    })

    parsedUser
  }

  def addFriendsInformation(user: User) = {
    val s = Source.fromURL(getUrlForMethod("friends.get")
      + addParameter("user_id", user.generalInformation.idLong.toString)).mkString
    val parsed: JValue = parse(s)
    val parsedUser = tr(user, {
      user =>
        user.copy(friends = (
          (parsed \ "response").extract[List[Long]]))
    })

    parsedUser
  }

  def addWallInformation(user: User) = {
    val s = Source.fromURL(getUrlForMethod("wall.get")
      + addParameter("owner_id", user.generalInformation.idLong.toString)).mkString
    val parsed: JValue = parse(s)

    var parsedUser = tr(user, {
      user =>
        user.copy(wall_count = (
          (parsed \ "response" \ "count").extract[Int]))
    })

    val arr = (parsed \ "response").extract[List[JValue]]
    val arr2 = arr diff List(arr(0)) // TODO is there a better way to remove first element?
    val posts = arr2.map(post => PostInformation(
        (post \ "id").extract[Long],
        (post \ "text").extract[String],
        (post \ "likes" \ "count").extract[Int],
        (post \ "reposts" \ "count").extract[Int],
        (post \ "from_id").extract[Long]))

    parsedUser = user.copy(wall_content = posts)

    parsedUser
  }

  def addPhotoInformation(user: User) = {
    val s = Source.fromURL(getUrlForMethod("photos.getAlbums")
      + addParameter("owner_id", user.generalInformation.idLong.toString)).mkString
    val parsed: JValue = parse(s)

    val parsedUser = tr(user, {
      user =>
        user.copy(wall_count = (
          (parsed \ "response" \ "count").extract[Int]))
    })

    parsedUser
  }

  def addGeneralInformation(user: User) = {
    val whatToReturn = "id,sex,bdate,city,country,photo_50,photo_100,photo_200_orig,photo_200,photo_400_orig,photo_max,photo_max_orig,photo_id,online,online_mobile,domain,has_mobile,contacts,connections,site,education,universities,schools,can_post,can_see_all_posts,can_see_audio,can_write_private_message,status,last_seen,relation,relatives,counters,screen_name,maiden_name,timezone,occupation,activities,interests,music,movies,tv,books,games,about,quotes,first_name,last_name,university_name"
    val s = Source.fromURL(
      getUrlForMethod("users.get") +
        addParameter("fields", whatToReturn) +
        addParameter("user_ids", user.id)).mkString
    var parsedUser = user

    implicit val formats = DefaultFormats

    val parsed: JValue = parse(s)
    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(yearOfBirth = (
          (parsed \ "response" \ "bdate").extract[String]).split('.')(2).toInt))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(idLong =
          (parsed \ "response" \ "uid").extract[Long]))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(monthOfBirth = (
          (parsed \ "response" \ "bdate").extract[String]).split('.')(1).toInt))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(sex = (
          (parsed \ "response" \ "sex").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        val screen_name = (parsed \ "response" \ "screen_name").extract[String]
        if (screen_name.startsWith("id"))
          user.copy(generalInformation = user.generalInformation.copy(uidIsChanged = 0))
        else user.copy(generalInformation = user.generalInformation.copy(uidIsChanged = 1))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(city = (
          (parsed \ "response" \ "city").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(country = (
          (parsed \ "response" \ "country").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(has_mobile = (
          (parsed \ "response" \ "has_mobile").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(can_see_all_posts = (
          (parsed \ "response" \ "can_see_all_posts").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(can_see_audio = (
          (parsed \ "response" \ "can_see_audio").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(twitter = (
          if ((parsed \ "response" \ "twitter").extract[String].size > 2) 1 else 0)))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(site = (
          if ((parsed \ "response" \ "site").extract[String].size > 2) 1 else 0)))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(occupation = (
          if ((parsed \ "response" \ "occupation" \ "type").extract[String].equals("work")) 1 else 0)))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(university = (
          (parsed \ "response" \ "university").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(graduation_year = (
          (parsed \ "response" \ "graduation").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(education_form = (
          if ((parsed \ "response" \ "education_form").extract[String].equals("Дневное отделение")) 1 else 0)))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(generalInformation = user.generalInformation.copy(relation = (
          (parsed \ "response" \ "relation").extract[Int])))
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(first_name =
          (parsed \ "response" \ "first_name").extract[String])
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(last_name =
          (parsed \ "response" \ "last_name").extract[String])
    })

    parsedUser = tr(parsedUser, {
      user =>
        user.copy(university_name =
          (parsed \ "response" \ "university_name").extract[String])
    })

    parsedUser
  }

  def tr(user: User, f: (User) => User): User = {
    try {
      f.apply(user)
    } catch {
      case e: Exception => {
        //e.printStackTrace()
        user
      }
    }
  }

  def getJsonRepresentation(user: User): JObject = {
    val res = Extraction.decompose(user).asInstanceOf[JObject]
    res
  }

  def restoreFromJson(jValue: JValue): User = {
    jValue.extract[User]
  }

  def restoreFromJsonString(jsonString: String): User = {
    restoreFromJson(parse(jsonString))
  }

  def restoreFromJsonFile(path: String): User = {
    val str = scala.io.Source.fromFile(path).mkString
    restoreFromJsonString(str)
  }

  def storeUser(user: User, path: String, extesion: String = ".json"): Unit = {
    Files.write(Paths.get(path + "\\" + user.id + extesion),
      pretty(getJsonRepresentation(user)).getBytes(StandardCharsets.UTF_8))
  }

}
