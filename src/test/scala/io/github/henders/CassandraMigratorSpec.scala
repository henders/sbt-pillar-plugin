package io.github.henders

import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import org.scalatest.mock.MockitoSugar
import sbt._

import scala.collection.JavaConversions._

class CassandraMigratorSpec extends FunSpec with MockitoSugar with Matchers with BeforeAndAfterAll {
  val logger = mock[Logger]
  val confFile = getResourceFile("test.conf")
  val migrationDir = confFile.getParentFile
  val migrator = new CassandraMigrator(confFile, migrationDir, logger)

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

  def keyspaces = {
//  val keyspaces = migrator.session.execute(s"SELECT keyspace_name FROM system_schema.keyspaces") // cassandra v3+
    val keyspaces = migrator.session.execute(s"SELECT keyspace_name FROM system.schema_keyspaces")   // cassandra v2.x
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
