# Object Relational Mapper

## About

This project is a sample implementation of an OR-Mapper from scratch. It is a **Code First** approach with Annotations, which means it will use Java Source Code
to create a meta modell in the background.

## Table of Contents

* [Quick Start](#quick-start)
* [Features](#features)
* [API](#api)
    + [Entity](#entity)
    + [Database API](#database-api)
    + [Querying](#querying)
    + [Caching + Entity Tracking](#caching---entity-tracking)
* [Install](#install)

## Quick Start

First an Entity has to be defined with all the wanted features. In this case we have a `User` with a `1:n` relationship to courses.

````java

@Table("user")
@Getter
@Setter
public class User {

    @Id
    private Long id;
    private String name;
    private int age;

    @OneToMany(foreignKeyName = "user_id")
    private List<Course> courses;
}
````

After the Entities are defined in Code using annotations, the `DslContext` can be used to access the database.

````java
class Sample {

    public static void main(String[] args) {
        // Database Access API
        DslContext dsl = new DslContext(getDatabaseConfig());

        // select Entity by ID
        User userEntity = dsl.findById(User.class, 1L).orElseThrow();

        // make change & save changes
        userEntity.setName("different");
        Long id = dsl.save(userEntity);

        // delete entity
        dsl.delete(userEntity);

        // Custom Where Query
        List<User> users = dsl.findBy(User.class, Query.where()
                .like("name", "%ifferent")
                .and()
                .greaterThan("age", 20));
    }
}
````

## Features

* **Code First** Approach
* **Relationship** support for `1:n` & `m:n`
* Custom `Where` **Queries** (`NOT`,`OR`,`AND`,`=`,`LIKE`,`IN`,`>`,`>=`,`<`,`<=`)
    * Fluent API with Context => e.g. `AND` only possible after a Operator was used
* **Save** Entities (decides automatically if `INSERT` or `UPDATE` is required)
    * Includes tracking changes on relationships on both sides (`1:n` or `n:1`)
    * **Not** implemented for `n:m`
* **Caching** after every `SELECT`, `UPDATE`, `INSERT`
    * Includes tracking which columns really changed in order to **only** changed columns
* **Lazy Loading** implemented with Proxies
    * No wrapper around objects required
    * Used on default for best performance
        * Can be disabled
* **Database Support**
    * Currently **only** PostgreSQL

## API

### Entity

A Java Class represents a SQL Table. These Annotations are **required**:

* The Class must be marked with `@Table("tableName")`.
* To assign an ID Field use `@Id`. Any primitive datatype can be used but I recommend `Long`.
* Every class variable will be used as a column. If you wish to exclude a variable use `@Ignore`.

**Optional Annotations:**

* `@Column(name = "columnName")`
    * On default, the variable name is used as the column name. Use this annotation to overwrite this behaviour.

* **Relations:**
    * `@ManyToOne` / `@OneToMany`
        * `@ManyToOne` is used on table were the foreign key is located!
            * The Type must be `T` were `T` is the table of the foreign key
        * `@OneToMany` can be used on the other table to create an automatic join
            * The Type must be `List<T>` were `T` is the joined table type
    * Default behaviour is lazy but can be overwritten with `@ManyToOne(lazy = false)` / `@OneToMany(lazy = false)`

````java

@Table("user")
@Getter
@Setter
public class User {

    @Id
    private Long id;

    @Column(name = "lastname")
    private String name;
    private int age;

    @Ignore
    private String unrelatedToTable;

    @OneToMany(foreignKeyName = "user_id")
    private List<Course> courses;

    @ManyToOne(foreignKeyName = "home_id", lazy = false)
    private Home home;
}
````

### Database API

First the database connection string, username & password must be configured via `ConnectionConfig`:

````
ConnectionConfig connectionConfig = ConnectionConfig.builder()
      .connectionString("jdbc:postgresql://localhost:5432/sample")
      .username("sample")
      .password("password")
      .build();
````

The Database API `DslContext` can be instantiated with `connectionConfig` e.g. `new DslContext(connectionConfig);`

**The API exposes following methods:**

| Method Name   | Signature                    | Return Type   | Remark                                        | 
|---------------|------------------------------|---------------|-----------------------------------------------|
| `findById`    | `Class<T> type, Object id`   | `Optional<T>` | Optional is empty if no row was returned      |
| `findBy`      | `Class<T> type, Query query` | `List<T>`     | ---                                           |
| `findFirstBy` | `Class<T> type, Query query` | `Optional<T>` | Optional is empty if no row was returned      |
| `findAll`     | `Class<T> type`              | `List<T>`     | ---                                           |
| `save`        | `T entity`                   | `Object`      | returns the ID                                |
| `delete`      | `T entity`                   | `boolean`     | returns `true` if the delete was successful   |
| `deleteAll`   | `List<T> entities`           | `boolean`     | returns `true` if all deletes were successful |

### Querying

Supports `NOT`,`OR`,`AND`,`=`,`LIKE`,`IN`,`>`,`>=`,`<`,`<=` as Java Fluent API. However, currently no support for grouping or brackets.

````java
class Examples {

    public static void main(String[] args) {
        // Example 1
        Query.where()
                .like("name", "%ifferent")
                .and()
                .greaterThan("age", 20);

        // Example 2
        Query.where()
                .in("name", "Stefan", "Max", "Maxine")
                .or()
                .not().equals("name", "test");
    }
}
````

An example application is located in the `demo-orm` folder.

### Caching + Entity Tracking

This feature is enabled per default and **cannot** be disabled. There is no way to interact with it at the moment.

## Install

Requirements:

* Docker + Docker Compose
* Java JDK 17
* Gradle (wrapper already included)

**Setup DB**

Run `docker-compose.yml` in `docker` folder. It will create a Postgres 13 Container and will also automatically mount and run the `school_init.sql` schema.

**Run Demo App:**

Run `./gradlew demo-orm:shadowJar && java -jar ./demo-orm/build/libs/demo-orm-1.0-SNAPSHOT-all.jar`
in the root directory using Bash
