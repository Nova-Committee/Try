package committee.nova.util;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings({"unchecked", "unused"})
public interface Try<T> {
    interface ThrowingSupplier<T> extends Supplier<T> {
        T doGet() throws Throwable;

        default T get() {
            try {
                return this.doGet();
            } catch (Throwable t) {
                if (NonFatal.check(t)) throw new NonFatalException(t);
                throw new RuntimeException(t);
            }
        }
    }

    final class NonFatalException extends RuntimeException {
        public NonFatalException(Throwable cause) {
            super(cause);
        }
    }

    static <U> Try<U> of(ThrowingSupplier<U> sup) {
        try {
            return Success.apply(sup.get());
        } catch (NonFatalException e) {
            return Failure.apply(e.getCause());
        }
    }

    boolean isFailure();

    boolean isSuccess();

    T get() throws Throwable;

    T getOrElse(T defaultValue);

    Try<T> orElse(Try<T> defaultTry);

    <U> void foreach(Function<T, U> fun);

    <U> Try<U> flatMap(Function<T, Try<U>> fun);

    <U> Try<U> map(Function<T, U> fun);

    Try<T> filter(Predicate<T> p);

    Try<T> recoverWith(Function<Throwable, Try<T>> fun);

    Try<T> recover(Function<Throwable, T> fun);

    Optional<T> toOptional();

    Try<Throwable> failed();

    class Success<T> implements Try<T> {
        private final T value;

        public Success(T value) {
            this.value = value;
        }

        public static <U> Success<U> apply(U value) {
            return new Success<>(value);
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
                if (NonFatal.check(t)) return Failure.apply(t);
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
                return p.test(value) ? this : Failure.apply(new NoSuchElementException("Predicate does not hold for " + value));
            } catch (Throwable t) {
                if (NonFatal.check(t)) return Failure.apply(t);
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
            return Failure.apply(new UnsupportedOperationException("Cannot use failed in a Success!"));
        }
    }

    class Failure<T> implements Try<T> {
        private final Throwable throwable;

        public Failure(Throwable throwable) {
            this.throwable = throwable;
        }

        public static <U> Failure<U> apply(Throwable throwable) {
            return new Failure<>(throwable);
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
                if (NonFatal.check(t)) return Failure.apply(t);
                throw t;
            }
        }

        @Override
        public Try<T> recover(Function<Throwable, T> fun) {
            try {
                return Try.of(() -> fun.apply(throwable));
            } catch (Throwable t) {
                if (NonFatal.check(t)) return Failure.apply(t);
                throw t;
            }
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public Try<Throwable> failed() {
            return Success.apply(throwable);
        }
    }

    class NonFatal {
        private NonFatal() {
        }

        private static final Class<? extends Throwable>[] fatals = (Class<? extends Throwable>[]) new Class<?>[]{
                VirtualMachineError.class, ThreadDeath.class, InterruptedException.class, LinkageError.class
        };

        public static boolean check(Throwable t) {
            for (final Class<? extends Throwable> fatal : fatals)
                if (fatal.isAssignableFrom(t.getClass())) return false;
            return true;
        }
    }
}
