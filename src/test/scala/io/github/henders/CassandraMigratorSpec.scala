package io.github.henders

import java.nio.file.Files

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import sbt._

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

class CassandraMigratorSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll {
  val logger = mock[Logger]
  val confFile = getResourceFile("test.conf")
  val migrationDir = Files.createTempDirectory("pillar")
  val migrator = new CassandraMigrator(confFile, migrationDir.toFile, logger)

  override def beforeAll = {
    super.beforeAll
    migrator.session.execute("DROP keyspace if exists pillar_test")
  }

  describe("createKeyspace") {
    it("should return success upon creating a new keyspace") {
      keyspaces.contains("pillar_test") should equal(false)
      migrator.createKeyspace
      keyspaces.contains("pillar_test") should equal(true)
    }
  }

  describe("dropKeyspace") {
    it("should return success upon dropping an existing keyspace") {
      migrator.createKeyspace
      keyspaces.contains("pillar_test") should equal(true)
      migrator.dropKeyspace
      keyspaces.contains("pillar_test") should equal(false)
    }
  }

  describe("generateMigration") {
    it("should create a new migration file") {
//      verify(logger, times(1)).success(anyString())
      migrator.createMigration("foo") should equal(true)
    }
  }


  def keyspaces = {
    val keyspaces = try {
      migrator.session.execute(s"SELECT keyspace_name FROM system.schema_keyspaces") // cassandra v2.x
    } catch {
      case NonFatal(e) => migrator.session.execute(s"SELECT keyspace_name FROM system_schema.keyspaces") // cassandra v3+
    }
    keyspaces.all.toList.map(_.getString(0))
  }

  // SBT has differences with the class loaders vs running jre directly, gah!
  private def getResourceFile(fileName: String): java.io.File = {
    val url = Option(getClass.getClassLoader.getResource(fileName)) match {
      case Some(x) => x
      case _ => ClassLoader.getSystemClassLoader.getResource(fileName)
    }
    new java.io.File(url.toURI)
  }
}
