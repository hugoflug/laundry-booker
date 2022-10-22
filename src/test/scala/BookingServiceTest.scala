import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import doobie.Update
import org.scalatest.BeforeAndAfterEach

import java.time.{Instant, LocalDateTime, Month, ZoneOffset}
import scala.io.Source

class BookingServiceTest extends AnyFlatSpec with Matchers with ForAllTestContainer with BeforeAndAfterEach {
  override lazy val container: PostgreSQLContainer = PostgreSQLContainer()

  lazy val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](container.driverClassName, container.jdbcUrl, container.username, container.password)

  def setupDb(): Unit = {
    val dbStr = Source.fromResource("db.sql").mkString
    Update(dbStr).run(()).transact(transactor).unsafeRunSync()
  }

  def clearTable(): Unit =
    sql"DELETE FROM booked_time; ALTER SEQUENCE booked_time_booking_id_seq RESTART WITH 1;"
      .update.run.transact(transactor).unsafeRunSync()

  override def afterStart(): Unit = setupDb()

  override def beforeEach(): Unit = clearTable()

  val validStartTime: Instant = LocalDateTime.of(2022, Month.OCTOBER, 22, 12, 0).toInstant(ZoneOffset.UTC)

  val room1 = RoomNumber(1).get
  val room2 = RoomNumber(2).get

  val apt1 = ApartmentNumber(1).get
  val apt2 = ApartmentNumber(2).get

  lazy val service = new BookingService(transactor)

  "Listing bookings when there are none" should "return empty list" in {
    service.list.unsafeRunSync() shouldBe List.empty
  }

  "Booking a time and then listing the bookings" should "return the booking" in {
    val start = validStartTime
    val end = start.plusSeconds(300)
    service.book(start, end, room1, apt1).unsafeRunSync() shouldBe Right(BookingId(1))
    service.list.unsafeRunSync() shouldBe List(Booking(BookingId(1), start, end, room1, apt1))
  }

  "Booking a time, cancelling it and then listing the bookings" should "not return the booking" in {
    val start = validStartTime
    val end = start.plusSeconds(300)
    val bookingId = service.book(start, end, room1, apt1).unsafeRunSync().right.get
    service.cancel(bookingId).unsafeRunSync()
    service.list.unsafeRunSync() shouldBe List.empty
  }

  "Cancelling a non-existent booking" should "fail with NoSuchBooking" in {
    service.cancel(BookingId(1)).unsafeRunSync() shouldBe Left(NoSuchBooking)
  }

  "Booking a time that overlaps with an existing booking in the same room" should "fail with RoomAlreadyBooked" in {
    val start1 = validStartTime
    val end1 = start1.plusSeconds(300)

    val start2 = end1.minusSeconds(10)
    val end2 = start2.plusSeconds(300)

    service.book(start1, end1, room1, apt1).unsafeRunSync() shouldBe Right(BookingId(1))
    service.book(start2, end2, room1, apt2).unsafeRunSync() shouldBe Left(RoomAlreadyBooked)
  }

  "Booking a time that overlaps with an existing booking in a different room" should "succeed" in {
    val start1 = validStartTime
    val end1 = start1.plusSeconds(300)

    val start2 = end1.minusSeconds(10)
    val end2 = start2.plusSeconds(300)

    service.book(start1, end1, room1, apt1).unsafeRunSync() shouldBe Right(BookingId(1))
    service.book(start2, end2, room2, apt2).unsafeRunSync() shouldBe Right(BookingId(2))
  }

  "Booking a start time outside of allowed booking times" should "fail with InvalidStartTime" in {
    val invalidStartTime = LocalDateTime.of(2022, Month.OCTOBER, 22, 6, 0).toInstant(ZoneOffset.UTC)
    val end = invalidStartTime.plusSeconds(300)
    service.book(invalidStartTime, end, room1, apt1).unsafeRunSync() shouldBe Left(InvalidStartTime)
  }

  "Booking an end time outside of allowed booking times" should "fail with InvalidEndTime" in {
    val start = LocalDateTime.of(2022, Month.OCTOBER, 22, 21, 0).toInstant(ZoneOffset.UTC)
    val invalidEndTime = LocalDateTime.of(2022, Month.OCTOBER, 22, 23, 0).toInstant(ZoneOffset.UTC)
    service.book(start, invalidEndTime, room1, apt1).unsafeRunSync() shouldBe Left(InvalidEndTime)
  }
}
