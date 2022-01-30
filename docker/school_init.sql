-- Created by Vertabelo (http://vertabelo.com)
-- Last modification date: 2022-01-30 16:12:03.096

-- tables
-- Table: class_room
CREATE TABLE class_room (
    id bigserial  NOT NULL,
    name varchar(50)  NOT NULL,
    teacher_id int8  NOT NULL,
    CONSTRAINT class_room_pk PRIMARY KEY (id)
);

-- Table: courses
CREATE TABLE courses (
    id bigserial  NOT NULL,
    name varchar(50)  NOT NULL,
    active boolean  NOT NULL,
    teacher_id int8  NOT NULL,
    CONSTRAINT courses_pk PRIMARY KEY (id)
);

-- Table: student
CREATE TABLE student (
    id bigserial  NOT NULL,
    firstname varchar(50)  NOT NULL,
    lastname varchar(50)  NOT NULL,
    gender int  NOT NULL,
    birthday date  NOT NULL,
    grade int  NOT NULL,
    class_room_id int8  NOT NULL,
    CONSTRAINT student_pk PRIMARY KEY (id)
);

-- Table: student_courses
CREATE TABLE student_courses (
    student_id int8  NOT NULL,
    courses_id int8  NOT NULL,
    CONSTRAINT student_courses_pk PRIMARY KEY (student_id,courses_id)
);

-- Table: teacher
CREATE TABLE teacher (
    id bigserial  NOT NULL,
    firstname varchar(50)  NOT NULL,
    lastname varchar(50)  NOT NULL,
    gender int  NOT NULL,
    birthday date  NOT NULL,
    hireDate date  NOT NULL,
    CONSTRAINT teacher_pk PRIMARY KEY (id)
);

-- foreign keys
-- Reference: class_room_teacher (table: class_room)
ALTER TABLE class_room ADD CONSTRAINT class_room_teacher
    FOREIGN KEY (teacher_id)
    REFERENCES teacher (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: courses_teacher (table: courses)
ALTER TABLE courses ADD CONSTRAINT courses_teacher
    FOREIGN KEY (teacher_id)
    REFERENCES teacher (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: student_class_room (table: student)
ALTER TABLE student ADD CONSTRAINT student_class_room
    FOREIGN KEY (class_room_id)
    REFERENCES class_room (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: student_courses_courses (table: student_courses)
ALTER TABLE student_courses ADD CONSTRAINT student_courses_courses
    FOREIGN KEY (courses_id)
    REFERENCES courses (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: student_courses_student (table: student_courses)
ALTER TABLE student_courses ADD CONSTRAINT student_courses_student
    FOREIGN KEY (student_id)
    REFERENCES student (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- End of file.

