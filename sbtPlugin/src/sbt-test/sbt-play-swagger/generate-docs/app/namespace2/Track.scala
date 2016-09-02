package namespace2

import namespace1.Artist

case class Track(name: String,
                 genre: Option[String],
                 artist: Artist,
                 related: Seq[Artist],
                 numbers: Seq[Int],
                 length: java.time.Duration)
