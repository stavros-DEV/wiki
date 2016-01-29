create table sequence
(
   next_value integer
);
insert into sequence value (1000);

create table page
(
   name varchar(255) primary key,
   content text not null,
   published int default 0,
   published_id varchar(255)
);

