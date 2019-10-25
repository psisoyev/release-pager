package io.pager

import java.io.{ PrintWriter, StringWriter }

object ThrowableOps {
  implicit class ThrowableOps(t: Throwable) {
    def stackTrace: String = {
      val sw = new StringWriter
      t.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
  }
}
