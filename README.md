# **Release Pager**

This Telegram bot is alternative to [GitHub watch function](https://help.github.com/en/github/receiving-notifications-about-activity-on-github/watching-and-unwatching-releases-for-a-repository). 
It is checking for new releases in your pre-defined list of GitHub repositories.

![Release Pager](/my-pager.png)

The service is build using Scala, ZIO 2, Doobie, Http4s, circe, canoe.
Originally it was created for [this blog post](https://scala.monster/welcome-zio/), but the blog post is not updated to ZIO 2.

### TODO
There are several things left todo: 
* Try replacing `canoe` with different Telegram API wrapper
* Use proper logging 
* ??? 
