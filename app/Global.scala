import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    println("started")
  }

  override def onStop(app: Application) {
    println("finished")
  }

}