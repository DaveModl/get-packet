create table t_red_packet(
id int(12) not null auto_increment,
user_id int(12) not null,
amount decimal(16,2) not null,
send_date timestamp not null,
total int(12) not null,
unit_amount decimal(12) not null,
stock int(12) not null,
version int(12) default 0 not null,
note varchar(256) null,
primary key(id)
);

create table t_user_red_packet(
id int(12) not null auto_increment,
red_packet_id int(12) not null,
user_id int(12) not null,
amount decimal(16,2) not null,
get_time timestamp not null,
note varchar(256) null,
primary key(id));
