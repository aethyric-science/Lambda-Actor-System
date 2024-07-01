package science.aethyric.support.actors
import java.io.InputStream

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import science.aethyric.utils.transport.JsonPathReader

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object IoStreamPathQuery {

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  case class AskId(id: Int)

  trait AnswerContent
  case class AnswerList(list: List[_]) extends AnswerContent
  case class AnswerMap(map: Map[_, _]) extends AnswerContent
  case class AnswerString(string: String) extends AnswerContent
  case class AnswerInt(`int`: Int) extends AnswerContent
  case class AnswerDouble(`double`: Double) extends AnswerContent
  case class AnswerBoolean(bool: Boolean) extends AnswerContent
  case class AnswerNull() extends AnswerContent

  trait Client
  case class Answer(content: AnswerContent, askId: AskId) extends Client

  trait Agent
  case class Ask(path: String, askId: AskId, answerTo: ActorRef[Client]) extends Agent

  def query(reader: JsonPathReader, path: String, askId: AskId, answerTo: ActorRef[Client]) = Future {
    val answerContent = reader.read(path) match {
      case list:    List[_]  ⇒ AnswerList(list)
      case map:     Map[_,_] ⇒ AnswerMap(map)
      case string:  String   ⇒ AnswerString(string)
      case intN:    Int      ⇒ AnswerInt(intN)
      case doubleN: Double   ⇒ AnswerDouble(doubleN)
      case bool:    Boolean  ⇒ AnswerBoolean(bool)
      case _                 ⇒ AnswerNull()
    }
    answerTo ! Answer(answerContent, askId)
  }

  def agent(inputStream: InputStream): Behavior[Agent] = {

    val reader = JsonPathReader(inputStream)

    Behaviors.receive { (context, message) ⇒
      message match {
        case Ask(path, askId, answerTo) ⇒
          query(reader, path, askId, answerTo)
        case _ ⇒
          context.log.warning("unrecognized message")
      }
      Behaviors.same
    }
  }

}
