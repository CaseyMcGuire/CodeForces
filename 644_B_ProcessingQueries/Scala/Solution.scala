
import scala.collection.mutable._
import scala.io.StdIn._

object Solution {

  def main(args: Array[String]): Unit = {
    val lines = scala.io.Source.stdin.getLines.toList
    val (firstLine, rest) = 
      lines match {
        case x::xs => (x, xs)
        case _ => throw new RuntimeException
      }

    val (numQueries, queueSize) = 
      firstLine.split(" ") match {
        case Array(x, y) => (x.toInt, y.toInt)
      }

    val jobs = (for {
      line <- rest.zipWithIndex
    } yield parseLine(line)).toList

    val finishedJobs = processQueries(jobs, numQueries, queueSize).sortBy(_.getId)
    finishedJobs.foreach(x => print(x.getEndTime + " "))
    println
  }


  def processQueries(pendingJobs: List[PendingJob], numQueries: Int, queueSize: Int): Vector[FinishedJob] = {
    val queue = Queue[PendingJob]()
    val finishedJobs: ArrayBuffer[FinishedJob] = ArrayBuffer.empty[FinishedJob]
    val remainingJobs: ListBuffer[PendingJob] = pendingJobs.to[ListBuffer]

    def queuePendingJobs(curTime: Long) = {
      while (!remainingJobs.isEmpty && remainingJobs.head.arrivalTime < curTime) {
        val head = remainingJobs.remove(0)
        if (queue.size == queueSize) {
          finishedJobs += head.fail
        } else {
          queue enqueue head
        }
      }
    }

    def getNextRunningJob(curTime: Long): Option[PendingJob] = {
      if (!queue.isEmpty) {
        val job = queue.dequeue
        finishedJobs += job.run(curTime)
        Some(job)
      }
      else if (!remainingJobs.isEmpty && remainingJobs.head.arrivalTime == curTime) {
        val job = remainingJobs.remove(0)
        finishedJobs += job.run(curTime)
        Some(job)
      }
      else                
        None
    }

    @scala.annotation.tailrec
    def loop(curJob: Option[CompleteJob], curTime: Long): Vector[FinishedJob] = {
      if (queue.isEmpty && remainingJobs.isEmpty) {
        finishedJobs.toVector
      } else if (!queue.isEmpty && remainingJobs.isEmpty) {
        var currentTime = curTime
        while (!queue.isEmpty) {
	  val curJob = queue.dequeue
          val runJob = curJob.run(currentTime)
          finishedJobs += runJob
          currentTime = currentTime + curJob.processTime
        }
        finishedJobs.toVector
      } else {
        queuePendingJobs(curTime)

        val getNextJob = () => {
          val nextJob = getNextRunningJob(curTime)
          nextJob match {
            case Some(y) => {
              val completeNextJob = y.run(curTime)
              Some(completeNextJob)
            }
            case _ => None
          }
        }
        val nextRunningJob: Option[CompleteJob] = 
          curJob match {
            case Some(x) if (x.endTime <= curTime) => getNextJob()
            case Some(x) => Some(x)
            case None => getNextJob()
          }


        val getNextStartTime: Long = 
          nextRunningJob match {
            case Some(x) => x.endTime
            case None    => if (!remainingJobs.isEmpty) remainingJobs.head.arrivalTime else curTime + 1
          }
        loop(nextRunningJob, getNextStartTime)
      }
    }

    loop(None, 0)
  }

  def parseLine(line: Tuple2[String, Int]): PendingJob = {
    line._1.split(" ") match {
      case Array(x, y) => PendingJob(line._2, x.toInt, y.toInt)
    }
  }

}


case class PendingJob(id: Int, arrivalTime: Long, processTime: Long) {
  def run(curTime: Long): CompleteJob = CompleteJob(id, processTime + curTime)
  def fail: FailedJob = FailedJob(id)
}
trait FinishedJob {
  def getId: Int
  def getEndTime: Long
}
case class CompleteJob(id: Int, endTime: Long) extends FinishedJob {
  override def getId: Int = id
  override def getEndTime = endTime
}
case class FailedJob(id: Int) extends FinishedJob {
  override def getId: Int = id
  override def getEndTime = -1L
}

