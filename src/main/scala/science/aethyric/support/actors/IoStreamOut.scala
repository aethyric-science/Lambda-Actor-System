package science.aethyric.support.actors

import java.io.OutputStream

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString

object IoStreamOut {

  trait Client
  case class Ack(ackId: Int) extends Client

  trait Agent
  case class Write(content: ByteString) extends Agent
  case class WriteAndAck(content: ByteString, ack: Ack, ackTo: ActorRef[Client]) extends Agent

  def agent(outputStream: OutputStream): Behavior[Agent] = {

    Behaviors.receive { (context, message) ⇒
      message match {
        case Write(content) ⇒
          context.log.debug(s"writing to stream on ${context.self.path.toStringWithoutAddress}")
          outputStream.write(content.toByteBuffer.array)

        case WriteAndAck(content, ack, ackTo) ⇒
          context.log.debug(s"writing with ack to stream on ${context.self.path.toStringWithoutAddress}")
          outputStream.write(content.toByteBuffer.array)

          context.log.debug(s"acknowledging ${ack.ackId.toString} on ${context.self.path.toStringWithoutAddress}")
          ackTo ! ack
      }
      Behaviors.same
    }

  }

}
