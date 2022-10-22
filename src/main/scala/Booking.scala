import doobie.Get

import java.time.Instant

case class Booking(
  bookingId: BookingId,
  startTime: Instant,
  endTime: Instant,
  roomNumber: RoomNumber,
  apartmentNumber: ApartmentNumber)

case class RoomNumber private(number: Int) extends AnyVal
object RoomNumber {
  def apply(number: Int): Option[RoomNumber] = {
    if (number < 1 || number > 2) {
      None
    } else {
      Some(new RoomNumber(number))
    }
  }

  implicit def get: Get[RoomNumber] =
    Get[Int].tmap(new RoomNumber(_))
}

case class ApartmentNumber private(number: Int) extends AnyVal
object ApartmentNumber {
  def apply(number: Int): Option[ApartmentNumber] = {
    if (number < 1 || number > 20) {
      None
    } else {
      Some(new ApartmentNumber(number))
    }
  }

  implicit def get: Get[ApartmentNumber] =
    Get[Int].tmap(new ApartmentNumber(_))
}

case class BookingId(number: Int) extends AnyVal