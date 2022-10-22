import cats.effect.IO
import doobie.Transactor
import doobie.implicits._

import java.time.Instant
import doobie.implicits.legacy.instant._
import doobie.postgres._

import java.time.LocalDateTime
import java.time.ZoneOffset

class BookingService(transactor: Transactor[IO]) {
  private val validTimes = Range.inclusive(7, 22)

  private def validateTime(startTime: Instant, endTime: Instant): Option[BookingError] = {
    val startHour = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC).getHour
    val endHour = LocalDateTime.ofInstant(endTime, ZoneOffset.UTC).getHour
    if (!validTimes.contains(startHour)) {
      Some(InvalidStartTime)
    } else if (!validTimes.contains(endHour)) {
      Some(InvalidEndTime)
    } else {
      None
    }
  }

  def book(startTime: Instant, endTime: Instant, roomNumber: RoomNumber, apartmentNumber: ApartmentNumber): IO[Either[BookingError, BookingId]] =
    validateTime(startTime, endTime) match {
      case Some(err) => IO.pure(Left(err))
      case None =>
        sql"""insert into booked_time(start_time, end_time, room_number, apartment_number)
              values ($startTime, $endTime, $roomNumber, $apartmentNumber)
              returning booking_id"""
          .query[BookingId].unique.attemptSomeSqlState {
            case sqlstate.class23.EXCLUSION_VIOLATION => RoomAlreadyBooked
          }.transact(transactor)
    }

  def cancel(bookingId: BookingId): IO[Either[NoSuchBooking.type, Unit]] =
    sql"delete from booked_time where booking_id=$bookingId"
      .update.run.transact(transactor).map {
        case 0 => Left(NoSuchBooking)
        case _ => Right(())
      }

  def list: IO[List[Booking]] =
    sql"select booking_id, start_time, end_time, room_number, apartment_number from booked_time"
      .query[Booking].to[List].transact(transactor)
}
