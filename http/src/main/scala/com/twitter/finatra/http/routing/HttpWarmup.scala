package com.twitter.finatra.http.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.Await
import javax.inject.Inject

private object HttpWarmup {
  val userAgent = "http-warmup-client"

  /**  Function curried as the default arg for the responseCallback: Response => Unit parameter. */
  val unitFunction: Response => Unit = _ => Unit
}

/**
 * A utility for performing requests to endpoints defined by a configured [[HttpRouter]] for the
 * purpose of warming up the `HttpServer`.
 *
 * @note This only provides routing to user-defined routes of the configured [[HttpRouter]].
 *
 * @param router the configured [[com.twitter.finatra.http.routing.HttpRouter]]
 * @param mapper the configured server [[com.twitter.finatra.json.FinatraObjectMapper]]
 *
 * @see [[com.twitter.finatra.http.routing.HttpRouter]]
 */
class HttpWarmup @Inject()(router: HttpRouter, mapper: FinatraObjectMapper) extends Logging {
  import HttpWarmup._

  /* Public */

  /**
   * Send a request to warmup services that are not yet externally receiving traffic.
   *
   * @param request the [[com.twitter.finagle.http.Request]] to send.
   * @param admin if the request should be sent to a route that is intended to be exposed on the
   *              TwitterServer HTTP admin interface.
   * @param times the number of times to send the request.
   * @param responseCallback a callback called for every response where assertions can be made.
   *
   * @note be aware that in the response callback, failed assertions that throw exceptions could
   *       prevent a server from starting. This is generally when dependent services are
   *       unresponsive, causing the warm-up request(s) to fail. As such, you should wrap your
   *       warm-up calls in these situations in a try/catch {}.
   *
   * @see [[http://twitter.github.io/finatra/user-guide/http/controllers.html#controllers-and-routing]]
   * @see [[http://twitter.github.io/finatra/user-guide/http/controllers.html#admin-paths]]
   * @see [[https://twitter.github.io/twitter-server/Admin.html TwitterServer HTTP Admin Interface]]
   */
  def send(request: => Request, admin: Boolean = false, times: Int = 1)(
    responseCallback: Response => Unit = unitFunction
  ): Unit = {
    /* Mutation */
    request.headerMap.set("Host", "127.0.0.1")
    request.headerMap.set("User-Agent", userAgent)

    val service: Service[Request, Response] =
      if (request.uri.startsWith(HttpRouter.FinatraAdminPrefix) || admin) {
        router.services.adminService
      } else {
        router.services.externalService
      }

    for (_ <- 1 to times) {
      infoResult("%s") {
        val response = Await.result(service(request))
        responseCallback(response)
        s"Warmup $request completed with ${response.status}"
      }
    }
  }

  @deprecated("This is now a no-op.", "2018-03-20")
  def close(): Unit = {}
}
