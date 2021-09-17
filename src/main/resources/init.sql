create sequence user_id_seq;

create table user_entity
(
    id        bigint default nextval('user_id_seq'::regclass) not null,
    firstname varchar(150),
    lastname  varchar(150),
    age       integer,
    constraint user_pk
        primary key (id)
);

alter sequence user_id_seq owned by user_entity.id;

create table logs
(
    id      bigserial,
    user_id bigint,
    entry   varchar(100),
    constraint logs_pk
        primary key (id),
    constraint logs_user_entity_id_fk
        foreign key (user_id) references user_entity
);

