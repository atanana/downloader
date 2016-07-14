import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import net.liftweb.json._

import scala.concurrent._
import scalaj.http._

object Main {
  def request(link: String): HttpResponse[String] = {
    Http(link).asString
  }

  def filename(link: String): String = {
    val index: Int = link.lastIndexOf('/')
    link.substring(index + 1)
  }

  def cut(link: String): String = link.substring(0, link.lastIndexOf('/'))

  def main(args: Array[String]) {
    val json = request(args.head).body
    val linksNodes: JsonAST.JValue = parse(json) \ "threads" \\ "posts" \\ "path"
    val relativePath: String = cut(cut(args.head))
    val links: List[String] = linksNodes.children.map(relativePath + "/" + _.values.toString)
    val downloadFolder = Paths.get(System.getProperty("user.dir"), "downloads_" + System.currentTimeMillis())
    downloadFolder.toFile.mkdirs()
    implicit val executor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    val counter: AtomicInteger = new AtomicInteger

    Await.ready(Future.sequence(links.map(link => Future {
      val file: String = filename(link)
      Files.write(Paths.get(downloadFolder.toString, file), Http(link).asBytes.body)
      println(s"${counter.incrementAndGet()} / ${links.size} $file")
    })), duration.Duration.Inf)

    System.exit(0)
  }
}
