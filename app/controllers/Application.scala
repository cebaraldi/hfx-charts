package controllers

/*
 * DEBUGGING PLAY 2.2x
 * $ activator -jvm-debug 9999 run
 */

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action.async {
    val url = play.Play.application.configuration.getString("urlEcb90d")
    val xml = WS.url(url).get
    xml.map(response => {
      
      // Extract FX data from XML 
      val currList = (response.xml \\ "@currency").map(_.text).toSet.toList.sorted
      //exportCurrencies2TxtFile(currList)
      val date = (response.xml \\ "@time").map(_.text)
      
      val nod = date.length
      val noc = currList.length
      val fxDates = Array.ofDim[Int](nod,3)
      val fxRates = Array.ofDim[Double](nod, noc)
      var pos = -1
      for(d <- 0 until nod){
        fxDates(d)(0) = date(d).substring(0, 4).toInt
        fxDates(d)(1) = date(d).substring(5, 7).toInt - 1// month starts at index zero!!!
        fxDates(d)(2) = date(d).substring(8, 10).toInt
        var cl = ((response.xml\"Cube"\"Cube")(d)\"Cube"\\"@rate").toList.map(_.toString)
        for(c <- 0 until noc){
          pos = ((response.xml\"Cube"\"Cube")(d)\"Cube"\\"@currency")
            .toList.map(_.toString).indexOf(currList(c))
          if(pos>=0){
            fxRates(d)(c) = cl(pos).toDouble 
          }else{ 
            fxRates(d)(c) = -1.0
          }
        }
      }      
        
      // Extract preliminary data from xml structure
      val time = (response.xml \\ "@time").map(_.text)
      val subject = capitalz((response.xml \\ "subject").map(_.text).head) // Reference rates
      val name = capitalz((response.xml \\ "name").map(_.text).head) // European Central Bank
      Ok(views.html.index(fxRates, fxDates, currList, subject, name, time.reverse.head, time.head))
    })
  }

  def exportCurrencies2TxtFile(cl: List[String]) {
    import java.io.PrintWriter
    val pw = new PrintWriter("bar")
    pw.println(cl)
    pw.close
  }

//  def prettyPrintJ(j: org.json4s.JValue) {
//    println(JsonMethods.pretty(JsonMethods.render(j)))
//  }

  def prettyPrintX(x: scala.xml.Node) {
    val p = new scala.xml.PrettyPrinter(80, 4)
    println(p.format(x))
  }

  def capitalz(line: String): String = {
    ((line toLowerCase) split " " map (_ capitalize)).mkString(" ")
  }

}