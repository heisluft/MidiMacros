import java.awt.Robot
import java.nio.file.{Path, Paths}
import java.util.Scanner

import javax.sound.midi.{MidiMessage, MidiSystem, Receiver, ShortMessage}

import scala.collection.mutable

class MidiInputCapture extends Receiver {
  override def send(message: MidiMessage, timeStamp: Long): Unit = message match {
    case message: ShortMessage => MidiInputCapture.keybinds(message.getData1)(message.getCommand, MidiInputCapture.r)
  }

  override def close(): Unit = {}
}

object MidiInputCapture {

  val r = new Robot()
  val CFG_PATH: Path = Paths.get("config")

  val NOTE_ON = 0x90
  val NOTE_OFF = 0x80

  val keybinds: mutable.HashMap[Int, (Int, Robot) => Unit] = new mutable.HashMap[Int, (Int, Robot) => ()]() {
    override def default(key: Int): (Int, Robot) => () = (_, _) => {}
  }

  def main(args: Array[String]): Unit = {

    val in = new Scanner(System.in)

    println("Discovered MIDI devices: ")
    val infos = MidiSystem.getMidiDeviceInfo()
    for (i <- 0 until infos.length) {
      val d = infos(i)
      println(s"${i + 1}. ${d.getName()} by ${d.getVendor()}")
    }

    println("Enter the number for the device you want to use for MIDI keyboard input: ")
    val input = in.nextInt
    if (input < 1 || input >= infos.length) {
      println("The number you entered is invalid. It must lie between (inclusive) 1 and " + infos.length)
      return
    }

    val info = infos(input - 1)
    try {
      val device = MidiSystem.getMidiDevice(info)
      device.getTransmitter().setReceiver(new MidiInputCapture)
      device.open()

      Settings.parseConfig(CFG_PATH)

      println("loaded " + keybinds.size + " keybinds!")
      println("Type q to quit!")

      while (in.nextLine() != "q") {}
      device.close()
      in.close()
    } catch {
      case e: Exception =>
        println("Exception occurred while getting MIDI devices!")
        e.printStackTrace()
    }
  }
}