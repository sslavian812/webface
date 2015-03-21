package controllers

import play.api._
import play.api.mvc._


object Application extends Controller {

//  def index = Action {
//    Redirect(routes.DisplayUsers.top10)
//  }

  def about() = Action {
    Ok(views.html.about())
  }

}