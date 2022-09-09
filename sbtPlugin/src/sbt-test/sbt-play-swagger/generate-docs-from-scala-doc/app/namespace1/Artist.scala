package namespace1

/**
  * @param name Fully Name
  * @param age expressed in the Western style of counting fully completed years
  */
case class Artist(
  name:      String,
  age:       Int,
  birthdate: java.time.LocalDate)
