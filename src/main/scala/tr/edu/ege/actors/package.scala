package tr.edu.ege

import com.typesafe.config.{Config, ConfigFactory}

package object actors {
  val conf: Config = ConfigFactory.load("application.conf")
}
