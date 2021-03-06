package com.sandinh.couchbase

import com.couchbase.client.java.bucket.AsyncBucketManager
import com.couchbase.client.java.document.Document
import com.couchbase.client.java.error.DocumentDoesNotExistException
import com.couchbase.client.java.query.{AsyncQueryResult, Query}
import com.couchbase.client.java.view.{AsyncViewResult, ViewQuery}
import com.couchbase.client.java.{AsyncBucket, PersistTo, ReplicaMode, ReplicateTo}
import com.sandinh.couchbase.document.JsDocument
import com.sandinh.rx.Implicits._
import rx.{Observer, Observable}
import scala.concurrent.{Promise, Future}
import scala.reflect.ClassTag
import scala.util.Try

object ScalaBucket {
  private[ScalaBucket] implicit class RichCbObs[T](val underlying: Observable[T]) extends AnyVal {
    /** couchbase-java-client return an empty Observable for get (or getXX) operators if document do not exist,
      * but return an `onError(DocumentDoesNotExistException)` Observable for unlock, replace, append, prepend operators.
      * When convert the getXX-return-Observable to Future (note that Future can not be empty),
      * for consistent, we also return a DocumentDoesNotExistException-failed Future.
      * @see com.sandinh.rx.Implicits.RichJObs#toFuture() */
    def toCbGetFuture: Future[T] = {
      val p = Promise[T]()
      underlying.single.subscribe(new Observer[T] {
        def onCompleted(): Unit = {}
        def onNext(t: T): Unit = p success t
        def onError(e: Throwable): Unit = e match {
          //NoSuchElementException is thrown in underlying.single
          case _: NoSuchElementException => p failure new DocumentDoesNotExistException
          case _                         => p failure e
        }
      })
      p.future
    }

    /** similar to underlying.<com.sandinh.rx.Implicits.RichJObs.toFuture>.map(f)
      * but do not require an ExecutionContext */
    def toFutureMap[U](f: T => U): Future[U] = {
      val p = Promise[U]()
      underlying.single.subscribe(new Observer[T] {
        def onCompleted(): Unit = {}
        def onNext(t: T): Unit = p tryComplete Try(f(t))
        def onError(e: Throwable): Unit = p failure e
      })
      p.future
    }
  }
}

final class ScalaBucket(val asJava: AsyncBucket) /*extends AnyVal*/ {
  import ScalaBucket._

  @inline def name = asJava.name()

  @inline def getJs(id: String) = get[JsDocument](id)
  def get[D <: Document[_]](id: String)(implicit tag: ClassTag[D]): Future[D] =
    asJava.get(id, tag.runtimeClass.asInstanceOf[Class[D]]).toCbGetFuture

  def getFromReplica[D <: Document[_]](id: String, tpe: ReplicaMode)(implicit tag: ClassTag[D]): Future[D] =
    asJava.getFromReplica(id, tpe, tag.runtimeClass.asInstanceOf[Class[D]]).toCbGetFuture

  def getAndLock[D <: Document[_]](id: String, lockTime: Int)(implicit tag: ClassTag[D]): Future[D] =
    asJava.getAndLock(id, lockTime, tag.runtimeClass.asInstanceOf[Class[D]]).toCbGetFuture

  def getAndTouch[D <: Document[_]](id: String, expiry: Int)(implicit tag: ClassTag[D]): Future[D] =
    asJava.getAndTouch(id, expiry, tag.runtimeClass.asInstanceOf[Class[D]]).toCbGetFuture

  def insert[D <: Document[_]](document: D): Future[D] = asJava.insert(document).toFuture
  def insert[D <: Document[_]](document: D, persistTo: PersistTo): Future[D] = asJava.insert(document, persistTo).toFuture
  def insert[D <: Document[_]](document: D, replicateTo: ReplicateTo): Future[D] = asJava.insert(document, replicateTo).toFuture
  def insert[D <: Document[_]](document: D, persistTo: PersistTo, replicateTo: ReplicateTo): Future[D] =
    asJava.insert(document, persistTo, replicateTo).toFuture

  def upsert[D <: Document[_]](document: D): Future[D] = asJava.upsert(document).toFuture
  def upsert[D <: Document[_]](document: D, persistTo: PersistTo): Future[D] = asJava.upsert(document, persistTo).toFuture
  def upsert[D <: Document[_]](document: D, replicateTo: ReplicateTo): Future[D] = asJava.upsert(document, replicateTo).toFuture
  def upsert[D <: Document[_]](document: D, persistTo: PersistTo, replicateTo: ReplicateTo): Future[D] =
    asJava.upsert(document, persistTo, replicateTo).toFuture

  def replace[D <: Document[_]](document: D): Future[D] = asJava.replace(document).toFuture
  def replace[D <: Document[_]](document: D, persistTo: PersistTo): Future[D] = asJava.replace(document, persistTo).toFuture
  def replace[D <: Document[_]](document: D, replicateTo: ReplicateTo): Future[D] = asJava.replace(document, replicateTo).toFuture
  def replace[D <: Document[_]](document: D, persistTo: PersistTo, replicateTo: ReplicateTo): Future[D] =
    asJava.replace(document, persistTo, replicateTo).toFuture

  def remove[D <: Document[_]](document: D): Future[D] = asJava.remove(document).toFuture
  def remove[D <: Document[_]](document: D, persistTo: PersistTo): Future[D] = asJava.remove(document, persistTo).toFuture
  def remove[D <: Document[_]](document: D, replicateTo: ReplicateTo): Future[D] = asJava.remove(document, replicateTo).toFuture
  def remove[D <: Document[_]](document: D, persistTo: PersistTo, replicateTo: ReplicateTo): Future[D] =
    asJava.remove(document, persistTo, replicateTo).toFuture

  def remove[D <: Document[_]](id: String)(implicit tag: ClassTag[D]) = asJava.remove(id, tag.runtimeClass.asInstanceOf[Class[D]]).toFuture
  def remove[D <: Document[_]](id: String, persistTo: PersistTo)(implicit tag: ClassTag[D]): Future[D] =
    asJava.remove(id, persistTo, tag.runtimeClass.asInstanceOf[Class[D]]).toFuture
  def remove[D <: Document[_]](id: String, replicateTo: ReplicateTo)(implicit tag: ClassTag[D]): Future[D] =
    asJava.remove(id, replicateTo, tag.runtimeClass.asInstanceOf[Class[D]]).toFuture
  def remove[D <: Document[_]](id: String, persistTo: PersistTo, replicateTo: ReplicateTo)(implicit tag: ClassTag[D]): Future[D] =
    asJava.remove(id, persistTo, replicateTo, tag.runtimeClass.asInstanceOf[Class[D]]).toFuture

  def query(query: ViewQuery): Future[AsyncViewResult] = asJava.query(query).toFuture
  def query(query: Query): Future[AsyncQueryResult] = asJava.query(query).toFuture
  def query(query: String): Future[AsyncQueryResult] = asJava.query(query).toFuture

  def unlock(id: String, cas: Long): Future[Boolean] = asJava.unlock(id, cas).toFutureMap(_.booleanValue)
  def unlock[D <: Document[_]](document: D): Future[Boolean] = asJava.unlock(document).toFutureMap(_.booleanValue)

  def touch(id: String, expiry: Int): Future[Boolean] = asJava.touch(id, expiry).toFutureMap(_.booleanValue)
  def touch[D <: Document[_]](document: D): Future[Boolean] = asJava.touch(document).toFutureMap(_.booleanValue)

  def counter(id: String, delta: Long): Future[Long] = asJava.counter(id, delta).toFutureMap(_.content.longValue)
  def counter(id: String, delta: Long, initial: Long, expiry: Int = 0): Future[Long] =
    asJava.counter(id, delta, initial, expiry).toFutureMap(_.content.longValue)

  /** @note the result document has expiry = 0 & content = null
    * @see https://github.com/couchbase/couchbase-java-client/commit/6f0c7cf2247a3ef99a71ef2edd67f1077e4646e0 */
  def append[D <: Document[_]](document: D): Future[D] = asJava.append(document).toFuture
  /** @note the result document has expiry = 0 & content = null
    * @see https://github.com/couchbase/couchbase-java-client/commit/6f0c7cf2247a3ef99a71ef2edd67f1077e4646e0 */
  def prepend[D <: Document[_]](document: D): Future[D] = asJava.prepend(document).toFuture

  def bucketManager: Future[AsyncBucketManager] = asJava.bucketManager().toFuture

  def close(): Future[Boolean] = asJava.close().toFutureMap(_.booleanValue)

  ///////////////////
  //  def read[T: Reads](id: String): Future[T] = this.get[JsDocument](id).map(_.content.as[T])
}
