package controllers

import play.api.mvc.{Action, Controller}
import models.DisplayUser

/**
 * Created by viacheslav on 21.03.2015.
 */
object  DisplayUsers extends Controller {
  def top10 =  Action {implicit request =>
    val users = DisplayUser.getTop10
    Ok(views.html.list(users))
  }
}
