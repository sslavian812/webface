package controllers

import models.DisplayUser
import play.api.mvc.{Action, Controller}

/**
 * Created by viacheslav on 21.03.2015.
 */
object Search extends Controller {
  def empty =  Action {
    Redirect(routes.Application.about)
  }

  def show(query: String) =  Action {
    implicit request =>
      val users: List[DisplayUser] = DisplayUser.getUsersAndSnippets(query)
//      Ok(views.html.twoElementContainer(views.html.searchForm())(views.html.list(users)))
      Ok(views.html.main(query)(views.html.list(users)))
  }

  def top10 =  Action {implicit request =>
    val users = DisplayUser.getTop10
    Ok(views.html.list(users))
  }

}
