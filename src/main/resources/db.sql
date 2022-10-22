create extension btree_gist;

create table booked_time (
    booking_id serial primary key,
    start_time timestamp not null,
    end_time timestamp not null,
    apartment_number integer not null check (apartment_number >= 1 and apartment_number <= 20),
    room_number integer not null check (room_number >= 1 and room_number <= 2),
    exclude using gist
    (
      room_number with =,
      tsrange(start_time, end_time, '[)') with &&
    )
);
