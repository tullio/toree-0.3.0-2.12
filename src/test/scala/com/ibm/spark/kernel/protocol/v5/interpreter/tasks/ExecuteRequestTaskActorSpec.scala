package com.ibm.spark.kernel.protocol.v5.interpreter.tasks

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.ibm.spark.interpreter.Interpreter
import com.ibm.spark.kernel.protocol.v5._
import com.ibm.spark.kernel.protocol.v5.content._
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock._
import org.scalatest.{FunSpecLike, Matchers}

import com.ibm.spark.interpreter._

import scala.tools.nsc.interpreter._
import scala.concurrent.duration._
import scala.util.Either.LeftProjection

object ExecuteRequestTaskActorSpec {
  val config = """
    akka {
      loglevel = "WARNING"
    }"""
}

class ExecuteRequestTaskActorSpec extends TestKit(
  ActorSystem(
    "ExecuteRequestTaskActorSpec",
    ConfigFactory.parseString(ExecuteRequestTaskActorSpec.config)
  )
) with ImplicitSender with FunSpecLike with Matchers with MockitoSugar
{
  describe("ExecuteRequestTaskActor") {
    describe("#receive") {
      it("should return an ExecuteReplyOk if the interpreter returns success") {
        val mockInterpreter = mock[Interpreter]
        doReturn(IR.Success, Left(new ExecuteOutput)).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        executeRequestTask ! executeRequest

        // TODO: Convert to tuple of (ExecuteReplyOk, ExecuteResult)
        val response =
          receiveOne(5.seconds).asInstanceOf[Tuple2[ExecuteReply, ExecuteResult]]
        response._1 shouldBe an [ExecuteReplyOk]
        response._2 shouldBe an [ExecuteResult]
      }

      it("should return an ExecuteReplyError if the interpreter returns error") {
        val mockInterpreter = mock[Interpreter]
        doReturn(IR.Error, Right(mock[ExecuteError])).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        executeRequestTask ! executeRequest

        // TODO: Convert to tuple of (ExecuteReplyError, ExecuteResult)
        val response =
          receiveOne(5.seconds).asInstanceOf[Tuple2[ExecuteReply, ExecuteResult]]
        response._1 shouldBe an [ExecuteReplyError]
        response._2 shouldBe an [ExecuteResult]
      }

      it("should return an ExecuteReplyError if the interpreter returns incomplete") {
        val mockInterpreter = mock[Interpreter]
        doReturn(IR.Incomplete, Right("")).when(mockInterpreter)
          .interpret(anyString(), anyBoolean())

        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        val executeRequest = ExecuteRequest(
          "val x = 3", false, false,
          UserExpressions(), false
        )

        executeRequestTask ! executeRequest

        // TODO: Convert to tuple of (ExecuteReplyError, ExecuteResult)
        val response =
          receiveOne(5.seconds).asInstanceOf[Tuple2[ExecuteReply, ExecuteResult]]
        response._1 shouldBe an [ExecuteReplyError]
        response._2 shouldBe an [ExecuteResult]
      }

      it("should return a failure message if an unknown message was sent") {
        val mockInterpreter = mock[Interpreter]
        val executeRequestTask =
          system.actorOf(Props(
            classOf[ExecuteRequestTaskActor],
            mockInterpreter
          ))

        executeRequestTask ! "???" // TODO: Provide a better unknown message?

        expectMsg("Unknown message") // TODO: Replace with real type
      }
    }
  }
}