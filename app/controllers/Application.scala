package controllers

import play.api._
import play.api.mvc._


object Application extends Controller {

  def index = Action {
    Ok(views.html.startPage())
  }

  def about() = Action {
    Ok(views.html.about())
  }

  def test() = Action {
    Ok(views.html.gridFace("my title"))
  }

}