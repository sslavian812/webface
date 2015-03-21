package controllers


import play.api.mvc.{Action, Controller}

/**
 * Created by viacheslav on 21.03.2015.
 */
object Search extends Controller {
  def empty =  Action {
    Ok(views.html.search())
  }

}
