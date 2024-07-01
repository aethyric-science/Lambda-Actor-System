package science.aethyric.examples.lambdaactor

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.ByteString
import akka.NotUsed

import science.aethyric.support.actors.{IoStreamOut, IoStreamPathQuery}
import science.aethyric.awslambda.LambdaRequestSystem

import com.amazonaws.services.lambda.runtime.Context

class ExampleApplication extends LambdaRequestSystem {

  /**
    * A factory function for an actor capable of handling responses to certain JSONPath requests
    * from the main lambda input interpreter.
    *
    * @param writer: The actor to which write requests should be addressed
    * @param manager The actor to which write acknowledgements should be sent.
    * @return An actor behavior
    */
  def inspectionReport(writer: ActorRef[IoStreamOut.Agent], manager: ActorRef[IoStreamOut.Client]): Behavior[IoStreamPathQuery.Client] = {
    // Name spaces can be narrowed once we're past the point of ambiguity.  This makes later code more readable
    import IoStreamPathQuery._
    import IoStreamOut._

    Behaviors.receive { (context, message) ⇒

      message match {
        case Answer(content, askId) =>
          context.log.info(s"Handling response to $askId")

          content match {
            case AnswerString(contents) ⇒
              context.log.info("requesting string write")
              writer ! WriteAndAck(ByteString(contents + "\n"), Ack(1), manager)

            case AnswerInt(contents) ⇒
              context.log.info("requesting int write")
              writer ! WriteAndAck(ByteString(contents.toString + "\n"), Ack(2), manager)

            case _ ⇒
              context.log.warning("unexpected Answer content")
          }
      }
      Behaviors.same
    }
  }

  /**
    * A factory function for an actor orchestrating read and write activities.  This is a sort of event-dispatch
    * mechanism, initiating a stream of events in the Behaviors.setup clause and responding to events in the
    * Behaviors.receive clause.
    * @param reader Reference to an actor capable of responding to JSONPath queries
    * @param writer Reference to an actor capable of responding to write requests
    * @return An actor managing orchestration tasks
    */
  def inspectionManager(reader: ActorRef[IoStreamPathQuery.Agent], writer: ActorRef[IoStreamOut.Agent]): Behavior[IoStreamOut.Client] = {
    // Name spaces can be narrowed once we're past the point of ambiguity.  This makes later code more readable
    import IoStreamOut._
    import IoStreamPathQuery._

    Behaviors.setup { context ⇒

      val reporter = context.spawn(inspectionReport(writer, context.self), "science.aethyric.inspectionReporter")

      reader ! Ask("$.AssetAuthority.name", AskId(1), reporter)

      Behaviors.receive { (context, message) =>
        context.log.debug("handling write ack")

        message match {
          case Ack(1) ⇒
            reader ! Ask("$.AssetAuthority.rank", AskId(2), reporter)
            Behaviors.same

          case Ack(2) ⇒
            Behaviors.stopped

          case Ack(ackId) ⇒
            context.log.warning(s"unexpected AckId: $ackId")
            Behaviors.same
        }
      }
    }
  }

  /**
    * The entry point for the Lambda Actor System application.  Note that the actor system itself is constructed
    * _before_ this method is invoked!  The actor system is available on the context object in Behavior.setup.
    *
    * Note that this actor simply wraps a more specialized actor.  This is because the application actor is
    * required to be of type Behavior[NotUsed], and does not itself respond to messages.
    *
    * @param readerAgent Reference to an actor capable of extracting information from the Lambda input stream
    * @param writerAgent Reference to an actor capable of writing to the Lambda output stream
    * @param lambdaContext The Lambda context object
    * @return The application actor
    */
  override def applicationActor(readerAgent: ActorRef[IoStreamPathQuery.Agent], writerAgent: ActorRef[IoStreamOut.Agent], lambdaContext: Context): Behavior[NotUsed] = {

    Behaviors.setup { context ⇒
      val manager = context.spawn(inspectionManager(readerAgent, writerAgent), "science.aethyric.inspectionManager")
      context.watch(manager)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) ⇒
          Behaviors.stopped
      }
    }
  }
}
