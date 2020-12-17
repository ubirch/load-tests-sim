package com.ubirch

import monix.execution.Scheduler

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait Executions {
  implicit def ec: ExecutionContextExecutor
  implicit val scheduler: Scheduler
}

class DefaultExecutions(threadPoolSize: Int) extends Executions{

  override implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))
  override implicit val scheduler: Scheduler = monix.execution.Scheduler(ec)

}

