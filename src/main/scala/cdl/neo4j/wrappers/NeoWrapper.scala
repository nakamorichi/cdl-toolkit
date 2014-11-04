/**
 * @author Petri Kivikangas
 * @date 6.2.2012
 *
 */
package cdl.neo4j.wrappers

import java.io.File
import java.net.URI

import org.neo4j.cypher.ExecutionResult
import org.slf4j.LoggerFactory

import cdl.editor.Config
import cdl.objects.UW

class NeoWrapperException(reason: String) extends Exception(reason)

trait CDLNeoWrapper {
  protected val log = LoggerFactory.getLogger(this.getClass)
  var neoURI: URI = null
  def getNeoURI: String = neoURI.getPath

  /* Implement these */
  def start(): Boolean
  def stop(): Boolean
  def isConnected: Boolean
  def getHyponyms(uw: UW): Iterator[UW]
  def query(q: String): ExecutionResult
  def cleanDB(): Boolean
  def fetchUWs(hw: String): Iterator[UW]
  def importDocuments(docs: Array[File]): Boolean
  def importOntology(): Boolean

  protected def registerShutdownHook() = {
    // Registers a shutdown hook for the Neo4j instance so that it
    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
    // running example before it's completed)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run = NeoWrapper.stop
    })
  }
}

object NeoWrapper {
  protected val log = LoggerFactory.getLogger(this.getClass)
  var impl: CDLNeoWrapper = null

  /* Direct the db calls to wrapper implementations */
  def getHyponyms(uw: UW): Iterator[UW] = impl.getHyponyms(uw)
  def query(q: String): ExecutionResult = impl.query(q)
  def cleanDB(): Boolean = impl.cleanDB
  def fetchUWs(head: String): Iterator[UW] = impl.fetchUWs(head)
  def importDocuments(docs: Array[File]): Boolean = impl.importDocuments(docs)
  def isConnected: Boolean = impl.isConnected
  def stop(): Boolean = impl.stop
  def start(): Boolean = impl.start
  def importOntology(): Boolean = impl.importOntology
  def getNeoURI: String = impl.neoURI.getPath

  def toggleRestWrapper: Boolean = {
    log.info("Toggling REST API wrapper...")
    if (impl != null)
      if (impl.isInstanceOf[RestNeoWrapper])
        log.info("RestNeoWrapper already set")
      else stop()

    Config.getProperty("restNeoURI") match {
      case Some(uri) => {
        impl = new RestNeoWrapper(uri)
        impl.start()
        return impl.isConnected
      }
      case None => log.error("<restNeoURI> configuration parameter not found!")
    }
    return false
  }

  def toggleRestBatchWrapper() = {
    // TODO: implement
  }

  def toggleEmbeddedWrapper(): Boolean = {
    log.info("Toggling EmbeddedNeoWrapper...")
    if (impl != null && impl.isInstanceOf[EmbeddedNeoWrapper]) {
      log.warn("EmbeddedNeoWrapper already set")
    }
    Config.getProperty("neoPath") match {
      case Some(path) => {
        stop()
        log.info("Embedding Neo4j located at <"+path+">")
        impl = new EmbeddedNeoWrapper(path)
        start()
        log.info("Toggled EmbeddedNeoWrapper at URI "+getNeoURI)
        return true
      }
      case None => log.error("<neoPath> configuration parameter not found!")
    }
    return false
  }

  def toggleEmbeddedBatchWrapper(): Boolean = {
    log.info("Toggling EmbeddedBatchWrapper...")
    if (impl != null && impl.isInstanceOf[EmbeddedBatchWrapper]) {
      log.info("EmbeddedNeoWrapper already set at URI"+getNeoURI)
    }
    Config.getProperty("neoPath") match {
      case Some(path) => {
        stop()
        log.info("Embedding Neo4j located at <"+path+"> for batch insertion")
        impl = new EmbeddedBatchWrapper(path)
        start()
        log.info("Toggled EmbeddedBatchWrapper at URI "+getNeoURI)
        return true
      }
      case None => log.error("<neoPath> configuration parameter not found!")
    }
    return false
  }
}