import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import scala.concurrent.duration._
import scala.io.Source._
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._
import scala.concurrent.impl.Future
import java.util.concurrent.Future

/**
 * ****************************************************************
 * A simple program to demonstrate the use of actor framework     *
 * ****************************************************************
 */

class HelloActor extends Actor {

  def receive = {
    case "Hello" => println("Hello back at you")
    case _       => println("Huh")
  }
}

object HelloAkka extends App {
  val system = ActorSystem("HelloSystem")
  //default actor constructor
  val helloActor = system.actorOf(Props[HelloActor], name = "helloactor")
  helloActor ! "Hello"
  helloActor ! "Hey"
}

/**
 * ****************************************************************
 * Program to calculate total number of words in a file           *
 * ****************************************************************
 */

case class StringMessage(line: String)
case class LineProcessedMessage(count: Int)

/**
 * Child Actor to process each line of text file
 */

class WordCounterActor extends Actor {

  def receive = {
    case StringMessage(line) => {
      val totalWords: Int = line.split(" ").length
      println("Total words: " + totalWords)
      sender ! LineProcessedMessage(totalWords)
    }
    case _ => println("Message Not found")
  }
}

/**
 * Parent actor to process file and calculate result
 */

case class StartProcessMessage(fileName:String)

class FileProcessorActor extends Actor {

  var running = false
  var totalLines: Int = 0
  var fileSender: Option[ActorRef] = None
  var totalWords: Int = 0
  var lineProcessed: Int = 0

  def receive = {
    case StartProcessMessage(fileName:String) => {
      fileSender = Some(sender)
      if (running) {
        println("Process is being repeted.. Terminating..!!")
      } else {
        println("Process start")
        running = true
        println("Running = true")

        fileSender = Some(sender) // save reference to process invoker
        import scala.io.Source._
        try {
          fromFile(fileName).getLines.foreach { line =>
            println("Line to process: " + line)
            context.actorOf(Props[WordCounterActor]) ! StringMessage(line)
            totalLines += 1
            println("Total lines: "+ totalLines)
          }
        } catch {
            case ex: Exception => println("Exception: " + ex)
        }
      }
    }
    case LineProcessedMessage(count) => {
      totalWords += count
      lineProcessed += 1
      println("\n\n\nLine processed="+lineProcessed +"\nTotal Lines="+totalLines)
      if (lineProcessed == totalLines) {
        println("Sending back the result")
        fileSender.map { _ ! totalWords }
      }
    }
    case _ => println("Message can not recognized")
  }
}

object WordCountProgram extends App {

  implicit val ec = global

  val system = ActorSystem("System")
  val fileActor = system.actorOf(Props(new FileProcessorActor()))
  implicit val timeout = Timeout(5 seconds)

  val futureResult = fileActor ? StartProcessMessage("/home/knoldus/test")
  
  println("***********************Waiting for result*************\nCurrent value of futureResult is:" + futureResult)
  
  futureResult.map { result =>
    print("\n\n\n\n______________Total number of words in file are:  " + result)
    println("_______________")
    system.shutdown
  }
}
