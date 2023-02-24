package committee.nova.util;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The `Try` type represents a lazy or non-lazy computation that may either result in an exception, or return a
 * successfully computed value.
 * <p>
 * Instances of `Try<T>` can be: {@link Success}, {@link Failure}, {@link Lazy}
 * <p>
 * If this is a {@link Lazy} instance, the method `run()V` will be called first while calling any other method.
 *
 * @author Twitter, Scala, Tapio
 */
@SuppressWarnings({"unchecked", "unused"})
public interface Try<T> {
    interface ThrowingSupplier<T> extends Supplier<T> {
        T doGet() throws Throwable;

        default T get() {
            try {
                return this.doGet();
            } catch (Throwable t) {
                if (t instanceof Exception) throw new NonFatalException(t);
                throw new RuntimeException(t);
            }
        }
    }

    final class NonFatalException extends RuntimeException {
        public NonFatalException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * @return Returns a {@link Success} if {@link Supplier#get()} runs successfully on the param `s`,
     * a {@link Failure} if a {@link NonFatalException} is caught,
     * otherwise throws the uncaught throwable.
     */
    static <U> Try<U> of(ThrowingSupplier<U> s) {
        try {
            return new Success<>(s.get());
        } catch (NonFatalException e) {
            return new Failure<>(e.getCause());
        }
    }

    /**
     * @return Returns a {@link Lazy} with the supplier `s`,
     * The supplier won't be computed until {@link Try#run()} or any other method is called.
     */
    static <U> Lazy<U> lazy(ThrowingSupplier<U> s) {
        return Lazy.of(s);
    }

    /**
     * @return If this is a {@link Lazy} instance: runs the supplier and returns a non-lazy `Try` instance.
     * Otherwise, does nothing and returns this.
     */
    Try<T> run();

    /**
     * @return `true` if the `Try` is a {@link Failure}, `false` otherwise.
     */
    boolean isFailure();

    /**
     * @return `true` if the `Try` is a {@link Success}, `false` otherwise.
     */
    boolean isSuccess();

    /**
     * @return Returns the value from this {@link Success}
     * or throws the exception if this is a {@link Failure}.
     */
    T get() throws Throwable;

    /**
     * @return Returns the value from this {@link Success}
     * or the given `defaultValue` argument if this is a {@link Failure}.
     * @apiNote This will throw an exception if it is a {@link Failure} and the computation of the defaultValue throws an exception.
     */
    T getOrElse(T defaultValue);

    /**
     * @return Returns this if it's a {@link Success}
     * or the given `defaultTry` argument if this is a {@link Failure}.
     */
    Try<T> orElse(Try<T> defaultTry);

    /**
     * Applies the given function `fun` if this is a {@link Success},
     * otherwise does nothing if this is a {@link Failure}.
     *
     * @apiNote If the `fun` throws, then this method may throw an exception.
     */
    <U> void foreach(Function<T, U> fun);

    /**
     * @return Returns the given function `fun` applied to the value from this {@link Success}
     * or returns this if this is a {@link Failure}.
     */
    <U> Try<U> flatMap(Function<T, Try<U>> fun);

    /**
     * @return Maps the given function `fun` to the value from this {@link Success}
     * or returns this if this is a {@link Failure}.
     */
    <U> Try<U> map(Function<T, U> fun);

    /**
     * @return Converts this to a {@link Failure} if the given predicate `p` is not satisfied.
     */
    Try<T> filter(Predicate<T> p);

    /**
     * @return Applies the given function `fun` if this is a {@link Failure}, otherwise returns this if this is a {@link Success}.
     * This is like `flatMap` for the exception.
     */
    Try<T> recoverWith(Function<Throwable, Try<T>> fun);

    /**
     * @return Applies the given function `f` if this is a {@link Failure}, otherwise returns this if this is a {@link Success}.
     * This is like `map` for the exception.
     */
    Try<T> recover(Function<Throwable, T> fun);

    /**
     * @return Returns {@link Optional#empty()} if this is a {@link Failure}
     * or a {@link Optional#of(T)} containing the value if this is a {@link Success}.
     */
    Optional<T> toOptional();

    /**
     * @return Completes this `Try` with an exception wrapped in a {@link Success}. The exception is either the exception that the
     * `Try` failed with (if a {@link Failure}) or an `UnsupportedOperationException`.
     */
    Try<Throwable> failed();

    class Success<T> implements Try<T> {
        private final T value;

        private Success(T value) {
            this.value = value;
        }

        @Override
        public Try<T> run() {
            return this;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }

        @Override
        public Try<T> orElse(Try<T> defaultTry) {
            return this;
        }

        @Override
        public <U> void foreach(Function<T, U> fun) {
            fun.apply(value);
        }

        @Override
        public <U> Try<U> flatMap(Function<T, Try<U>> fun) {
            try {
                return fun.apply(value);
            } catch (Throwable t) {
                if (t instanceof Exception) return new Failure<>(t);
                throw t;
            }
        }

        @Override
        public <U> Try<U> map(Function<T, U> fun) {
            return Try.of(() -> fun.apply(value));
        }

        @Override
        public Try<T> filter(Predicate<T> p) {
            try {
                return p.test(value) ? this : new Failure<>(new NoSuchElementException("Predicate does not hold for " + value));
            } catch (Throwable t) {
                if (t instanceof Exception) return new Failure<>(t);
                throw t;
            }
        }

        @Override
        public Try<T> recoverWith(Function<Throwable, Try<T>> fun) {
            return this;
        }

        @Override
        public Try<T> recover(Function<Throwable, T> fun) {
            return this;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public Try<Throwable> failed() {
            return new Failure<>(new UnsupportedOperationException("Cannot use failed in a Success!"));
        }
    }

    class Failure<T> implements Try<T> {
        private final Throwable throwable;

        private Failure(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public Try<T> run() {
            return this;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T get() throws Throwable {
            throw throwable;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public Try<T> orElse(Try<T> defaultTry) {
            return defaultTry;
        }

        @Override
        public <U> void foreach(Function<T, U> fun) {
        }

        @Override
        public <U> Try<U> flatMap(Function<T, Try<U>> fun) {
            return (Try<U>) this;
        }

        @Override
        public <U> Try<U> map(Function<T, U> fun) {
            return (Try<U>) this;
        }

        @Override
        public Try<T> filter(Predicate<T> p) {
            return this;
        }

        @Override
        public Try<T> recoverWith(Function<Throwable, Try<T>> fun) {
            try {
                return fun.apply(throwable);
            } catch (Throwable t) {
                if (t instanceof Exception) return new Failure<>(t);
                throw t;
            }
        }

        @Override
        public Try<T> recover(Function<Throwable, T> fun) {
            try {
                return Try.of(() -> fun.apply(throwable));
            } catch (Throwable t) {
                if (t instanceof Exception) return new Failure<>(t);
                throw t;
            }
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Try<Throwable> failed() {
            return new Success<>(throwable);
        }
    }

    class Lazy<T> implements Try<T> {
        private final ThrowingSupplier<T> sup;

        private Lazy(ThrowingSupplier<T> sup) {
            this.sup = sup;
        }

        public static <U> Lazy<U> of(ThrowingSupplier<U> s) {
            return new Lazy<>(s);
        }

        @Override
        public boolean isFailure() {
            return run().isFailure();
        }

        @Override
        public boolean isSuccess() {
            return run().isSuccess();
        }

        @Override
        public T get() throws Throwable {
            return run().get();
        }

        @Override
        public T getOrElse(T defaultValue) {
            return run().getOrElse(defaultValue);
        }

        @Override
        public Try<T> orElse(Try<T> defaultTry) {
            return run().orElse(defaultTry);
        }

        @Override
        public <U> void foreach(Function<T, U> fun) {
            run().foreach(fun);
        }

        @Override
        public <U> Try<U> flatMap(Function<T, Try<U>> fun) {
            return run().flatMap(fun);
        }

        @Override
        public <U> Try<U> map(Function<T, U> fun) {
            return run().map(fun);
        }

        @Override
        public Try<T> filter(Predicate<T> p) {
            return run().filter(p);
        }

        @Override
        public Try<T> recoverWith(Function<Throwable, Try<T>> fun) {
            return run().recoverWith(fun);
        }

        @Override
        public Try<T> recover(Function<Throwable, T> fun) {
            return run().recover(fun);
        }

        @Override
        public Optional<T> toOptional() {
            return run().toOptional();
        }

        @Override
        public Try<Throwable> failed() {
            return run().failed();
        }

        @Override
        public Try<T> run() {
            return Try.of(sup);
        }
    }
}
