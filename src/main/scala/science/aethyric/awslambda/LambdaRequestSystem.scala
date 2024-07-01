package science.aethyric.awslambda

import java.io.{InputStream, OutputStream}

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import science.aethyric.support.actors.{IoStreamOut, IoStreamPathQuery}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LambdaRequestSystem extends RequestStreamHandler {

  val applicationName: String = getClass.getName
  val actorSystemName: String = getClass.getName.replace(".", "_") + "_system"

  def applicationActor(readerAgent: ActorRef[IoStreamPathQuery.Agent], writerAgent: ActorRef[IoStreamOut.Agent], lambdaContext: Context): Behavior[NotUsed] = {
    Behaviors.stopped
  }

  override def handleRequest(input: InputStream, output: OutputStream, lambdaContext: Context): Unit = {

    val main: Behavior[NotUsed] = Behaviors.setup { context ⇒

      context.log.debug(s"Available memory: ${lambdaContext.getMemoryLimitInMB}")
      context.log.debug(s"Available cores: ${Runtime.getRuntime.availableProcessors()}")

      context.log.debug("initializing query agent")
      val jsonQueryAgent = context.spawn(IoStreamPathQuery.agent(input), "lambdaJsonQueryAgent")

      context.log.debug("initializing write agent")
      val streamOutAgent = context.spawn(IoStreamOut.agent(output), "lambdaStreamOutAgent")

      context.log.debug("initializing application")

      val application = context.spawn(applicationActor(jsonQueryAgent, streamOutAgent, lambdaContext), applicationName)

      context.watch(application)
      Behaviors.receiveSignal {
        case (_, Terminated(_)) ⇒
          Behaviors.stopped
      }
    }

    val actorSystem = ActorSystem(main, actorSystemName)
    Await.ready(actorSystem.whenTerminated, Duration.Inf)
  }
}
