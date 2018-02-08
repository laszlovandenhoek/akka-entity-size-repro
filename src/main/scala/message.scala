case class PullStart(feedUrl: String)
case class PullProgress(soFar: Long, total: Option[Long])
case class PullSummary(timeTaken: Long)
case class PullFailure(error: String)
