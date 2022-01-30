package service;

import entity.ClassRoom;
import entity.Course;
import entity.Student;
import entity.Teacher;
import lombok.RequiredArgsConstructor;
import orm.meta.DslContext;
import orm.meta.Query;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class DemoService {

    private final DslContext dsl;

    public void start() {
        createTeacherAndDelete();
        System.out.println("----------------------------------------");
        saveAndSelectNToOne();
        System.out.println("----------------------------------------");
        customQueries();
        System.out.println("----------------------------------------");
        saveChangesFromOneToMany();
        System.out.println("----------------------------------------");
        System.out.println("----------------------------------------");

        dsl.deleteAll(dsl.findAll(Student.class));
        dsl.deleteAll(dsl.findAll(Course.class));
        dsl.deleteAll(dsl.findAll(ClassRoom.class));
        dsl.deleteAll(dsl.findAll(Teacher.class));
    }

    private void createTeacherAndDelete() {
        Teacher insertTeacher = Teacher.builder()
                .firstname("Max")
                .lastname("Mustermann")
                .birthday(LocalDate.of(1980, 4, 16))
                .gender(1)
                .hireDate(LocalDate.now())
                .build();
        Object id = dsl.save(insertTeacher);
        System.out.println("Save Teacher " + insertTeacher);
        System.out.println("After Teacher saved it got assigend ID  " + id);

        boolean result = dsl.delete(dsl.findById(Teacher.class, id).orElseThrow());
        System.out.println("Teacher has been delete with result " + result);
    }

    private void saveAndSelectNToOne() {
        Teacher teacher = Teacher.builder()
                .firstname("Max")
                .lastname("Mustermann")
                .birthday(LocalDate.of(1980, 4, 16))
                .gender(1)
                .hireDate(LocalDate.now())
                .build();
        teacher = dsl.findById(Teacher.class, dsl.save(teacher)).orElseThrow();
        ClassRoom classRoom = ClassRoom.builder()
                .name("Class Room Test")
                .teacher(teacher)
                .build();
        classRoom = dsl.findById(ClassRoom.class, dsl.save(classRoom)).orElseThrow();
        System.out.println("Teacher and Classroom saved: " + classRoom);
        System.out.println("----------------------------------------");
        classRoom.setName("Class Room Modified");
        classRoom = dsl.findById(ClassRoom.class, dsl.save(classRoom)).orElseThrow();
        System.out.println("----------------------------------------");
        System.out.println("Modified Classroom: " + classRoom);
    }

    private void customQueries() {
        Teacher teacher = Teacher.builder()
                .firstname("Max")
                .lastname("Mustermann")
                .birthday(LocalDate.of(1980, 4, 16))
                .gender(1)
                .hireDate(LocalDate.now())
                .build();
        dsl.save(teacher);
        dsl.save(teacher.toBuilder().id(null).firstname("Stefan").build());
        dsl.save(teacher.toBuilder().id(null).firstname("Marcus").build());
        dsl.save(teacher.toBuilder().id(null).firstname("Maxi").gender(0).build());
        System.out.println("Add multiple teachers");
        System.out.println("----------------------------------------");

        List<Teacher> t = dsl.findBy(Teacher.class, Query.where().in("firstname", "Stefan", "Max", "Other"));
        System.out.println("Custom Query Teacher: Find Stefan & Max");

        System.out.println(t);
        System.out.println("----------------------------------------");
        t = dsl.findBy(Teacher.class, Query.where().like("firstname", "Ma%"));
        System.out.println("Custom Query Teacher: Find all starting with 'Ma'");
        System.out.println(t);
    }

    private void saveChangesFromOneToMany() {
        Teacher teacher = Teacher.builder()
                .firstname("Max")
                .lastname("Mustermann")
                .birthday(LocalDate.of(1980, 4, 16))
                .gender(1)
                .hireDate(LocalDate.now())
                .build();
        teacher = dsl.findById(Teacher.class, dsl.save(teacher)).orElseThrow();

        Course course = Course.builder()
                .name("Course 1")
                .teacher(teacher)
                .build();
        dsl.findById(Course.class, dsl.save(course));

        System.out.println("Saving Courses");
        System.out.println(course);

        ClassRoom classRoom = ClassRoom.builder()
                .name("Class Room Test")
                .teacher(teacher)
                .build();
        dsl.save(classRoom);

        Student student = Student.builder()
                .firstname("Max")
                .lastname("Mustermann")
                .birthday(LocalDate.of(1980, 4, 16))
                .gender(1)
                .grade(3)
                .course(course.toBuilder().name("Course 2").build())
                .classRoom(classRoom)
                .build();
        dsl.findById(Student.class, dsl.save(student));

        System.out.println("----------------------------------------");
        System.out.println("After saving Student with new course changes");
        System.out.println(dsl.findById(Course.class, course.getId()));
    }
}
