package io.github.henders

import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.chrisomeara.pillar.{Migrator, Registry, ReplicationOptions}
import com.datastax.driver.core.exceptions.InvalidQueryException
import com.datastax.driver.core.{Cluster, ConsistencyLevel, QueryOptions}
import com.typesafe.config.ConfigFactory
import sbt.{Logger, _}

import scala.util.Try

class CassandraMigrator(configFile: File, migrationsDir: File, logger: Logger) {
  val env = sys.props.getOrElse("SCALA_ENV", sys.env.getOrElse("SCALA_ENV", "development"))
  logger.info(s"Loading config file: $configFile for environment: $env")
  val config = ConfigFactory.parseFile(configFile).resolve().getConfig(env)
  val cassandraConfig = config.getConfig("cassandra")
  val hosts = cassandraConfig.getString("hosts").split(',')
  val keyspace = cassandraConfig.getString("keyspace")
  val port = cassandraConfig.getInt("port")
  val replicationStrategy = Try(cassandraConfig.getString("replicationStrategy"))
    .getOrElse(CassandraMigrator.DefaultReplicationStrategy)
  val replicationFactor = Try(cassandraConfig.getString("replicationFactor"))
    .getOrElse(CassandraMigrator.DefaultReplicationFactor)
  val defaultConsistencyLevel = Try(ConsistencyLevel.valueOf(cassandraConfig.getString("defaultConsistencyLevel")))
    .getOrElse(CassandraMigrator.DefaultConsistencyLevel)
  val username = Try(cassandraConfig.getString("username")).getOrElse(CassandraMigrator.DefaultUsername)
  val password = Try(cassandraConfig.getString("password")).getOrElse(CassandraMigrator.DefaultPassword)
  val session = createSession

  def createKeyspace = {
    logger.info(s"Creating keyspace $keyspace at ${hosts(0)}:$port")
    Migrator(Registry(Seq.empty))
      .initialize(session, keyspace, new ReplicationOptions(Map("class" -> replicationStrategy, "replication_factor" -> replicationFactor)))
    this
  }

  def dropKeyspace = {
    logger.info(s"Dropping keyspace $keyspace at ${hosts(0)}:$port")
    try {
      Migrator(Registry(Seq.empty)).destroy(session, keyspace)
    } catch {
      case e: InvalidQueryException => logger.warn(s"Failed to drop keyspace ($keyspace) - ${e.getMessage}")
    }
    this
  }

  def migrate = {
    val registry = Registry.fromDirectory(migrationsDir)
    logger.info(s"Migrating keyspace $keyspace at ${hosts(0)}:$port")
    session.execute(s"USE $keyspace")
    Migrator(registry).migrate(session)
    this
  }

  def createMigration(name: String): Boolean = {
    val now = LocalDateTime.now
    val creationDate: String = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    val migrationFileName: String = s"$migrationsDir/${creationDate}_$name.cql"
    logger.info(s"Creating migration file: $migrationFileName")
    val migrationTemplate: String = s"""-- description: $name
-- authoredAt: ${System.currentTimeMillis}
-- up:

###UP MIGRATION###

-- down:

###DOWN MIGRATION###

"""
    new PrintWriter(migrationFileName) {
      write(migrationTemplate)
      close()
    }
    logger.success(s"Created migration '$migrationFileName'")
    true
  }

  private def createSession = {
    val queryOptions = new QueryOptions()
    queryOptions.setConsistencyLevel(defaultConsistencyLevel)

    Cluster
      .builder()
      .addContactPoints(hosts: _*)
      .withCredentials(username, password)
      .withPort(port)
      .withQueryOptions(queryOptions)
      .build()
      .connect
  }
}

object CassandraMigrator {
  val DefaultConsistencyLevel = ConsistencyLevel.QUORUM
  val DefaultReplicationStrategy = "SimpleStrategy"
  val DefaultReplicationFactor = 3
  val DefaultUsername = "cassandra"
  val DefaultPassword = "cassandra"
}
