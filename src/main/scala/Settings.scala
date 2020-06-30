import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Locale
import java.util.regex.Pattern

object Settings {
  def readAllLines(path: Path): Seq[String] = {
    var out = Seq[String]()
    var reader: BufferedReader = null
    try {
      reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8.newDecoder()))
      var current = reader.readLine()
      while (current != null) {
        out = current +: out
        current = reader.readLine()
      }
    } finally {
      reader.close()
    }
    out.reverse
  }

  private val notePattern = Pattern.compile("^([A-G])(#|b|)(-?\\d)$")

  def parseConfig(path: Path): Unit = {
    if (!Files.exists(path)) {
      val out = Files.newOutputStream(path)
      val in = getClass.getResourceAsStream("/config")
      var i = in.read()
      while (i != -1) {
        out.write(i)
        i = in.read()
      }
      in.close()
      out.close()
    }
    readAllLines(path).filter(!_.startsWith("#")).map(parseLine).foreach(l => MidiInputCapture.keybinds.put(l._1, l._2))
  }

  def noteToCode(note: String): Int = {
    val matcher = notePattern.matcher(note)
    if (!matcher.matches()) throw new IllegalArgumentException
    val noteBase = matcher.group(1).charAt(0) match {
      case 'C' => 0
      case 'D' => 2
      case 'E' => 4
      case 'F' => 5
      case 'G' => 7
      case 'A' => 9
      case 'B' => 11
    }
    (matcher.group(3).toInt + 2) * 12 + noteBase + (if (matcher.group(2) == "#") 1 else 0)
  }

  def parseLine(string: String): (Int, (Int, Robot) => ()) = {
    val split = string.split(",").map(_.trim)
    if (split.length != 3) throw new Exception
    val keysToPress = split(2).split('+').map(_.trim).map(k => {
      if (k == "CTRL") KeyEvent.VK_CONTROL
      else classOf[KeyEvent].getField("VK_" + k.toUpperCase(Locale.ROOT)).get(null).asInstanceOf[Int]
    })
    if (split(1) == "ONCE")
      (noteToCode(split(0)), (i, r) =>
        if (i == MidiInputCapture.NOTE_ON) {
          keysToPress.foreach(r.keyPress)
          keysToPress.foreach(r.keyRelease)
        }
      ) else (
      noteToCode(split(0)), (i, r) =>
      i match {
        case MidiInputCapture.NOTE_ON => keysToPress.foreach(r.keyPress)
        case MidiInputCapture.NOTE_OFF => keysToPress.foreach(r.keyRelease)
        case _ =>
      }
    )
  }
}