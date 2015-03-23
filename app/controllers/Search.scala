package controllers


import controllers.DisplayUsers._
import models.DisplayUser
import play.api.mvc.{Action, Controller}

/**
 * Created by viacheslav on 21.03.2015.
 */
object Search extends Controller {
  def empty =  Action {
    Ok(views.html.search())
  }

  def show(query: String) =  Action {
    implicit request =>
      val users = DisplayUser.getByQuery(query)
      Ok(views.html.list(users))
  }

}
