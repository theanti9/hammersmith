
package hammersmith.util.rx
package operators

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Try, Success, Failure}


object DropWhileOperator {
  def apply[T](source: MongoObservable[T], p: (T) => Boolean) = {
    new DropWhileOperator[T](source, p)
  }
}
class DropWhileOperator[T] private(source: MongoObservable[T], p: (T) => Boolean) extends RxOperator[T] {

  override def apply(observer: MongoObserver[T]) = {
    source.subscribe(new MongoObserver[T] {

      val skipping = new AtomicBoolean(true)

      /**
       * Indicates that the data stream inside the Observable has ended,
       * and no more data will be send (i.e. no more calls to `onNext`, and `onError`
       * will not be invoked)
       *
       * This is especially useful with something like a Cursor to indicate
       * that the total data stream has been exhausted.
       */
      def onComplete(): Unit = observer.onComplete()

      /**
       * What to do in the case of an error.
       *
       * Once this is invoked, no further calls to `onNext` will be made,
       * and `onComplete` will not be invoked.
       * @param t
       */
      def onError(t: Throwable): Unit = observer.onError(t)

      def onNext(item: T): Unit = {
        if (!skipping.get) observer.onNext(item)
        else {
          Try(
            if (!p(item))  {
              skipping.set(false)
              observer.onNext(item)
            } else {}
          ) match {
            case Success(_) =>
            case Failure(t) => observer.onError(t)
          }
        }
      }
    })
  }
}