package io.github.henders

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._

object PillarPlugin extends AutoPlugin {

  object autoImport {
    //  available tasks
    val createKeyspace = taskKey[Unit]("Create keyspace.")
    val dropKeyspace = taskKey[Unit]("Drop keyspace.")
    val migrate = taskKey[Unit]("Run pillar migrations.")
    val cleanMigrate = taskKey[Unit]("Recreate keyspace and run pillar migrations.")
    val createMigration = inputKey[Unit]("Create a new migration file")
    //  build settings declared in the user's build.sbt
    val pillarConfigFile = settingKey[File]("Path to the configuration file with the cassandra settings")
    val pillarMigrationsDir = settingKey[File]("Path to the directory with the migration files")
  }

  import autoImport._

  override def trigger = allRequirements

  override lazy val buildSettings = Seq(
    pillarConfigFile := file("db/pillar.conf"),
    pillarMigrationsDir := file("db/migrations"),
    createKeyspace := createKeyspaceTask.value,
    dropKeyspace := dropKeyspaceTask.value,
    migrate := migrateTask.value,
    cleanMigrate := cleanMigrateTask.value,
    // Need to declare this task inline because I can't figure out why it won't work as a Dyn.inputTask object
    createMigration := {
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      val migrationName = args.headOption.getOrElse("")

      if (migrationName.isEmpty) {
        streams.value.log.error("You should call this with an argument, e.g.: $ sbt 'createMigration my_table'")
        // Workaround for SBT bug where this task is called multiple times from single invocation
        System.exit(1)
      }

      streams.value.log.info(s"Creating migration for '${migrationName}'....")
      new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).createMigration(migrationName)
      // Workaround for SBT bug where this task is called multiple times from single invocation
      System.exit(0)
    }
  )

  lazy val createKeyspaceTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).createKeyspace
  }

  lazy val dropKeyspaceTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).dropKeyspace
  }

  lazy val migrateTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log).migrate
  }

  lazy val cleanMigrateTask = Def.task {
    new CassandraMigrator(pillarConfigFile.value, pillarMigrationsDir.value, streams.value.log)
      .dropKeyspace
      .createKeyspace
      .migrate
  }
}
