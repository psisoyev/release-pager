# **Release Pager**

This Telegram bot is alternative to [GitHub watch function](https://help.github.com/en/github/receiving-notifications-about-activity-on-github/watching-and-unwatching-releases-for-a-repository). 
It is checking for new releases in your pre-defined list of GitHub repositories.

![Release Pager](/my-pager.png)

The service is build using Scala, ZIO, Doobie, Http4s, circe, canoe.
You can learn more about it in [this blog post](https://scala.monster/welcome-zio/).

### TODO
There are several things left todo: 
* Try replacing `canoe` with different Telegram API wrapper
* Use proper logging 
* ??? 
