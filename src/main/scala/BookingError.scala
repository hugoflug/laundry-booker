sealed trait BookingError
case object InvalidStartTime extends BookingError
case object InvalidEndTime extends BookingError
case object NoSuchBooking extends BookingError
case object RoomAlreadyBooked extends BookingError
